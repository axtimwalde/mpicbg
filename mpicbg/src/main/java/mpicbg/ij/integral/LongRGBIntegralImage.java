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

import ij.process.ColorProcessor;
import mpicbg.util.Util;

/**
 * A 2d integral image (summed-area table) that allows for fast computation of
 * the sum of pixel values in a rectangular area.
 *
 * @author Stephan Saalfeld &lt;saalfeld@mpi-cbg.de&gt;
 */
final public class LongRGBIntegralImage implements IntegralImage
{
	final private int width;
	final private int height;

	final private int w;
	final private int w1;

	final private long[] sumR;
	final private long[] sumG;
	final private long[] sumB;
	
	LongRGBIntegralImage( final int[] pixels, final int width, final int height )
	{
		this.width = width;
		this.height = height;
		
		w = width + 1;
		w1 = w + 1;
		final int h = height + 1;
		final int n = w * h;

		sumR = new long[ n ];
		sumG = new long[ n ];
		sumB = new long[ n ];

		/* rows */
		for (int j = 1; j < h; ++j) {
			long rowSumR = 0;
			long rowSumG = 0;
			long rowSumB = 0;
			final int offset = (j - 1) * width;
			final int offsetSum = j * w + 1;

			for (int i = 0; i < width; ++i) {
				final int rgb = pixels[offset + i];

				rowSumR += ( rgb >> 16 ) & 0xff;
				rowSumG += ( rgb >> 8 ) & 0xff;
				rowSumB += rgb & 0xff;

				sumR[offsetSum + i] = rowSumR;
				sumG[offsetSum + i] = rowSumG;
				sumB[offsetSum + i] = rowSumB;
			}
		}

		/* columns */
		final long[] columnSumR = new long[width];
		final long[] columnSumG = new long[width];
		final long[] columnSumB = new long[width];
		for (int j = 1; j < w; ++j) {
			final int offset = j * w + 1;

			for (int i = 0; i < height; ++i) {
				final int index = offset + i;

				columnSumR[i] += sumR[index];
				sumR[index] = columnSumR[i];
				columnSumG[i] += sumG[index];
				sumG[index] = columnSumG[i];
				columnSumB[i] += sumB[index];
				sumB[index] = columnSumB[i];
			}
		}
	}

	public LongRGBIntegralImage( final ColorProcessor ip )
	{
		this( ( int[] )ip.getPixels(), ip.getWidth(), ip.getHeight() );
	}
	
	
	@Override
	public int getWidth() { return width; }
	@Override
	public int getHeight() { return height; }
	

	/**
	 * Write the <em>r</em>,<em>g</em>,<em>b</em> sums at a specified
	 * <em>x</em>,<em>y</em> location into a passed array with &ge;3 fields.
	 *  
	 * @param x
	 * @param y
	 * @param sums
	 */
	public void longSums( final long[] sums, final int x, final int y )
	{
		final int i = y * w + w1 + x;
		
		sums[ 0 ] = sumR[ i ];
		sums[ 1 ] = sumG[ i ];
		sums[ 2 ] = sumB[ i ];
	}
	
	
	/**
	 * Write the <em>r</em>,<em>g</em>,<em>b</em> sums in a specified rectangle
	 * into a passed array with &ge;3 fields.
	 *  
	 * @param xMin
	 * @param yMin
	 * @param xMax
	 * @param yMax
	 * @param sums
	 */
	public void longSums( final long[] sums, final int xMin, final int yMin, final int xMax, final int yMax )
	{
		final int y1w = yMin * w + w1;
		final int y2w = yMax * w + w1;
		final int a = y1w + xMin;
		final int b = y2w + xMax;
		final int c = y1w + xMax;
		final int d = y2w + xMin;
		
		sums[ 0 ] = sumR[ a ] + sumR[ b ] - sumR[ c ] - sumR[ d ];
		sums[ 1 ] = sumG[ a ] + sumG[ b ] - sumG[ c ] - sumG[ d ];
		sums[ 2 ] = sumB[ a ] + sumB[ b ] - sumB[ c ] - sumB[ d ];
	}
	
	
	@Override
	public int getSum( final int xMin, final int yMin, final int xMax, final int yMax )
	{
		final int y1w = yMin * w + w1;
		final int y2w = yMax * w + w1;
		final int a = y1w + xMin;
		final int b = y2w + xMax;
		final int c = y1w + xMax;
		final int d = y2w + xMin;
		
		final int r = ( int )( sumR[ a ] + sumR[ b ] - sumR[ c ] - sumR[ d ] );
		final int g = ( int )( sumG[ a ] + sumG[ b ] - sumG[ c ] - sumG[ d ] );
		final int x = ( int )( sumB[ a ] + sumB[ b ] - sumB[ c ] - sumB[ d ] );
		
		return ( ( ( r << 8 ) | g ) << 8 ) | x;
	}
	
	@Override
	public int getScaledSum( final int xMin, final int yMin, final int xMax, final int yMax, final float scale )
	{
		final int y1w = yMin * w + w1;
		final int y2w = yMax * w + w1;
		final int a = y1w + xMin;
		final int b = y2w + xMax;
		final int c = y1w + xMax;
		final int d = y2w + xMin;
		
		final int r = Util.roundPos( ( sumR[ a ] + sumR[ b ] - sumR[ c ] - sumR[ d ] ) * scale );
		final int g = Util.roundPos( ( sumG[ a ] + sumG[ b ] - sumG[ c ] - sumG[ d ] ) * scale );
		final int x = Util.roundPos( ( sumB[ a ] + sumB[ b ] - sumB[ c ] - sumB[ d ] ) * scale );
		
		return ( ( ( r << 8 ) | g ) << 8 ) | x;
	}
}
