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
 * 
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
final public class IntIntegralImage implements IntegralImage
{
	final private int width;
	final private int height;
	
	final private int w;
	final private int w1;

	final protected int[] sum;
	
	IntIntegralImage( final byte[] pixels, final int width, final int height )
	{
		this.width = width;
		this.height = height;
		
		w = width + 1;
		w1 = w + 1;
		
		final int w2 = w + w;
		
		final int n = w * height + w;
		final int n1 = n - w1;
		final int n2 = n1 - w + 2;
		
		sum = new int[ n ];

		/* rows */
		for ( int i = 0, j = w1; j < n; ++j )
		{
			final int end = i + width;
			int s = sum[ j ] = pixels[ i ];
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
			
			int s = sum[ j ];
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
	 * @param height height of the original data
	 */
	IntIntegralImage( final int[] sum, final int width, final int height )
	{
		this.width = width;
		this.height = height;
		
		w = width + 1;
		w1 = w + 1;

		this.sum = sum;
	}
	
	public IntIntegralImage( final ImageProcessor ip )
	{
		this.width = ip.getWidth();
		this.height = ip.getHeight();

		w = width + 1;
		w1 = w + 1;
		
		final int w2 = w + w;
		
		final int n = w * height + w;
		final int n1 = n - w1;
		final int n2 = n1 - w + 2;
		
		sum = new int[ n ];

		/* rows */
		for ( int i = 0, j = w1; j < n; ++j )
		{
			final int end = i + width;
			int s = sum[ j ] = ip.get( i );
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
			
			int s = sum[ j ];
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
	
	final public int getIntSum( final int x, final int y )
	{
		return sum[ y * w + w1 + x ];
	}
	
	
	final public int getIntSum( final int xMin, final int yMin, final int xMax, final int yMax )
	{
		final int y1w = yMin * w + w1;
		final int y2w = yMax * w + w1;
		return sum[ y1w + xMin ] + sum[ y2w + xMax ] - sum[ y1w + xMax ] - sum[ y2w + xMin ];
	}
	
	
	@Override
	final public int getSum( final int xMin, final int yMin, final int xMax, final int yMax )
	{
		return getIntSum( xMin, yMin, xMax, yMax );
	}
	
	@Override
	final public int getScaledSum( final int xMin, final int yMin, final int xMax, final int yMax, final float scale )
	{
		final int y1w = yMin * w + w1;
		final int y2w = yMax * w + w1;
		return Util.roundPos( ( sum[ y1w + xMin ] + sum[ y2w + xMax ] - sum[ y1w + xMax ] - sum[ y2w + xMin ] ) * scale );
	}
}
