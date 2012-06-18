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
 * 
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
final public class DoubleIntegralImage implements IntegralImage
{
	final private int width;
	final private int height;
	
	final private int w;
	final private int w1;

	final private double[] sum;
	
	DoubleIntegralImage( final float[] pixels, final int width, final int height )
	{
		this.width = width;
		this.height = height;
		
		w = width + 1;
		w1 = w + 1;
		
		final int w2 = w + w;
		
		final int n = w * height + w;
		final int n1 = n - w1;
		final int n2 = n1 - w + 2;
		
		sum = new double[ n ];

		/* rows */
		for ( int i = 0, j = w1; j < n; ++j )
		{
			final int end = i + width;
			double s = sum[ j ] = pixels[ i ];
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
			
			double s = sum[ j ];
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
	DoubleIntegralImage( final double[] sum, final int width, final int height )
	{
		this.width = width;
		this.height = height;
		
		w = width + 1;
		w1 = w + 1;

		this.sum = sum;
	}
	
	
	public DoubleIntegralImage( final FloatProcessor fp )
	{
		this( ( float[] )fp.getPixels(), fp.getWidth(), fp.getHeight() );
	}
	
	public DoubleIntegralImage( final ImageProcessor ip )
	{
		this.width = ip.getWidth();
		this.height = ip.getHeight();

		w = width + 1;
		w1 = w + 1;
		
		final int w2 = w + w;
		
		final int n = w * height + w;
		final int n1 = n - w1;
		final int n2 = n1 - w + 2;
		
		sum = new double[ n ];

		/* rows */
		for ( int i = 0, j = w1; j < n; ++j )
		{
			final int end = i + width;
			double s = sum[ j ] = ip.getf( i );
			for ( ++i, ++j; i < end; ++i, ++j )
			{
				s += ip.getf( i );
				sum[ j ] = s;
			}
		}
		
		/* columns */
		for ( int j = w1; j < w2; j -= n1 )
		{
			final int end = j + n2;
			
			double s = sum[ j ];
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
	
	final public double getDoubleSum( final int x, final int y )
	{
		return sum[ y * w + w1 + x ];
	}
	
	final public double getDoubleSum( final int xMin, final int yMin, final int xMax, final int yMax )
	{
		final int y1w = yMin * w + w1;
		final int y2w = yMax * w + w1;
		return sum[ y1w + xMin ] + sum[ y2w + xMax ] - sum[ y1w + xMax ] - sum[ y2w + xMin ];
	}

	@Override
	final public int getSum( final int xMin, final int yMin, final int xMax, final int yMax )
	{
		return Float.floatToIntBits( ( float )getDoubleSum( xMin, yMin, xMax, yMax ) );
	}
	
	@Override
	final public int getScaledSum( final int xMin, final int yMin, final int xMax, final int yMax, final float scale )
	{
		final int y1w = yMin * w + w1;
		final int y2w = yMax * w + w1;
		return Float.floatToIntBits( ( float )( sum[ y1w + xMin ] + sum[ y2w + xMax ] - sum[ y1w + xMax ] - sum[ y2w + xMin ] ) * scale );
	}
	
	final double[] getData()
	{
		return sum;
	}
	
	final public FloatProcessor toProcessor()
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
