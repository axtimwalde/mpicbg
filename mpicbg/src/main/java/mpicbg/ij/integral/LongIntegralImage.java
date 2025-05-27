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

import ij.process.ImageProcessor;
import mpicbg.util.Util;

/**
 * <p>Summed area table using 64bit signed integer precision.  This table can
 * be used safely for 16bit unsigned integer precision images with a maximal
 * size of {@code >2<sup>31</sup>&nbsp;px} which is ImageJ's size limit due to
 * usage of a single basic type array for pixel storage.  For the squares of
 * 16bit unsigned integer precision images, the size limit is two pixels less
 * (2<sup>31</sup>-2&nbsp;px) which should not impose a practical limitation.
 * These limits are calculated for the extreme case that all pixels have the
 * maximum possible value.</p>
 * 
 * <p>Boolean or byte integer precision images and their squares are safe.</p> 
 * 
 * @author Stephan Saalfeld &lt;saalfeld@mpi-cbg.de&gt;
 */
final public class LongIntegralImage implements IntegralImage
{
	final private int width;
	final private int height;
	
	final private int w;
	final private int w1;

	final private long[] sum;
	
	LongIntegralImage( final int[] pixels, final int width, final int height )
	{
		this.width = width;
		this.height = height;
		
		w = width + 1;
		w1 = w + 1;
		final int h = height + 1;
		final int n = w * h;

		sum = new long[ n ];

		/* rows */
		for (int j = 1; j < h; ++j) {
			long rowSum = 0;
			final int offset = (j - 1) * width;
			final int offsetSum = j * w + 1;

			for (int i = 0; i < width; ++i) {
				rowSum += pixels[offset + i];
				sum[offsetSum + i] = rowSum;
			}
		}

		/* columns */
		final long[] columnSum = new long[width];
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
	LongIntegralImage( final long[] sum, final int width, final int height )
	{
		this.width = width;
		this.height = height;
		
		w = width + 1;
		w1 = w + 1;

		this.sum = sum;
	}
	
	public LongIntegralImage( final ImageProcessor ip )
	{
		this.width = ip.getWidth();
		this.height = ip.getHeight();

		w = width + 1;
		w1 = w + 1;
		final int h = height + 1;
		final int n = w * h;

		sum = new long[ n ];

		/* rows */
		for (int j = 1; j < h; ++j) {
			long rowSum = 0;
			final int offset = (j - 1) * width;
			final int offsetSum = j * w + 1;

			for (int i = 0; i < width; ++i) {
				rowSum += ip.get(offset + i);
				sum[offsetSum + i] = rowSum;
			}
		}

		/* columns */
		final long[] columnSum = new long[width];
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
	
	public long getLongSum( final int x, final int y )
	{
		return sum[ y * w + w1 + x ];
	}
	
	public long getLongSum( final int xMin, final int yMin, final int xMax, final int yMax )
	{
		final int y1w = yMin * w + w1;
		final int y2w = yMax * w + w1;
		return sum[ y1w + xMin ] + sum[ y2w + xMax ] - sum[ y1w + xMax ] - sum[ y2w + xMin ];
	}
	
	@Override
	public int getSum( final int xMin, final int yMin, final int xMax, final int yMax )
	{
		return ( int )getLongSum( xMin, yMin, xMax, yMax );
	}
	
	@Override
	public int getScaledSum( final int xMin, final int yMin, final int xMax, final int yMax, final float scale )
	{
		final int y1w = yMin * w + w1;
		final int y2w = yMax * w + w1;
		return Util.roundPos( ( sum[ y1w + xMin ] + sum[ y2w + xMax ] - sum[ y1w + xMax ] - sum[ y2w + xMin ] ) * scale );
	}
}
