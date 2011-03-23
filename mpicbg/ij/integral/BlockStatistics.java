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
package mpicbg.ij.integral;

import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/**
 * Basic statistical properties of a block of pixels that can be accelerated
 * using {@link IntegralImage integral images}.  At this time, it is
 * implemented for single channel images at floating point accuracy only, RGB
 * is transferred into grey, integer accuracy into floating point accuracy
 * accordingly.  That is, for processing multiple color channels, separate them
 * into individual {@link ImageProcessor ImageProcessors} first and execute the
 * calculation on each of them individually.
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
final public class BlockStatistics
{
	final private DoubleIntegralImage sums;
	final private DoubleIntegralImage sumsOfSquares;
	final private FloatProcessor fp;
	
	public BlockStatistics( final FloatProcessor fp )
	{
		this.fp = fp;
		
		final int width = fp.getWidth();
		final int height = fp.getHeight();
		
		final int w = width + 1;

		final double[] sum = new double[ w * ( height + 1 ) ];
		final double[] sumOfSquares = new double[ sum.length ];
		
		double s = 0;
		double ss = 0;
		for ( int x = 0; x < width; )
		{
			final float a = fp.getf( x );
			final int i = ++x + w;
			
			s += a;
			sum[ i ] = s;
			
			ss += a * a;
			sumOfSquares[ i ] = ss;
		}
		for ( int y = 1; y < height; ++y )
		{
			final int ywidth = y * width;
			final int yw = y * w + w + 1;
			final int yww = yw - w;
			
			final float a = fp.getf( ywidth );
			
			sum[ yw ] = sum[ yww ] + a;
			sumOfSquares[ yw ] = sumOfSquares[ yww ] + a * a;
			for ( int x = 1; x < width; ++x )
			{
				final int ywx = yw + x;
				final int ywx1 = ywx - 1;
				final int ywxw = ywx - w;
				final int ywxw1 = ywxw - 1;
				
				final float b = fp.getf( ywidth + x );
				
				sum[ ywx ] = b + sum[ ywxw ] + sum[ ywx1 ] - sum[ ywxw1 ];
				sumOfSquares[ ywx ] = b * b + sumOfSquares[ ywxw ] + sumOfSquares[ ywx1 ] - sumOfSquares[ ywxw1 ];
			}
		}
		
		sums = new DoubleIntegralImage( sum, width, height );
		sumsOfSquares = new DoubleIntegralImage( sumOfSquares, width, height );
	}
	
	
	/**
	 * Set all pixels in <code>ip</code> to their block mean for a block
	 * with given radius.
	 * 
	 * @param blockRadiusX
	 * @param blockRadiusY
	 */
	final public void mean( final int blockRadiusX, final int blockRadiusY )
	{
		final int width = fp.getWidth();
		final int w = fp.getWidth() - 1;
		final int h = fp.getHeight() - 1;
		for ( int y = 0; y <= h; ++y )
		{
			final int row = y * width;
			final int yMin = Math.max( -1, y - blockRadiusY - 1 );
			final int yMax = Math.min( h, y + blockRadiusY );
			final int bh = yMax - yMin;
			for ( int x = 0; x <= w; ++x )
			{
				final int xMin = Math.max( -1, x - blockRadiusX - 1 );
				final int xMax = Math.min( w, x + blockRadiusX );
				final double scale = 1.0 / ( xMax - xMin ) / bh;
				fp.setf( row + x, ( float )( scale * sums.getDoubleSum( xMin, yMin, xMax, yMax ) ) );
			}
		}
	}
	
	final public void mean( final int blockRadius )
	{
		mean( blockRadius, blockRadius );
	}
	
	
	/**
	 * Set all pixels in <code>ip</code> to their block variance for a block
	 * with given radius.
	 * 
	 * @param blockRadiusX
	 * @param blockRadiusY
	 */
	final public void variance( final int blockRadiusX, final int blockRadiusY )
	{
		final int width = fp.getWidth();
		final int w = fp.getWidth() - 1;
		final int h = fp.getHeight() - 1;
		for ( int y = 0; y <= h; ++y )
		{
			final int row = y * width;
			final int yMin = Math.max( -1, y - blockRadiusY - 1 );
			final int yMax = Math.min( h, y + blockRadiusY );
			final int bh = yMax - yMin;
			for ( int x = 0; x <= w; ++x )
			{
				final int xMin = Math.max( -1, x - blockRadiusX - 1 );
				final int xMax = Math.min( w, x + blockRadiusX );
				final double scale = 1.0 / ( xMax - xMin ) / bh;
				final double sum = sums.getDoubleSum( xMin, yMin, xMax, yMax );
				final double var = scale * ( sumsOfSquares.getDoubleSum( xMin, yMin, xMax, yMax ) - sum * sum * scale );
				
				fp.setf( row + x, var < 0 ? 0 : ( float )var );
			}
		}
	}
	
	/**
	 * Set all pixels in <code>ip</code> to their block variance for a block
	 * with given radius.
	 * 
	 * @param blockRadius
	 */
	final public void variance( final int blockRadius )
	{
		variance( blockRadius, blockRadius );
	}
	
	
	/**
	 * Set all pixels in <code>ip</code> to their block STD for a block with
	 * given radius.
	 * 
	 * @param blockRadiusX
	 * @param blockRadiusY
	 */
	final public void std( final int blockRadiusX, final int blockRadiusY )
	{
		final int width = fp.getWidth();
		final int w = fp.getWidth() - 1;
		final int h = fp.getHeight() - 1;
		for ( int y = 0; y <= h; ++y )
		{
			final int row = y * width;
			final int yMin = Math.max( -1, y - blockRadiusY - 1 );
			final int yMax = Math.min( h, y + blockRadiusY );
			final int bh = yMax - yMin;
			for ( int x = 0; x <= w; ++x )
			{
				final int xMin = Math.max( -1, x - blockRadiusX - 1 );
				final int xMax = Math.min( w, x + blockRadiusX );
//				final float scale = 1.0f / ( xMax - xMin ) / bh;
//				final double sum = sums.getDoubleSum( xMin, yMin, xMax, yMax );
//				final double var = scale * ( sumsOfSquares.getDoubleSum( xMin, yMin, xMax, yMax ) - sum * sum * scale );
				final long bs = ( xMax - xMin ) * bh;
				final double scale1 = 1.0 / ( bs - 1 );
				final double scale2 = 1.0 / ( bs * bs - bs );
				final double sum = sums.getDoubleSum( xMin, yMin, xMax, yMax );
				final double var = scale1 * sumsOfSquares.getDoubleSum( xMin, yMin, xMax, yMax ) - scale2 * sum * sum;
				
				fp.setf( row + x, var < 0 ? 0 : ( float )Math.sqrt( var ) );
			}
		}
	}
	
	/**
	 * Set all pixels in <code>ip</code> to their block STD for a block with
	 * given radius.
	 * 
	 * @param blockRadiusX
	 * @param blockRadiusY
	 */
	final public void std( final int blockRadius )
	{
		std( blockRadius, blockRadius );
	}
	
	
	/**
	 * Set all pixels in <code>ip</code> to their block sample variance for a block
	 * with given radius.
	 * 
	 * @param blockRadiusX
	 * @param blockRadiusY
	 */
	final public void sampleVariance( final int blockRadiusX, final int blockRadiusY )
	{
		final int width = fp.getWidth();
		final int w = fp.getWidth() - 1;
		final int h = fp.getHeight() - 1;
		for ( int y = 0; y <= h; ++y )
		{
			final int row = y * width;
			final int yMin = Math.max( -1, y - blockRadiusY - 1 );
			final int yMax = Math.min( h, y + blockRadiusY );
			final int bh = yMax - yMin;
			for ( int x = 0; x <= w; ++x )
			{
				final int xMin = Math.max( -1, x - blockRadiusX - 1 );
				final int xMax = Math.min( w, x + blockRadiusX );
				final long bs = ( xMax - xMin ) * bh;
				final double scale1 = 1.0 / ( bs - 1 );
				final double scale2 = 1.0 / ( bs * bs - bs );
				final double sum = sums.getDoubleSum( xMin, yMin, xMax, yMax );
				final double var = scale1 * sumsOfSquares.getDoubleSum( xMin, yMin, xMax, yMax ) - scale2 * sum * sum;
				
				fp.setf( row + x, var < 0 ? 0 : ( float )var );
			}
		}
	}
	
	/**
	 * Set all pixels in <code>ip</code> to their block sample variance for a block
	 * with given radius.
	 * 
	 * @param blockRadius
	 */
	final public void sampleVariance( final int blockRadius )
	{
		sampleVariance( blockRadius, blockRadius );
	}
}
