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

import ij.CompositeImage;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.util.ArrayList;

/**
 * &lsquot;Contrast Limited Adaptive Histogram Equalization&rsquot; as
 * described in
 * 
 * <br />BibTeX:
 * <pre>
 * &#64;article{zuiderveld94,
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
 * @version 0.3b
 */
public class FastFlat extends Flat
{
	/**
	 * Process an {@link ImagePlus} with a given set of parameters.  Create
	 * mask and bounding box from the {@link Roi} of that {@link ImagePlus} and
	 * the passed mask if any.  Process {@link CompositeImage CompositeImages}
	 * as such.
	 * 
	 * @param imp
	 * @param blockRadius
	 * @param bins
	 * @param slope
	 * @param mask can be null
	 * 
	 * @deprecated Use the instance method
	 *   {@link #getInstance()}.{@link #run(ImagePlus, int, int, float, ByteProcessor, boolean)}
	 *   instead.
	 */
	@Deprecated
	static public void run(
			final ImagePlus imp,
			final int blockRadius,
			final int bins,
			final float slope,
			final ByteProcessor mask )
	{
		getFastInstance().run( imp, blockRadius, bins, slope, mask, true );
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
				++hist[ mpicbg.util.Util.roundPos( src.get( row + x ) / 255.0f * bins ) ];
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
	@Override
	final protected void run(
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
			final ImageProcessor ip,
			final boolean composite,
			final ArrayList< Apply< ? > > appliers )
	{
		final boolean updatePerRow = imp.isVisible();
		final boolean updatePerCell = updatePerRow & !composite; //!< CompositeImage.updateAndDraw() is very slow
		
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
			
			final int yMin = ( r == 0 ? 0 : rs[ r0 ] );
			final int yMax = ( r < rs.length ? rs[ r1 ] : ip.getHeight() - 1 );
			
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
				final int xMax = ( c < cs.length ? cs[ c1 ] : ip.getWidth() - 1 );
				
				for ( int y = yMin; y < yMax; ++y )
				{
					final int o = y * ip.getWidth();
					final float wy = ( float )( rs[ r1 ] - y ) / dr;
					
					for ( int x = xMin; x < xMax; ++x )
					{
						final float wx = ( float )( cs[ c1 ] - x ) / dc;
						final int v = mpicbg.util.Util.roundPos( src.get( o + x ) / 255.0f * bins );
						
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
						
						dst.set( o + x, Math.max( 0, Math.min( 255, mpicbg.util.Util.roundPos( t * 255.0f ) ) ) );
					}
				}
				
				/* multiply the current cell into ip or the respective composite channels */
				if ( updatePerCell )
				{
					for ( final Apply< ? > apply : appliers )
						apply.apply(
							xMin,
							yMin,
							xMax,
							yMax );
					imp.updateAndDraw();
				}
			}
			if ( updatePerRow && !updatePerCell )
			{
				for ( final Apply< ? > apply : appliers )
					apply.apply(
						boxXMin,
						yMin,
						boxXMax,
						yMax );
				imp.updateAndDraw();
			}
		}
		if ( !updatePerRow )
		{
			for ( final Apply< ? > apply : appliers )
				apply.apply(
					boxXMin,
					boxYMin,
					boxXMax,
					boxYMax );
			imp.updateAndDraw();
		}
	}
}
