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
 * A 2d integral image (summed-area table) that allows for fast computation of
 * the sum of pixel values in a rectangular area.
 *
 * @author Stephan Saalfeld &lt;saalfeld@mpi-cbg.de&gt;
 */
final public class DoubleIntegralImage implements IntegralImage
{
	final private int width;
	final private int height;
	
	final private int w;
	final private int h;
	final private int w1;

	final private double[] sum;

	/**
	 * Package protected constructor with the original pixel data passed as a parameter.
	 *
	 * @param pixels original pixel data to be summed
	 * @param width width of the original data
	 * @param height height of the original data
	 */
	DoubleIntegralImage( final float[] pixels, final int width, final int height )
	{
		this.width = width;
		this.height = height;
		
		w = width + 1;
		w1 = w + 1;
		h = height + 1;
		
		final int n = w * height + w;
		sum = new double[ n ];

		/* rows */
		for (int j = 1; j < h; ++j) {
			double rowSum = 0;
			final int offset = (j - 1) * width;
			final int offsetSum = j * w + 1;

			for (int i = 0; i < width; ++i) {
				rowSum += pixels[offset + i];
				sum[offsetSum + i] = rowSum;
			}
		}

		/* columns */
		final double[] columnSum = new double[width];
		for (int j = 1; j < w; ++j) {
			final int offset = j * w + 1;

			for (int i = 0; i < height; ++i) {
				final int index = offset + i;
				columnSum[i] += sum[index];
				sum[index] = columnSum[i];
			}
		}
	}
	
	
	/**
	 * Package protected constructor with the area sums passed as a parameter.
	 * 
	 * @param sum area sums with size = (width + 1) * (height + 1)
	 * @param width width of the original data
	 * @param height heigth of the original data
	 */
	DoubleIntegralImage( final double[] sum, final int width, final int height )
	{
		this.width = width;
		this.height = height;
		
		w = width + 1;
		w1 = w + 1;
		h = height + 1;

		this.sum = sum;
	}
	

	/**
	 * Construct a new {@link DoubleIntegralImage} from a {@link FloatProcessor}.
	 *
	 * @param fp the {@link FloatProcessor} as a source of pixel data
	 */
	public DoubleIntegralImage( final FloatProcessor fp )
	{
		this( ( float[] )fp.getPixels(), fp.getWidth(), fp.getHeight() );
	}

	/**
	 * Construct a new {@link DoubleIntegralImage} from an {@link ImageProcessor}.
	 *
	 * @param ip the {@link ImageProcessor} as a source of pixel data
	 */
	public DoubleIntegralImage( final ImageProcessor ip )
	{
		this.width = ip.getWidth();
		this.height = ip.getHeight();

		w = width + 1;
		w1 = w + 1;
		h = height + 1;

		final int n = w * height + w;
		sum = new double[ n ];

		/* rows */
		for (int j = 1; j < h; ++j) {
			double rowSum = 0;
			final int offset = (j - 1) * width;
			final int offsetSum = j * w + 1;

			for (int i = 0; i < width; ++i) {
				rowSum += ip.getf(offset + i);
				sum[offsetSum + i] = rowSum;
			}
		}
		
		/* columns */
		final double[] columnSum = new double[width];
		for (int j = 1; j < w; ++j) {
			final int offset = j * w + 1;

			for (int i = 0; i < height; ++i) {
				final int index = offset + i;
				columnSum[i] += sum[index];
				sum[index] = columnSum[i];
			}
		}
	}
	
	@Override
	public int getWidth() { return width; }
	@Override
	public int getHeight() { return height; }

	/**
	 * Get the sum of the area from (0, 0) (inclusively) to (x, y) (inclusively).
	 *
	 * @param x minimum x coordinate
	 * @param y minimum y coordinate
	 * @return the sum of the area in the original pixel data
	 */
	public double getDoubleSum( final int x, final int y )
	{
		return sum[ y * w + w1 + x ];
	}

	/**
	 * Get the sum of the area from (xMin, yMin) (exclusively) to (xMax, yMax) (inclusively).
	 *
	 * @param xMin minimum x coordinate
	 * @param yMin minimum y coordinate
	 * @param xMax maximum x coordinate
	 * @param yMax maximum y coordinate
	 * @return the sum of the area in the original pixel data
	 */
	public double getDoubleSum( final int xMin, final int yMin, final int xMax, final int yMax )
	{
		final int y1w = yMin * w + w1;
		final int y2w = yMax * w + w1;
		return sum[ y1w + xMin ] + sum[ y2w + xMax ] - sum[ y1w + xMax ] - sum[ y2w + xMin ];
	}

	@Override
	public int getSum( final int xMin, final int yMin, final int xMax, final int yMax )
	{
		return Float.floatToIntBits( ( float )getDoubleSum( xMin, yMin, xMax, yMax ) );
	}
	
	@Override
	public int getScaledSum( final int xMin, final int yMin, final int xMax, final int yMax, final float scale )
	{
		final int y1w = yMin * w + w1;
		final int y2w = yMax * w + w1;
		return Float.floatToIntBits( ( float )( sum[ y1w + xMin ] + sum[ y2w + xMax ] - sum[ y1w + xMax ] - sum[ y2w + xMin ] ) * scale );
	}
	
	double[] getData()
	{
		return sum;
	}
	
	public FloatProcessor toProcessor()
	{
		final float[] pixels = new float[ width * height ];
		for ( int y = 0; y < height; ++y )
		{
			final int row = y * width;
			for ( int x = 0; x < width; ++x )
			{
				pixels[ row + x ] = ( float )getDoubleSum( x - 1, y - 1, x, y );
			}
		}
		return new FloatProcessor( width, height, pixels, null );
	}
}
