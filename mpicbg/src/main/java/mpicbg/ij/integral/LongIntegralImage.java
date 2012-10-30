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
 * size of >2<sup>31</sup>&nbsp;px which is ImageJ's size limit due to usage
 * of a single basic type array for pixel storage.  For the squares of 16bit
 * unsigned integer precision images, the size limit is two pixels less
 * (2<sup>31</sup>-2&nbsp;px) which should not impose a practical limitation.
 * These limits are calculated for the extreme case that all pixels have the
 * maximum possible value.</p>
 * 
 * <p>Boolean or byte integer precision images and their squares are safe.</p> 
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
final public class LongIntegralImage implements IntegralImage
{
	final private int width;
	final private int height;
	
	final private int w;
	final private int w1;

	final protected long[] sum;
	
	LongIntegralImage( final int[] pixels, final int width, final int height )
	{
		this.width = width;
		this.height = height;
		
		w = width + 1;
		w1 = w + 1;
		
		final int w2 = w + w;
		
		final int n = w * height + w;
		final int n1 = n - w1;
		final int n2 = n1 - w + 2;
		
		sum = new long[ n ];

		/* rows */
		for ( int i = 0, j = w1; j < n; ++j )
		{
			final int end = i + width;
			long s = sum[ j ] = pixels[ i ];
			for ( ++i, ++j; i < end; ++i, ++j )
			{
				s += pixels[ i ];
				sum[ j ] = s;
			}
		}
		
		/* columns */
		for ( int j = w1; j < w2; j -= n1 )
		{
			final int end = j + n2;
			
			long s = sum[ j ];
			for ( j += w; j < end; j += w )
			{
				s += sum[ j ];
				sum[ j ] = s;
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
		
		final int w2 = w + w;
		
		final int n = w * height + w;
		final int n1 = n - w1;
		final int n2 = n1 - w + 2;
		
		sum = new long[ n ];

		/* rows */
		for ( int i = 0, j = w1; j < n; ++j )
		{
			final int end = i + width;
			long s = sum[ j ] = ip.get( i );
			for ( ++i, ++j; i < end; ++i, ++j )
			{
				s += ip.get( i );
				sum[ j ] = s;
			}
		}
		
		/* columns */
		for ( int j = w1; j < w2; j -= n1 )
		{
			final int end = j + n2;
			
			long s = sum[ j ];
			for ( j += w; j < end; j += w )
			{
				s += sum[ j ];
				sum[ j ] = s;
			}
		}
	}
	
	@Override
	final public int getWidth() { return width; }
	@Override
	final public int getHeight() { return height; }
	
	final public long getLongSum( final int x, final int y )
	{
		return sum[ y * w + w1 + x ];
	}
	
	final public long getLongSum( final int xMin, final int yMin, final int xMax, final int yMax )
	{
		final int y1w = yMin * w + w1;
		final int y2w = yMax * w + w1;
		return sum[ y1w + xMin ] + sum[ y2w + xMax ] - sum[ y1w + xMax ] - sum[ y2w + xMin ];
	}
	
	@Override
	final public int getSum( final int xMin, final int yMin, final int xMax, final int yMax )
	{
		return ( int )getLongSum( xMin, yMin, xMax, yMax );
	}
	
	@Override
	final public int getScaledSum( final int xMin, final int yMin, final int xMax, final int yMax, final float scale )
	{
		final int y1w = yMin * w + w1;
		final int y2w = yMax * w + w1;
		return Util.roundPos( ( sum[ y1w + xMin ] + sum[ y2w + xMax ] - sum[ y1w + xMax ] - sum[ y2w + xMin ] ) * scale );
	}
}
