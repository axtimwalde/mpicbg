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

	final protected int w;
	final protected int w1;

	final protected long[] sumR;
	final protected long[] sumG;
	final protected long[] sumB;
	
	LongRGBIntegralImage( final int[] pixels, final int width, final int height )
	{
		this.width = width;
		this.height = height;
		
		w = width + 1;
		w1 = w + 1;

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
			final int yw = y * w + w1;
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
	

	/**
	 * Write the <em>r</em>,<em>g</em>,<em>b</em> sums at a specified
	 * <em>x</em>,<em>y</em> location into a passed array with &ge;3 fields.
	 *  
	 * @param x
	 * @param y
	 * @param sums
	 */
	final public void longSums( final long[] sums, final int x, final int y )
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
	final public void longSums( final long[] sums, final int xMin, final int yMin, final int xMax, final int yMax )
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
	final public int getSum( final int xMin, final int yMin, final int xMax, final int yMax )
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
	final public int getScaledSum( final int xMin, final int yMin, final int xMax, final int yMax, final float scale )
	{
		final int y1w = yMin * w + w1;
		final int y2w = yMax * w + w1;
		final int a = y1w + xMin;
		final int b = y2w + xMax;
		final int c = y1w + xMax;
		final int d = y2w + xMin;
		
		final int r = Util.roundPositive( ( sumR[ a ] + sumR[ b ] - sumR[ c ] - sumR[ d ] ) * scale );
		final int g = Util.roundPositive( ( sumG[ a ] + sumG[ b ] - sumG[ c ] - sumG[ d ] ) * scale );
		final int x = Util.roundPositive( ( sumB[ a ] + sumB[ b ] - sumB[ c ] - sumB[ d ] ) * scale );
		
		return ( ( ( r << 8 ) | g ) << 8 ) | x;
	}
}
