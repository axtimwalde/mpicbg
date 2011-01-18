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

/**
 * 
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
final public class LongRGBIntegralImage implements IntegralImage
{
	final protected int width;
	final protected int height;

	final private int w;

	final protected long[] sumR;
	final protected long[] sumG;
	final protected long[] sumB;
	
	LongRGBIntegralImage( final int[] pixels, final int width, final int height )
	{
		this.width = width;
		this.height = height;
		
		w = width + 1;

		sumR = new long[ w * ( height + 1 ) ];
		sumG = new long[ w * ( height + 1 ) ];
		sumB = new long[ w * ( height + 1 ) ];
		
		long sR = 0;
		long sG = 0;
		long sB = 0;
		for ( int x = 0; x < width; ++x )
		{
			final int xw = w + x + 1;
			final int rgb = pixels[ x ];

			sR += ( rgb >> 16 ) & 0xff;;
			sG += ( rgb >> 8 ) & 0xff;
			sB += rgb & 0xff;

			sumR[ xw ] = sR;
			sumG[ xw ] = sG;
			sumB[ xw ] = sB;
		}
		for ( int y = 1; y < height; ++y )
		{
			final int ywidth = y * width;
			final int yw = y * w + w + 1;
			final int rgb = pixels[ ywidth ];
			final int k = yw - w;
			sumR[ yw ] = sumR[ k ] + ( ( rgb >> 16 ) & 0xff );
			sumG[ yw ] = sumG[ k ] + ( ( rgb >> 8 ) & 0xff );
			sumB[ yw ] = sumB[ k ] + ( rgb & 0xff );
			for ( int x = 1; x < width; ++x )
			{
				final int ywx = yw + x;
				final int a = ywx - w;
				final int b = ywx - 1;
				final int c = a - 1;
				final int rgbb = pixels[ ywidth + x ];
				sumR[ ywx ] = sumR[ a ] + sumR[ b ] + ( ( rgbb >> 16 ) & 0xff ) - sumR[ c ];
				sumG[ ywx ] = sumG[ a ] + sumG[ b ] + ( ( rgbb >> 8 ) & 0xff ) - sumG[ c ];
				sumB[ ywx ] = sumB[ a ] + sumB[ b ] + ( rgbb & 0xff ) - sumB[ c ];
			}
		}
	}

	public LongRGBIntegralImage( final ColorProcessor ip )
	{
		this( ( int[] )ip.getPixels(), ip.getWidth(), ip.getHeight() );
	}
	
	@Override
	final public int getWidth() { return width; }
	@Override
	final public int getHeight() { return height; }

	@Override
	final public int getSum( final int xMin, final int yMin, final int xMax, final int yMax )
	{
		final int y1w = yMin * w + w + 1;
		final int y2w = yMax * w + w + 1;
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
	final public int getScaledSum( final int xMin, final int yMin, final int xMax, final int yMax, final float scale )
	{
		final int y1w = yMin * w + w + 1;
		final int y2w = yMax * w + w + 1;
		final int a = y1w + xMin;
		final int b = y2w + xMax;
		final int c = y1w + xMax;
		final int d = y2w + xMin;
		
		final int r = Math.round( ( sumR[ a ] + sumR[ b ] - sumR[ c ] - sumR[ d ] ) * scale );
		final int g = Math.round( ( sumG[ a ] + sumG[ b ] - sumG[ c ] - sumG[ d ] ) * scale );
		final int x = Math.round( ( sumB[ a ] + sumB[ b ] - sumB[ c ] - sumB[ d ] ) * scale );
		
		return ( ( ( r << 8 ) | g ) << 8 ) | x;
	}
	
	@Override
	final public int getScaledSumDifference(
			final int xMin1, final int yMin1, final int xMax1, final int yMax1, final float scale1,
			final int xMin2, final int yMin2, final int xMax2, final int yMax2, final float scale2 )
	{
		final int y1w1 = yMin1 * w + w + 1;
		final int y2w1 = yMax1 * w + w + 1;
		final int a1 = y1w1 + xMin1;
		final int b1 = y2w1 + xMax1;
		final int c1 = y1w1 + xMax1;
		final int d1 = y2w1 + xMin1;
		
		final int y1w2 = yMin2 * w + w + 1;
		final int y2w2 = yMax2 * w + w + 1;
		final int a2 = y1w2 + xMin2;
		final int b2 = y2w2 + xMax2;
		final int c2 = y1w2 + xMax2;
		final int d2 = y2w2 + xMin2;
		
		final int r = Math.round( ( sumR[ a1 ] + sumR[ b1 ] - sumR[ c1 ] - sumR[ d1 ] ) * scale1 - ( sumR[ a2 ] + sumR[ b2 ] - sumR[ c2 ] - sumR[ d2 ] ) * scale2 );
		final int g = Math.round( ( sumG[ a1 ] + sumG[ b1 ] - sumG[ c1 ] - sumG[ d1 ] ) * scale1 - ( sumG[ a2 ] + sumG[ b2 ] - sumG[ c2 ] - sumG[ d2 ] ) * scale2 );
		final int x = Math.round( ( sumB[ a1 ] + sumB[ b1 ] - sumB[ c1 ] - sumB[ d1 ] ) * scale1 - ( sumB[ a2 ] + sumB[ b2 ] - sumB[ c2 ] - sumB[ d2 ] ) * scale2 );
		
		return ( ( ( r << 8 ) | g ) << 8 ) | x;
	}
}
