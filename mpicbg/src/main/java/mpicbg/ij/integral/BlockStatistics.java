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
public class BlockStatistics
{
	final protected DoubleIntegralImage sums;
	final protected DoubleIntegralImage sumsOfSquares;
	final protected FloatProcessor fp;
	
	final protected class RowIntegrator extends Thread
	{
		final protected double[] sum;
		final protected double[] sumOfSquares;
		final protected int n;
		final protected int w1;
		final protected int width;
		public int i;
		
		public RowIntegrator(
				final double[] sum,
				final double[] sumOfSquares,
				final int n,
				final int w1,
				final int width )
		{
			this.sum = sum;
			this.sumOfSquares = sumOfSquares;
			this.n = n;
			this.w1 = w1;
			this.width = width;
		}
		
		@Override
		public void run()
		{
			for ( int j = i + w1; j < n; ++j )
			{
				final int end = i + width;
				double s = sum[ j ] = fp.getf( i );
				double ss = sumOfSquares[ j ] = s * s;
				for ( ++i, ++j; i < end; ++i, ++j )
				{
					final float a = fp.getf( i );
					s += a;
					ss += a * a;
					sum[ j ] = s;
					sumOfSquares[ j ] = ss;
				}
			}
		}
	}
	
	final protected void integrateRows(
			final int w1,
			final int n,
			final int width,
			final double[] sum,
			final double[] sumOfSquares )
	{
		for ( int i = 0, j = w1; j < n; ++j )
		{
			final int end = i + width;
			double s = sum[ j ] = fp.getf( i );
			double ss = sumOfSquares[ j ] = s * s;
			for ( ++i, ++j; i < end; ++i, ++j )
			{
				final float a = fp.getf( i );
				s += a;
				ss += a * a;
				sum[ j ] = s;
				sumOfSquares[ j ] = ss;
			}
		}
	}
	
	final protected void integrateRowsParallel(
			final int w1,
			final int n,
			final int width,
			final int height,
			final double[] sum,
			final double[] sumOfSquares )
	{
//		final int rowsPerThread = ( int )Math.ceil( ( double )height / Runtime.getRuntime().availableProcessors() );
		for ( int i = 0, j = w1; j < n; ++j )
		{
			final int end = i + width;
			double s = sum[ j ] = fp.getf( i );
			double ss = sumOfSquares[ j ] = s * s;
			for ( ++i, ++j; i < end; ++i, ++j )
			{
				final float a = fp.getf( i );
				s += a;
				ss += a * a;
				sum[ j ] = s;
				sumOfSquares[ j ] = ss;
			}
		}
	}
	
	final static protected void integrateColumns(
			final int w1,
			final int w2,
			final int n1,
			final int n2,
			final double[] sum,
			final double[] sumOfSquares,
			final int w )
	{
		for ( int j = w1; j < w2; j -= n1 )
		{
			final int end = j + n2;
			
			double s = sum[ j ];
			double ss = sumOfSquares[ j ];
			for ( j += w; j < end; j += w )
			{
				s += sum[ j ];
				ss += sumOfSquares[ j ];
				
				sum[ j ] = s;
				sumOfSquares[ j ] = ss;
			}
		}
	}
	
	public BlockStatistics( final FloatProcessor fp )
	{
		this.fp = fp;
		
		final int width = fp.getWidth();
		final int height = fp.getHeight();
		
		final int w = width + 1;
		final int w1 = w + 1;
		final int w2 = w + w;
		
		final int n = w * height + w;
		final int n1 = n - w1;
		final int n2 = n1 - w + 2;
		
		final double[] sum = new double[ n ];
		final double[] sumOfSquares = new double[ n ];
		
		/* rows */
		integrateRows( w1, n, width, sum, sumOfSquares );
		
		/* columns */
		integrateColumns( w1, w2, n1, n2, sum, sumOfSquares, w );
		
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
