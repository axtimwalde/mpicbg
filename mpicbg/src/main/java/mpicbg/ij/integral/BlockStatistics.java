/*-
 * #%L
 * MPICBG Core Library.
 * %%
 * Copyright (C) 2008 - 2025 Stephan Saalfeld et. al.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
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
 * @author Stephan Saalfeld &lt;saalfeld@mpi-cbg.de&gt;
 */
public class BlockStatistics
{
	final protected DoubleIntegralImage sums;
	final protected DoubleIntegralImage sumsOfSquares;
	final protected FloatProcessor fp;

	/**
	 * Deprecated: use {@link #integrateRows(int, int, double[], double[])} instead.
	 */
	@Deprecated
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
			final int w,
			final int h,
			final double[] sum,
			final double[] sumOfSquares )
	{
		final int width = w - 1;
		for (int j = 1; j < h; ++j) {
			double rowSum = 0;
			double rowSumOfSquares = 0;
			final int offset = (j - 1) * width;
			final int offsetSum = j * w + 1;

			for (int i = 0; i < width; ++i) {
				final float a = fp.getf(offset + i);
				rowSum += a;
				sum[offsetSum + i] = rowSum;
				rowSumOfSquares += a * a;
				sumOfSquares[offsetSum + i] = rowSumOfSquares;
			}
		}
	}

	/**
	 * Deprecated: use {@link #integrateRows(int, int, double[], double[])} instead.
	 */
	@Deprecated
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
	
	static protected void integrateColumns(
			final int w,
			final int h,
			final double[] sum,
			final double[] sumOfSquares )
	{
		final int width = w - 1;
		final int height = h - 1;
		final double[] columnSum = new double[width];
		final double[] columnSumOfSquares = new double[width];
		for (int j = 1; j < w; ++j) {
			final int offset = j * w + 1;

			for (int i = 0; i < height; ++i) {
				final int index = offset + i;
				columnSum[i] += sum[index];
				sum[index] = columnSum[i];
				columnSumOfSquares[i] += sumOfSquares[index];
				sumOfSquares[index] = columnSumOfSquares[i];
			}
		}
	}
	
	public BlockStatistics( final FloatProcessor fp )
	{
		this.fp = fp;
		
		final int width = fp.getWidth();
		final int height = fp.getHeight();
		
		final int w = width + 1;
		final int h = height + 1;
		final int n = w * h;

		final double[] sum = new double[ n ];
		final double[] sumOfSquares = new double[ n ];
		
		/* rows */
		integrateRows( w, h, sum, sumOfSquares );
		
		/* columns */
		integrateColumns( w, h, sum, sumOfSquares );
		
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
				final double bs = ( xMax - xMin ) * bh;
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
	 * @param blockRadius
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
				final double bs = ( xMax - xMin ) * bh;
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
