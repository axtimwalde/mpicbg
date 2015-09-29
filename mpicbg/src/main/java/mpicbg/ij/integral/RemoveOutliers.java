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

/**
 * Remove saturated pixels by diffusing the neighbors in.
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1a
 */
public class RemoveOutliers extends BlockStatistics
{
	final protected FloatProcessor fpOriginal;
	
	public RemoveOutliers( final FloatProcessor fp )
	{
		super( fp );
		fpOriginal = ( FloatProcessor )fp.duplicate();
	}
	
	/**
	 * 
	 * @param ip
	 * @param v
	 */
	public void removeOutliers( final int blockRadiusX, final int blockRadiusY, final float meanFactor )
	{
		fp.setPixels( fpOriginal.getPixelsCopy() );
		
		final int width = fp.getWidth();
		final int height = fp.getHeight();
		final int wh = width * height;
		
		final int w = width - 1;
		final int h = height - 1;
		for ( int y = 0; y < height; ++y )
		{
			final int row = y * width;
			final int yMin = Math.max( -1, y - blockRadiusY - 1 );
			final int yMax = Math.min( h, y + blockRadiusY );
			final int bh = yMax - yMin;
			for ( int x = 0; x < width; ++x )
			{
				final int xMin = Math.max( -1, x - blockRadiusX - 1 );
				final int xMax = Math.min( w, x + blockRadiusX );
				final long bs = ( xMax - xMin ) * bh;
				final double scale = 1.0 / bs;
				final double scale1 = 1.0 / ( bs - 1 );
				final double scale2 = 1.0 / ( bs * bs - bs );
				final double sum = sums.getDoubleSum( xMin, yMin, xMax, yMax );
				final double var = scale1 * sumsOfSquares.getDoubleSum( xMin, yMin, xMax, yMax ) - scale2 * sum * sum;
				final int i = row + x;
				
				final float mean = ( float )( sum * scale );
				final float std = var < 0 ? 0 : ( float )Math.sqrt( var );
				final float v = fp.getf( i );
				final float min = mean - meanFactor * std;
				final float max = mean + meanFactor * std;
				
				if ( v < min || v > max )
					fp.setf( i, Float.NaN );
			}
		}
		
		final FloatProcessor fpCopy = ( FloatProcessor )fp.duplicate();
		
		int numSaturatedPixels = 0, numSaturatedPixelsBefore;
		do
		{
			numSaturatedPixelsBefore = numSaturatedPixels;
			numSaturatedPixels = 0;
			for ( int i = 0; i < wh; ++i )
			{
				final float v = fp.getf( i );
				
				if ( Float.isNaN( v ) )
				{
					++numSaturatedPixels;
					
					final int y = i / width;
					final int x = i % width;
					
					float s = 0;
					float n = 0;
					if ( y > 0 )
					{
						if ( x > 0 )
						{
							final float tl = fp.getf( x - 1, y - 1 );
							if ( !Float.isNaN( tl ) )
							{
								s += 0.5f * tl;
								n += 0.5f;
							}
						}
						final float t = fp.getf( x, y - 1 );
						if ( !Float.isNaN( t ) )
						{
							s += t;
							n += 1;
						}
						if ( x < w )
						{
							final float tr = fp.getf( x + 1, y - 1 );
							if ( !Float.isNaN( tr ) )
							{
								s += 0.5f * tr;
								n += 0.5f;
							}
						}
					}
					
					if ( x > 0 )
					{
						final float l = fp.getf( x - 1, y );
						if ( !Float.isNaN( l ) )
						{
							s += l;
							n += 1;
						}
					}
					if ( x < w )
					{
						final float r = fp.getf( x + 1, y );
						if ( !Float.isNaN( r ) )
						{
							s += r;
							n += 1;
						}
					}
					
					if ( y < h )
					{
						if ( x > 0 )
						{
							final float bl = fp.getf( x - 1, y + 1 );
							if ( !Float.isNaN( bl ) )
							{
								s += 0.5f * bl;
								n += 0.5f;
							}
						}
						final float b = fp.getf( x, y + 1 );
						if ( !Float.isNaN( b ) )
						{
							s += b;
							n += 1;
						}
						if ( x < w )
						{
							final float br = fp.getf( x + 1, y + 1 );
							if ( !Float.isNaN( br ) )
							{
								s += 0.5f * br;
								n += 0.5f;
							}
						}
					}
					
					if ( n > 0 )
						fpCopy.setf( i, s / n );
				}
			}
			fp.setPixels( fpCopy.getPixelsCopy() );
		}
		while ( numSaturatedPixels != numSaturatedPixelsBefore );
	}
}
