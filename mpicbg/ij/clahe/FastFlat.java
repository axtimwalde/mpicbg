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

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

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
	static abstract private class Apply< T extends ImageProcessor >
	{
		final protected T ip;
		final protected int width;
		final protected int height;
		
		final protected byte[] srcPixels;
		final protected byte[] dstPixels;
		final protected byte[] maskPixels;
		
		final protected int boxXMin, boxYMin, boxXMax, boxYMax;
		
		Apply(
				final T ip,
				final ByteProcessor src,
				final ByteProcessor dst,
				final ByteProcessor mask,
				final int boxXMin,
				final int boxYMin,
				final int boxXMax,
				final int boxYMax ) throws Exception
		{
			this.ip = ip;
			width = ip.getWidth();
			height = ip.getHeight();
			
			this.boxXMin = boxXMin;
			this.boxYMin = boxYMin;
			this.boxXMax = boxXMax;
			this.boxYMax = boxYMax;
			
			if ( !(
					src.getWidth() == width && src.getHeight() == height &&
					dst.getWidth() == width && dst.getHeight() == height ) )
				throw new Exception( "Image sizes do not match." );
			
			srcPixels = ( byte[] )src.getPixels();
			dstPixels = ( byte[] )dst.getPixels();
			if ( mask == null )
			{
				maskPixels = new byte[ srcPixels.length ];
				mpicbg.util.Util.memset( maskPixels, ( byte )255 );
			}
			else
			{
				if (
						boxXMin == 0 &&
						boxYMin == 0 &&
						boxXMax == mask.getWidth() &&
						boxYMax == mask.getHeight() )
					maskPixels = ( byte[] )mask.getPixels();
				else
				{
					final ByteProcessor extendedMask = new ByteProcessor( width, height );
					extendedMask.copyBits( mask, boxXMin, boxYMin, Blitter.COPY );
					maskPixels = ( byte[] )extendedMask.getPixels();
				}
			}
		}
		
		abstract public void apply( final int xMin, final int yMin, final int xMax, final int yMax );
	}
	
	final static private class ByteApply extends Apply< ByteProcessor >
	{
		final protected byte[] ipPixels;
		           
		public ByteApply(
				final ByteProcessor ip,
				final ByteProcessor src,
				final ByteProcessor dst,
				final ByteProcessor mask,
				final int boxXMin,
				final int boxYMin,
				final int boxXMax,
				final int boxYMax ) throws Exception
		{
			super( ip, src, dst, mask, boxXMin, boxYMin, boxXMax, boxYMax );
			ipPixels = ( byte[] )ip.getPixels();
		}
		
		@Override
		final public void apply( final int cellXMin, final int cellYMin, final int cellXMax, final int cellYMax )
		{
			final int xMin = Math.max( boxXMin, cellXMin );
			final int yMin = Math.max( boxYMin, cellYMin );
			final int xMax = Math.min( boxXMax, cellXMax );
			final int yMax = Math.min( boxYMax, cellYMax );
			
			for ( int y = yMin; y < yMax; ++y )
			{
				final int t = y * width;
				for ( int x = xMin; x < xMax; ++x )
				{
					final int i = t + x;
					final float m = ( maskPixels[ i ] & 0xff ) / 255.0f;
					final float v = srcPixels[ i ] & 0xff;
					final float a = dstPixels[ i ] & 0xff;
					final float b = m * a + ( 1.0f - m ) * v;
					ipPixels[ i ] = ( byte )Util.roundPositive( b );
				}
			}
		}
	}
	
	final static private class ShortApply extends Apply< ShortProcessor >
	{
		final protected short[] ipPixels;
		           
		public ShortApply(
				final ShortProcessor ip,
				final ByteProcessor src,
				final ByteProcessor dst,
				final ByteProcessor mask,
				final int boxXMin,
				final int boxYMin,
				final int boxXMax,
				final int boxYMax ) throws Exception
		{
			super( ip, src, dst, mask, boxXMin, boxYMin, boxXMax, boxYMax );
			ipPixels = ( short[] )ip.getPixels();
		}
		
		@Override
		final public void apply( final int cellXMin, final int cellYMin, final int cellXMax, final int cellYMax )
		{
			final int xMin = Math.max( boxXMin, cellXMin );
			final int yMin = Math.max( boxYMin, cellYMin );
			final int xMax = Math.min( boxXMax, cellXMax );
			final int yMax = Math.min( boxYMax, cellYMax );
		
			final int min = ( int )ip.getMin();
			for ( int y = yMin; y < yMax; ++y )
			{
				final int t = y * width;
				for ( int x = xMin; x < xMax; ++x )
				{
					final int i = t + x;
					final float m = ( maskPixels[ i ] & 0xff ) / 255.0f;
					final int v = ipPixels[ i ] & 0xffff;
					final float vSrc = srcPixels[ i ] & 0xff;
					final float a;
					if ( vSrc == 0 )
						a = 1.0f;
					else
						a = ( float )( dstPixels[ i ] & 0xff ) / vSrc;
					final float b = m * ( a * ( v - min ) + min - v ) + v;
					ipPixels[ i ] =  ( short )Math.max( 0, Math.min( 65535, Util.roundPositive( b ) ) );
				}
			}
		}
	}
	
	final static private class FloatApply extends Apply< FloatProcessor >
	{
		final protected float[] ipPixels;
		           
		public FloatApply(
				final FloatProcessor ip,
				final ByteProcessor src,
				final ByteProcessor dst,
				final ByteProcessor mask,
				final int boxXMin,
				final int boxYMin,
				final int boxXMax,
				final int boxYMax ) throws Exception
		{
			super( ip, src, dst, mask, boxXMin, boxYMin, boxXMax, boxYMax );
			ipPixels = ( float[] )ip.getPixels();
		}
		
		@Override
		final public void apply( final int cellXMin, final int cellYMin, final int cellXMax, final int cellYMax )
		{
			final int xMin = Math.max( boxXMin, cellXMin );
			final int yMin = Math.max( boxYMin, cellYMin );
			final int xMax = Math.min( boxXMax, cellXMax );
			final int yMax = Math.min( boxYMax, cellYMax );
		
			final float min = ( float )ip.getMin();
			for ( int y = yMin; y < yMax; ++y )
			{
				final int t = y * width;
				for ( int x = xMin; x < xMax; ++x )
				{
					final int i = t + x;
					final float m = ( maskPixels[ i ] & 0xff ) / 255.0f;
					final float v = ipPixels[ i ];
					final float vSrc = srcPixels[ i ] & 0xff;
					final float a;
					if ( vSrc == 0 )
						a = 1.0f;
					else
						a = ( float )( dstPixels[ i ] & 0xff ) / vSrc;
					ipPixels[ i ] = m * ( a * ( v - min ) + min - v ) + v;
				}
			}
		}
	}
	
	final static private class RGBApply extends Apply< ColorProcessor >
	{
		final protected int[] ipPixels;
		           
		public RGBApply(
				final ColorProcessor ip,
				final ByteProcessor src,
				final ByteProcessor dst,
				final ByteProcessor mask,
				final int boxXMin,
				final int boxYMin,
				final int boxXMax,
				final int boxYMax ) throws Exception
		{
			super( ip, src, dst, mask, boxXMin, boxYMin, boxXMax, boxYMax );
			ipPixels = ( int[] )ip.getPixels();
		}
		
		@Override
		final public void apply( final int cellXMin, final int cellYMin, final int cellXMax, final int cellYMax )
		{
			final int xMin = Math.max( boxXMin, cellXMin );
			final int yMin = Math.max( boxYMin, cellYMin );
			final int xMax = Math.min( boxXMax, cellXMax );
			final int yMax = Math.min( boxYMax, cellYMax );
		
			for ( int y = yMin; y < yMax; ++y )
			{
				final int t = y * width;
				for ( int x = xMin; x < xMax; ++x )
				{
					final int i = t + x;
					final float m = ( maskPixels[ i ] & 0xff ) / 255.0f;
					final int argb = ipPixels[ i ];
					final float vr = ( argb >> 16 ) & 0xff;
					final float vg = ( argb >> 8 ) & 0xff;
					final float vb = argb & 0xff;
					final float vSrc = srcPixels[ i ] & 0xff;
					final float a;
					if ( vSrc == 0 )
						a = 1.0f;
					else
						a = ( float )( dstPixels[ i ] & 0xff ) / vSrc;
					final float br = vr * ( 1.0f + m * ( a - 1.0f ) );
					final float bg = vg * ( 1.0f + m * ( a - 1.0f ) );
					final float bb = vb * ( 1.0f + m * ( a - 1.0f ) );
					
					final int r = Math.max( 0, Math.min( 255, Util.roundPositive( br ) ) );  
					final int g = Math.max( 0, Math.min( 255, Util.roundPositive( bg ) ) );
					final int b = Math.max( 0, Math.min( 255, Util.roundPositive( bb ) ) );
					ipPixels[ i ] = ( r << 16 ) | ( g << 8 ) | b;
				}
			}
		}
	}
	
		
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
			final int blockXCenter,
			final int blockYCenter,
			final ByteProcessor src )
	{
		final int[] hist = new int[ bins + 1 ];
		
		final int xMin = Math.max( 0, blockXCenter - blockRadius );
		final int yMin = Math.max( 0, blockYCenter - blockRadius );
		
		final int xMax = Math.min( src.getWidth(), blockXCenter + blockRadius + 1 );
		final int yMax = Math.min( src.getHeight(), blockYCenter + blockRadius + 1 );
		
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
		int[] hist;
		float[] tl;
		float[] tr;
		float[] bl;
		float[] br;
		
		final int blockSize = 2 * blockRadius + 1;
		final int limit = ( int )( slope * blockSize * blockSize / bins + 0.5f );
		
		/* div */
		final int nc = src.getWidth() / blockSize;
		final int nr = src.getHeight() / blockSize;
		
		/* % */
		final int cm = src.getWidth() - nc * blockSize;
		final int[] cs;
		switch ( cm )
		{
		case 0:
			cs = new int[ nc ];
			for ( int i = 0; i < nc; ++i )
				cs[ i ] = i * blockSize + blockRadius + 1;
			break;
		case 1:
			cs = new int[ nc + 1 ];
			for ( int i = 0; i < nc; ++i )
				cs[ i ] = i * blockSize + blockRadius + 1;
			cs[ nc ] = src.getWidth() - blockRadius - 1;
			break;
		default:
			cs = new int[ nc + 2 ];
			cs[ 0 ] = blockRadius + 1;
			for ( int i = 0; i < nc; ++i )
				cs[ i + 1 ] = i * blockSize + blockRadius + 1 + cm / 2;
			cs[ nc + 1 ] = src.getWidth() - blockRadius - 1;
		}
		
		final int rm = src.getHeight() - nr * blockSize;
		final int[] rs;
		switch ( rm )
		{
		case 0:
			rs = new int[ nr ];
			for ( int i = 0; i < nr; ++i )
				rs[ i ] = i * blockSize + blockRadius + 1;
			break;
		case 1:
			rs = new int[ nr + 1 ];
			for ( int i = 0; i < nr; ++i )
				rs[ i ] = i * blockSize + blockRadius + 1;
			rs[ nr ] = src.getHeight() - blockRadius - 1;
			break;
		default:
			rs = new int[ nr + 2 ];
			rs[ 0 ] = blockRadius + 1;
			for ( int i = 0; i < nr; ++i )
				rs[ i + 1 ] = i * blockSize + blockRadius + 1 + rm / 2;
			rs[ nr + 1 ] = src.getHeight() - blockRadius - 1;
		}
		
		final Apply< ? > apply;
		try
		{
			switch ( imp.getType() )
			{
			case ImagePlus.GRAY8:
				apply = new ByteApply( ( ByteProcessor )ip, src, dst, mask, boxXMin, boxYMin, boxXMax, boxYMax );
				break;
			case ImagePlus.GRAY16:
				apply = new ShortApply( ( ShortProcessor )ip, src, dst, mask, boxXMin, boxYMin, boxXMax, boxYMax );
				break;
			case ImagePlus.GRAY32:
				apply = new FloatApply( ( FloatProcessor )ip, src, dst, mask, boxXMin, boxYMin, boxXMax, boxYMax );
				break;
			case ImagePlus.COLOR_RGB:
				apply = new RGBApply( ( ColorProcessor )ip, src, dst, mask, boxXMin, boxYMin, boxXMax, boxYMax );
				break;
			default:
				apply = null;	
			}
		}
		catch ( Exception e )
		{
			IJ.error( e.getMessage() );
			return;
		}
		
		for ( int r = 0; r <= rs.length; ++r )
		{
			final int r0 = Math.max( 0, r - 1 );
			final int r1 = Math.min( rs.length - 1, r );
			final int dr = rs[ r1 ] - rs[ r0 ];
			
			hist = createHistogram( blockRadius, bins, cs[ 0 ], rs[ r0 ], src );
			tr = Util.createTransfer( hist, limit );
			if ( r0 == r1 )
				br = tr;
			else
			{
				hist = createHistogram( blockRadius, bins, cs[ 0 ], rs[ r1 ], src );
				br = Util.createTransfer( hist, limit );
			}
			
			for ( int c = 0; c <= cs.length; ++c )
			{
				final int c0 = Math.max( 0, c - 1 );
				final int c1 = Math.min( cs.length - 1, c );
				final int dc = cs[ c1 ] - cs[ c0 ];
				
				tl = tr;
				bl = br;
				
				if ( c0 != c1 )
				{
					hist = createHistogram( blockRadius, bins, cs[ c1 ], rs[ r0 ], src );
					tr = Util.createTransfer( hist, limit );
					if ( r0 == r1 )
						br = tr;
					else
					{
						hist = createHistogram( blockRadius, bins, cs[ c1 ], rs[ r1 ], src );
						br = Util.createTransfer( hist, limit );
					}
				}
				
				final int xMin = ( c == 0 ? 0 : cs[ c0 ] );
				final int yMin = ( r == 0 ? 0 : rs[ r0 ] );
				final int xMax = ( c < cs.length ? cs[ c1 ] : ip.getWidth() - 1 );
				final int yMax = ( r < rs.length ? rs[ r1 ] : ip.getHeight() - 1 );
				
				for ( int y = yMin; y < yMax; ++y )
				{
					final int o = y * ip.getWidth();
					final float wy = ( float )( rs[ r1 ] - y ) / dr;
					
					for ( int x = xMin; x < xMax; ++x )
					{
						final float wx = ( float )( cs[ c1 ] - x ) / dc;
						final int v = Util.roundPositive( src.get( o + x ) / 255.0f * bins );
						
						final float t00 = tl[ v ];
						final float t01 = tr[ v ];
						final float t10 = bl[ v ];
						final float t11 = br[ v ];
						
						final float t0, t1;
						if ( c0 == c1 )
						{
							t0 = t00;
							t1 = t10;
						}
						else
						{
							t0 = wx * t00 + ( 1.0f - wx ) * t01;
							t1 = wx * t10 + ( 1.0f - wx ) * t11;
						}
						
						final float t;
						if ( r0 == r1 )
							t = t0;
						else
							t = wy * t0 + ( 1.0f - wy ) * t1;
						
						dst.set( o + x, Math.max( 0, Math.min( 255, Util.roundPositive( t * 255.0f ) ) ) );
					}
				}
				/* multiply the current cell into ip */
				apply.apply(
						xMin,
						yMin,
						xMax,
						yMax );
				imp.updateAndDraw();
			}
		}
	}
}
