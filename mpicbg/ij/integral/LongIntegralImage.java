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

/**
 * 
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
final public class LongIntegralImage implements IntegralImage
{
	final private int width;
	final private int height;
	
	final private int w;

	final protected long[] sum;
	
	LongIntegralImage( final int[] pixels, final int width, final int height )
	{
		this.width = width;
		this.height = height;
		
		w = width + 1;

		sum = new long[ w * ( height + 1 ) ];

		long s = 0;
		for ( int x = 0; x < width; )
		{
			s += pixels[ x ];
			sum[ ++x + w ] = s;
		}
		for ( int y = 1; y < height; ++y )
		{
			final int ywidth = y * width;
			final int yw = y * w + w + 1;
			sum[ yw ] = sum[ yw - w ] + pixels[ ywidth ];
			for ( int x = 1; x < width; ++x )
			{
				final int ywx = yw + x;
				sum[ ywx ] = sum[ ywx - w ] + sum[ ywx - 1 ] + pixels[ ywidth + x ] - sum[ ywx - w - 1 ];
			}
		}
	}
	
	public LongIntegralImage( final ImageProcessor ip )
	{
		this.width = ip.getWidth();
		this.height = ip.getHeight();

		w = width + 1;

		sum = new long[ w * ( height + 1 ) ];

		long s = 0;
		for ( int x = 0; x < width; )
		{
			s += ip.get( x );
			sum[ ++x + w ] = s;
		}
		for ( int y = 1; y < height; ++y )
		{
			final int ywidth = y * width;
			final int yw = y * w + w + 1;
			sum[ yw ] = sum[ yw - w ] + ip.get( ywidth );
			for ( int x = 1; x < width; ++x )
			{
				final int ywx = yw + x;
				sum[ ywx ] = sum[ ywx - w ] + sum[ ywx - 1 ] + ip.get( ywidth + x ) - sum[ ywx - w - 1 ];
			}
		}
	}
	
	final static private int roundPositive( final float f )
	{
		return ( int )( f + 0.5f );
	}
	
	@Override
	final public int getWidth() { return width; }
	@Override
	final public int getHeight() { return height; }

	final public long getLongSum( final int xMin, final int yMin, final int xMax, final int yMax )
	{
		final int y1w = yMin * w + w + 1;
		final int y2w = yMax * w + w + 1;
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
		final int y1w = yMin * w + w + 1;
		final int y2w = yMax * w + w + 1;
		return roundPositive( ( sum[ y1w + xMin ] + sum[ y2w + xMax ] - sum[ y1w + xMax ] - sum[ y2w + xMin ] ) * scale );
	}
	
	@Override
	final public int getScaledSumDifference(
			final int xMin1, final int yMin1, final int xMax1, final int yMax1, final float scale1,
			final int xMin2, final int yMin2, final int xMax2, final int yMax2, final float scale2 )
	{
		final int y1w1 = yMin1 * w + w + 1;
		final int y2w1 = yMax1 * w + w + 1;
		
		final int y1w2 = yMin2 * w + w + 1;
		final int y2w2 = yMax2 * w + w + 1;
		
		return Math.round(
				( sum[ y1w1 + xMin1 ] + sum[ y2w1 + xMax1 ] - sum[ y1w1 + xMax1 ] - sum[ y2w1 + xMin1 ] ) * scale1 -
				( sum[ y1w2 + xMin2 ] + sum[ y2w2 + xMax2 ] - sum[ y1w2 + xMax2 ] - sum[ y2w2 + xMin2 ] ) * scale2 );
	}
}
