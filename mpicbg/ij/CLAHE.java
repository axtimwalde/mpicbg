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
package mpicbg.ij;

import java.awt.Rectangle;
import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.Undo;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.plugin.PlugIn;
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
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 */
public class CLAHE implements PlugIn
{
	static private int blockRadius = 63;
	static private int bins = 255;
	static private float slope = 3;
	static private ByteProcessor mask = null;
	
	
	/**
	 * Clip histogram and redistribute clipped entries.
	 * 
	 * @param hist source
	 * @param clippedHist target 
	 * @param limit clip limit
	 * @param bins number of bins
	 */
	final static private void clipHistogram(
			final int[] hist,
			final int[] clippedHist,
			final int limit,
			final int bins )
	{
		System.arraycopy( hist, 0, clippedHist, 0, hist.length );
		int clippedEntries = 0, clippedEntriesBefore;
		do
		{
			clippedEntriesBefore = clippedEntries;
			clippedEntries = 0;
			for ( int i = 0; i <= bins; ++i )
			{
				final int d = clippedHist[ i ] - limit;
				if ( d > 0 )
				{
					clippedEntries += d;
					clippedHist[ i ] = limit;
				}
			}
			
			final int d = clippedEntries / ( bins + 1 );
			final int m = clippedEntries % ( bins + 1 );
			for ( int i = 0; i <= bins; ++i)
				clippedHist[ i ] += d;
			
			if ( m != 0 )
			{
				final int s = bins / m;
				for ( int i = s / 2; i <= bins; i += s )
					++clippedHist[ i ];
			}
		}
		while ( clippedEntries != clippedEntriesBefore );
	}
	
	
	/**
	 * Get the CDF entry of a value.
	 * 
	 * @param v the value
	 * @param hist the histogram from which the CDF is collected
	 * @param bins
	 * @return
	 */
	final static private float transferValue(
			final int v,
			final int[] hist,
			final int bins )
	{
		int hMin = bins;
		for ( int i = 0; i < hMin; ++i )
			if ( hist[ i ] != 0 ) hMin = i;
		
		int cdf = 0;
		for ( int i = hMin; i <= v; ++i )
			cdf += hist[ i ];
		
		int cdfMax = cdf;
		for ( int i = v + 1; i <= bins; ++i )
			cdfMax += hist[ i ];
		
		final int cdfMin = hist[ hMin ];
		
		return ( cdf - cdfMin ) / ( float )( cdfMax - cdfMin );
	}
	
	
	/**
	 * Transfer a value through contrast limited histogram equalization.
	 * For efficiency, the histograms to be used are passed as parameters.
	 *  
	 * @param v
	 * @param hist
	 * @param clippedHist
	 * @param limit
	 * @param bins
	 * @return
	 */
	final static public float transferValue(
			final int v,
			final int[] hist,
			final int[] clippedHist,
			final int limit,
			final int bins )
	{
		clipHistogram( hist, clippedHist, limit, bins );
		return transferValue( v, clippedHist, bins );
	}
	
	
	final static private boolean setup( final ImagePlus imp )
	{
		final ArrayList< Integer > ids = new ArrayList< Integer >();
		final ArrayList< String > titles = new ArrayList< String >();
		
		titles.add( "*None*" );
		ids.add( -1 );
		for ( final int id : WindowManager.getIDList() )
		{
			final ImagePlus impId = WindowManager.getImage( id );
			if ( impId.getWidth() == imp.getWidth() && impId.getHeight() == imp.getHeight() )
			{
				titles.add( impId.getTitle() );
				ids.add( id );
			}
		}		
		
		final GenericDialog gd = new GenericDialog( "CLAHE" );
		gd.addNumericField( "blocksize : ", blockRadius * 2 + 1, 0 );
		gd.addNumericField( "histogram bins : ", bins + 1, 0 );
		gd.addNumericField( "maximum slope : ", slope, 2 );
		gd.addChoice( "mask : ", titles.toArray( new String[ 0 ] ),  titles.get( 0 ) );
        gd.addHelp( "http://pacific.mpi-cbg.de/wiki/index.php/Enhance_Local_Contrast_(CLAHE)" );
		
		gd.showDialog();
		
		if ( gd.wasCanceled() ) return false;
		
		blockRadius = ( ( int )gd.getNextNumber() - 1 ) / 2;
		bins = ( int )gd.getNextNumber() - 1;
		slope = ( float )gd.getNextNumber();
		final int maskId = ids.get( gd.getNextChoiceIndex() );
		if ( maskId != -1 ) mask = ( ByteProcessor )WindowManager.getImage( maskId ).getProcessor().convertToByte( true );
		else mask = null;
		
		return true;
	}
	
	
	final public void run( final String arg )
	{
		final ImagePlus imp = IJ.getImage();
		synchronized ( imp )
		{
			if ( !imp.isLocked() )
				imp.lock();
			else
			{
				IJ.error( "The image '" + imp.getTitle() + "' is in use currently.\nPlease wait until the process is done and try again." );
				return;
			}
		}
		
		if ( !setup( imp ) )
		{
			imp.unlock();
			return;
		}
		
		Undo.setup( Undo.TRANSFORM, imp );
		
		run( imp );
		imp.unlock();
	}
	
	/**
	 * Process an {@link ImagePlus} with the static parameters.  Create mask
	 * and bounding box from the {@link Roi} of that {@link ImagePlus}.
	 * 
	 * @param imp
	 */
	final static public void run( final ImagePlus imp )
	{
		run( imp, blockRadius, bins, slope, mask );
	}
	
	
	/**
	 * Process and {@link ImagePlus} with a given set of parameters.  Create
	 * mask and bounding box from the {@link Roi} of that {@link ImagePlus}.
	 * 
	 * @param imp
	 * @param blockRadius
	 * @param bins
	 * @param slope
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
						cropMaskPixels[ i ] = ( byte )roundPositive( ( cropMaskPixels[ i ] & 0xff ) * ( roiMaskPixels[ i ] & 0xff ) / 255.0f );
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
		
		for ( int y = box.y; y < boxYMax; ++y )
		{
			final int yMin = Math.max( 0, y - blockRadius );
			final int yMax = Math.min( imp.getHeight(), y + blockRadius + 1 );
			final int h = yMax - yMin;
			
			final int xMin0 = Math.max( 0, box.x - blockRadius );
			final int xMax0 = Math.min( imp.getWidth() - 1, box.x + blockRadius );
			
			/* initially fill histogram */
			final int[] hist = new int[ bins + 1 ];
			final int[] clippedHist = new int[ bins + 1 ];
			for ( int yi = yMin; yi < yMax; ++yi )
				for ( int xi = xMin0; xi < xMax0; ++xi )
					++hist[ roundPositive( src.get( xi, yi ) / 255.0f * bins ) ];
			
			for ( int x = box.x; x < boxXMax; ++x )
			{
				final int v = roundPositive( src.get( x, y ) / 255.0f * bins );
				
				final int xMin = Math.max( 0, x - blockRadius );
				final int xMax = x + blockRadius + 1;
				final int w = Math.min( imp.getWidth(), xMax ) - xMin;
				final int n = h * w;
				
				final int limit;
				if ( mask == null )
					limit = ( int )( slope * n / bins + 0.5f );
				else
					limit = ( int )( ( 1 + mask.get( x - box.x,  y - box.y ) / 255.0f * ( slope - 1 ) ) * n / bins + 0.5f );
				
				/* remove left behind values from histogram */
				if ( xMin > 0 )
				{
					final int xMin1 = xMin - 1;
					for ( int yi = yMin; yi < yMax; ++yi )
						--hist[ roundPositive( src.get( xMin1, yi ) / 255.0f * bins ) ];						
				}
					
				/* add newly included values to histogram */
				if ( xMax <= imp.getWidth() )
				{
					final int xMax1 = xMax - 1;
					for ( int yi = yMin; yi < yMax; ++yi )
						++hist[ roundPositive( src.get( xMax1, yi ) / 255.0f * bins ) ];						
				}
				
				dst.set( x, y, roundPositive( transferValue( v, hist, clippedHist, limit, bins ) * 255.0f ) );
			}
			
			/* multiply the current row into ip */
			final int t = y * imp.getWidth();
			if ( imp.getType() == ImagePlus.GRAY8 )
			{
				for ( int x = box.x; x < boxXMax; ++x )
				{
					final int i = t + x;
					ip.set( i, dst.get( i ) );
				}
			}
			else if ( imp.getType() == ImagePlus.GRAY16 )
			{
				final int min = ( int )ip.getMin();
				for ( int x = box.x; x < boxXMax; ++x )
				{
					final int i = t + x;
					final int v = ip.get( i );
					final float vSrc = src.get( i );
					final float a;
					if ( vSrc == 0 )
						a = 1.0f;
					else
						a = ( float )dst.get( i ) / vSrc;
					ip.set( i, Math.max( 0, Math.min( 65535, roundPositive( a * ( v - min ) + min ) ) ) );
				}
			}
			else if ( imp.getType() == ImagePlus.GRAY32 )
			{
				final float min = ( float )ip.getMin();
				for ( int x = box.x; x < boxXMax; ++x )
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
				for ( int x = box.x; x < boxXMax; ++x )
				{
					final int i = t + x;
					final int argb = ip.get( i );
					final float vSrc = src.get( i );
					final float a;
					if ( vSrc == 0 )
						a = 1.0f;
					else
						a = ( float )dst.get( i ) / vSrc;
					final int r = Math.max( 0, Math.min( 255, roundPositive( a * ( ( argb >> 16 ) & 0xff ) ) ) );  
					final int g = Math.max( 0, Math.min( 255, roundPositive( a * ( ( argb >> 8 ) & 0xff ) ) ) );
					final int b = Math.max( 0, Math.min( 255, roundPositive( a * ( argb & 0xff ) ) ) );
					ip.set( i, ( r << 16 ) | ( g << 8 ) | b );
				}
			}
			imp.updateAndDraw();
		}
	}
	
	final static private int roundPositive( float a )
	{
		return ( int )( a + 0.5f );
	}
}
