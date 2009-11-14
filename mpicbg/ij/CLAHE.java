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

import mpicbg.util.Util;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
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
 * @version 0.1a
 */
public class CLAHE implements PlugIn
{
	static private int gBlockRadius = 63;
	static private int gBins = 255;
	static private float gSlope = 3;

	final static public boolean setup()
	{
		final GenericDialog gd = new GenericDialog( "CLAHE" );
		gd.addNumericField( "blocksize : ", gBlockRadius * 2 + 1, 0 );
		gd.addNumericField( "histogram bins : ", gBins + 1, 0 );
		gd.addNumericField( "maximum slope : ", gSlope, 2 );
		
		gd.showDialog();
		
		if ( gd.wasCanceled() ) return false;
		
		gBlockRadius = ( ( int )gd.getNextNumber() - 1 ) / 2;
		gBins = ( int )gd.getNextNumber() - 1;
		gSlope = ( float )gd.getNextNumber();
		
		return true;
	}

	public void run( String arg )
	{
		final ImagePlus imp = IJ.getImage();
		
		if ( !setup() ) return;
		
		run( imp, gBlockRadius, gBins, gSlope );
	}

	public void run( final ImagePlus imp, final int blockRadius, final int bins, final float slope )
	{
		final ImageProcessor ip;
		if ( imp.getType() == ImagePlus.COLOR_256 )
		{
			ip = imp.getProcessor().convertToRGB();
			imp.setProcessor( imp.getTitle(), ip );
		}
		else
			ip = imp.getProcessor();
			
		final ByteProcessor src;
		if ( imp.getType() == ImagePlus.GRAY8 || imp.getType() == ImagePlus.COLOR_256 )
			src = ( ByteProcessor )ip.convertToByte( true ).duplicate();
		else
			src = ( ByteProcessor )ip.convertToByte( true );
		final ByteProcessor dst = ( ByteProcessor )src.duplicate();
		
		//dst.snapshot();
		
		//imp.setProcessor( imp.getTitle(), dst );
		for ( int y = 0; y < imp.getHeight(); ++y )
		{
			final int yMin = Math.max( 0, y - blockRadius );
			final int yMax = Math.min( imp.getHeight(), y + blockRadius + 1 );
			final int h = yMax - yMin;
			
			final int xMax0 = Math.min( imp.getWidth(), blockRadius + 1 );
			
			/* initially fill histogram */
			final int[] hist = new int[ bins + 1 ];
			final int[] clippedHist = new int[ bins + 1 ];
			for ( int yi = yMin; yi < yMax; ++yi )
				for ( int xi = 0; xi < xMax0; ++xi )
					++hist[ Util.round( src.get( xi, yi ) / 255.0f * bins ) ];
			
			for ( int x = 0; x < imp.getWidth(); ++x )
			{
				final int v = Util.round( src.get( x, y ) / 255.0f * bins );
				
				final int xMin = Math.max( 0, x - blockRadius );
				final int xMax = Math.min( imp.getWidth(), x + blockRadius + 1 );
				final int w = xMax - xMin;
				final int n = h * w;
				
				final int limit = ( int )( slope * n / bins + 0.5f );
				
				/* remove left behind values from histogram */
				if ( xMin > 0 )
				{
					final int xMin1 = xMin - 1;
					for ( int yi = yMin; yi < yMax; ++yi )
						--hist[ Util.round( src.get( xMin1, yi ) / 255.0f * bins ) ];						
				}
					
				/* add newly included values to histogram */
				if ( xMax < imp.getWidth() )
				{
					final int xMax1 = xMax - 1;
					for ( int yi = yMin; yi < yMax; ++yi )
						++hist[ Util.round( src.get( xMax1, yi ) / 255.0f * bins ) ];						
				}
				
				/* clip histogram and redistribute clipped entries */
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
						for ( int i = 0; i <= bins; i += s )
							++clippedHist[ i ];
					}
				}
				while ( clippedEntries != clippedEntriesBefore );
				
				
				/* build cdf of clipped histogram */
				int hMin = bins;
				for ( int i = 0; i < hMin; ++i )
					if ( clippedHist[ i ] != 0 ) hMin = i;
				
				int cdf = 0;
				for ( int i = hMin; i <= v; ++i )
					cdf += clippedHist[ i ];
				
				int cdfMax = cdf;
				for ( int i = v + 1; i <= bins; ++i )
					cdfMax += clippedHist[ i ];
				
				final int cdfMin = clippedHist[ hMin ];
				
				dst.set( x, y, Util.round( ( cdf - cdfMin ) / ( float )( cdfMax - cdfMin ) * 255.0f ) );
			}
			
			/* multiply the current row into ip */
			final int t = y * imp.getWidth();
			if ( imp.getType() == ImagePlus.GRAY8 )
			{
				for ( int x = 0; x < imp.getWidth(); ++x )
				{
					final int i = t + x;
					ip.set( i, dst.get( i ) );
				}
			}
			else if ( imp.getType() == ImagePlus.GRAY16 )
			{
				final int min = ( int )ip.getMin();
				for ( int x = 0; x < imp.getWidth(); ++x )
				{
					final int i = t + x;
					final int v = ip.get( i );
					final float a = ( float )dst.get( i ) / src.get( i );
					ip.set( i, Math.max( 0, Math.min( 65535, Util.round( a * ( v - min ) + min ) ) ) );
				}
			}
			else if ( imp.getType() == ImagePlus.GRAY32 )
			{
				final float min = ( float )ip.getMin();
				for ( int x = 0; x < imp.getWidth(); ++x )
				{
					final int i = t + x;
					final float v = ip.getf( i );
					final float a = ( float )dst.get( i ) / src.get( i );
					ip.setf( i, a * ( v - min ) + min );
				}
			}
			else if ( imp.getType() == ImagePlus.COLOR_RGB )
			{
				for ( int x = 0; x < imp.getWidth(); ++x )
				{
					final int i = t + x;
					final int argb = ip.get( i );
					final float a = ( float )dst.get( i ) / src.get( i );
					final int r = Math.max( 0, Math.min( 255, Util.round( a * ( ( argb >> 16 ) & 0xff ) ) ) );  
					final int g = Math.max( 0, Math.min( 255, Util.round( a * ( ( argb >> 8 ) & 0xff ) ) ) );
					final int b = Math.max( 0, Math.min( 255, Util.round( a * ( argb & 0xff ) ) ) );
					ip.set( i, ( r << 16 ) | ( g << 8 ) | b );
				}
			}
			imp.updateAndDraw();
		}	
	}
}
