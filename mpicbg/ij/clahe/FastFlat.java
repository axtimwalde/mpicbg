/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package mpicbg.ij.clahe;

import java.awt.Rectangle;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.Histogram;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

/**
 * &lsquot;Contrast Limited Adaptive Histogram Equalization&rsquot; as
 * described in
 * 
 * <br />BibTeX:
 * <pre>
 * @article{zuiderveld94,
 *   author    = {Zuiderveld, Karel},
 *   title     = {Contrast limited adaptive histogram equalization},
 *   book      = {Graphics gems IV},
 *   year      = {1994},
 *   isbn      = {0-12-336155-9},
 *   pages     = {474--485},
 *   publisher = {Academic Press Professional, Inc.},
 *   address   = {San Diego, CA, USA},
 * }
 * </pre>
 * 
 * This version calculates the CDF for adjacent blocks and interpolates
 * the respective CDF for each pixel location in between.
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 */
public class FastFlat
{
	/**
	 * Process and {@link ImagePlus} with a given set of parameters.  Create
	 * mask and bounding box from the {@link Roi} of that {@link ImagePlus} and
	 * the passed mask if any.
	 * 
	 * @param imp
	 * @param blockRadius
	 * @param bins
	 * @param slope
	 * @param mask can be null
	 */
	final static public void run(
			final ImagePlus imp,
			final int blockRadius,
			final int bins,
			final float slope,
			final ByteProcessor mask )
	{
		final Roi roi = imp.getRoi();
		if ( roi == null )
			run( imp, blockRadius, bins, slope, null, mask );
		else
		{
			final Rectangle roiBox = roi.getBounds();
			final ImageProcessor roiMask = roi.getMask();
			if ( mask != null )
			{
				final Rectangle oldRoi = mask.getRoi();
				mask.setRoi( roi );
				final ByteProcessor cropMask = ( ByteProcessor )mask.crop().convertToByte( true );
				if ( roiMask != null )
				{
					final byte[] roiMaskPixels = ( byte[] )roiMask.getPixels();
					final byte[] cropMaskPixels = ( byte[] )cropMask.getPixels();
					for ( int i = 0; i < roiMaskPixels.length; ++i )
						cropMaskPixels[ i ] = ( byte )Util.roundPositive( ( cropMaskPixels[ i ] & 0xff ) * ( roiMaskPixels[ i ] & 0xff ) / 255.0f );
				}
				run( imp, blockRadius, bins, slope, roiBox, cropMask );
				mask.setRoi( oldRoi );
			}
			else if ( roiMask == null )
				run( imp, blockRadius, bins, slope, roiBox, null );
			else
				run( imp, blockRadius, bins, slope, roiBox, ( ByteProcessor )roiMask.convertToByte( false ) );
		}
	}
	
	
	/**
	 * Process and {@link ImagePlus} with a given set of parameters including
	 * the bounding box and mask.
	 * 
	 * @param imp
	 * @param blockRadius
	 * @param bins
	 * @param slope
	 * @param roiBox can be null
	 * @param mask can be null
	 */
	final static public void run(
			final ImagePlus imp,
			final int blockRadius,
			final int bins,
			final float slope,
			final java.awt.Rectangle roiBox,
			final ByteProcessor mask )
	{
		/* initialize box if necessary */
		final Rectangle box;
		if ( roiBox == null )
		{
			if ( mask == null )
				box = new Rectangle( 0, 0, imp.getWidth(), imp.getHeight() );
			else
				box = new Rectangle( 0, 0, Math.min( imp.getWidth(), mask.getWidth() ), Math.min( imp.getHeight(), mask.getHeight() ) );
		}
		else
			box = roiBox;
		
		/* make sure that the box is not larger than the mask */
		if ( mask != null )
		{
			box.width = Math.min( mask.getWidth(), box.width );
			box.height = Math.min( mask.getHeight(), box.height );
		}
		
		/* make sure that the box is not larger than the image */
		box.width = Math.min( imp.getWidth() - box.x, box.width );
		box.height = Math.min( imp.getHeight() - box.y, box.height );
		
		final int boxXMax = box.x + box.width;
		final int boxYMax = box.y + box.height;
		
		/* convert 8bit processors with a LUT to RGB and create Undo-step */
		final ImageProcessor ip;
		if ( imp.getType() == ImagePlus.COLOR_256 )
		{
			ip = imp.getProcessor().convertToRGB();
			imp.setProcessor( imp.getTitle(), ip );
		}
		else
			ip = imp.getProcessor();
		
		/* work on ByteProcessors that reflect the user defined intensity range */
		final ByteProcessor src;
		if ( imp.getType() == ImagePlus.GRAY8 )
			src = ( ByteProcessor )ip.convertToByte( true ).duplicate();
		else
			src = ( ByteProcessor )ip.convertToByte( true );
		final ByteProcessor dst = ( ByteProcessor )src.duplicate();
		
		run( imp, blockRadius, bins, slope, box.x, box.y, boxXMax, boxYMax, src, dst, mask, ip );
	}
	
	
	final static private int[] createHistogram(
			final int blockRadius,
			final int bins,
			final int boxXMin,
			final int boxYMin,
			final int boxXMax,
			final int boxYMax,
			final int blockXCenter,
			final int blockYCenter,
			final ByteProcessor src )
	{
		final int[] hist = new int[ bins + 1 ];
		
		final int xMin = Math.max( boxXMin, blockXCenter - blockRadius );
		final int yMin = Math.max( boxYMin, blockYCenter - blockRadius );
		
		final int xMax = Math.min( boxXMax, blockXCenter + blockRadius + 1 );
		final int yMax = Math.min( boxYMax, blockYCenter + blockRadius + 1 );
		
		for ( int y = yMin; y < yMax; ++y )
		{
			final int row = src.getWidth() * y;
			for ( int x = xMin; x < xMax; ++x )
			{
				++hist[ Util.roundPositive( src.get( row + x ) / 255.0f * bins ) ];
			}
		}
		
		return hist;
	}
	
	final static private int sum( final int[] bins )
	{
		int n = 0;
		for ( final int k : bins )
			n += k;
		return n;
	}
	
	
	/**
	 * The actual implementation
	 * 
	 * @param imp
	 * @param blockRadius
	 * @param bins
	 * @param slope
	 * @param boxXMin
	 * @param boxYMin
	 * @param boxXMax
	 * @param boxYMax
	 * @param src
	 * @param dst
	 * @param mask
	 * @param ip
	 */
	final static private void run(
			final ImagePlus imp,
			final int blockRadius,
			final int bins,
			final float slope,
			final int boxXMin,
			final int boxYMin,
			final int boxXMax,
			final int boxYMax,
			final ByteProcessor src,
			final ByteProcessor dst,
			final ByteProcessor mask,
			final ImageProcessor ip )
	{
		
		for ( int y = boxYMin; y < boxYMax; ++y )
		{
			for ( int x = boxXMin; x < boxXMax; ++x )
			{
				final int v = Util.roundPositive( src.get( x, y ) / 255.0f * bins );
				
				final int[] hist = createHistogram( blockRadius, bins, boxXMin, boxYMin, boxXMax, boxYMax, x, y, src );
				final int[] clippedHist = new int[ hist.length ];
				
				final int n = sum( hist );
				final int limit;
				if ( mask == null )
					limit = ( int )( slope * n / bins + 0.5f );
				else
					limit = ( int )( ( 1 + mask.get( x, y ) / 255.0f * ( slope - 1 ) ) * n / bins + 0.5f );
				
				dst.set( x, y, Util.roundPositive( Util.transferValue( v, hist, clippedHist, limit, bins ) * 255.0f ) );
			}
			
			/* multiply the current row into ip */
			final int t = y * imp.getWidth();
			if ( imp.getType() == ImagePlus.GRAY8 )
			{
				for ( int x = boxXMin; x < boxXMax; ++x )
				{
					final int i = t + x;
					ip.set( i, dst.get( i ) );
				}
			}
			else if ( imp.getType() == ImagePlus.GRAY16 )
			{
				final int min = ( int )ip.getMin();
				for ( int x = boxXMin; x < boxXMax; ++x )
				{
					final int i = t + x;
					final int v = ip.get( i );
					final float vSrc = src.get( i );
					final float a;
					if ( vSrc == 0 )
						a = 1.0f;
					else
						a = ( float )dst.get( i ) / vSrc;
					ip.set( i, Math.max( 0, Math.min( 65535, Util.roundPositive( a * ( v - min ) + min ) ) ) );
				}
			}
			else if ( imp.getType() == ImagePlus.GRAY32 )
			{
				final float min = ( float )ip.getMin();
				for ( int x = boxXMin; x < boxXMax; ++x )
				{
					final int i = t + x;
					final float v = ip.getf( i );
					final float vSrc = src.get( i );
					final float a;
					if ( vSrc == 0 )
						a = 1.0f;
					else
						a = ( float )dst.get( i ) / vSrc;
					ip.setf( i, a * ( v - min ) + min );
				}
			}
			else if ( imp.getType() == ImagePlus.COLOR_RGB )
			{
				for ( int x = boxXMin; x < boxXMax; ++x )
				{
					final int i = t + x;
					final int argb = ip.get( i );
					final float vSrc = src.get( i );
					final float a;
					if ( vSrc == 0 )
						a = 1.0f;
					else
						a = ( float )dst.get( i ) / vSrc;
					final int r = Math.max( 0, Math.min( 255, Util.roundPositive( a * ( ( argb >> 16 ) & 0xff ) ) ) );  
					final int g = Math.max( 0, Math.min( 255, Util.roundPositive( a * ( ( argb >> 8 ) & 0xff ) ) ) );
					final int b = Math.max( 0, Math.min( 255, Util.roundPositive( a * ( argb & 0xff ) ) ) );
					ip.set( i, ( r << 16 ) | ( g << 8 ) | b );
				}
			}
			imp.updateAndDraw();
		}
	}
}
