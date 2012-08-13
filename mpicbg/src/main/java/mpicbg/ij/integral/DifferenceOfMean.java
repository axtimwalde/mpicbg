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

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/**
 * 
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
final public class DifferenceOfMean
{
	final private IntegralImage integral;
	final private ImageProcessor ip;
	final private int type;
	
	public DifferenceOfMean( final ColorProcessor ip )
	{
		this.ip = ip;
		integral = new LongRGBIntegralImage( ip );
		type = ImagePlus.COLOR_RGB;
	}
	
	public DifferenceOfMean( final ByteProcessor ip )
	{
		this.ip = ip;
		integral = new LongIntegralImage( ip );
		type = ImagePlus.GRAY8;
	}
	
	public DifferenceOfMean( final ShortProcessor ip )
	{
		this.ip = ip;
		integral = new LongIntegralImage( ip );
		type = ImagePlus.GRAY16;
	}

	public DifferenceOfMean( final FloatProcessor ip )
	{
		this.ip = ip;
		integral = new DoubleIntegralImage( ip );
		type = ImagePlus.GRAY32;
	}
	
	final static private int roundPositive( final float f )
	{
		return ( int )( f + 0.5f );
	}
	
	final static private int crop( final int a, final int min, final int max )
	{
		return a < min ? min : a > max ? max : a;
	}
	
	final static private void differenceOfMeanFloat( final FloatProcessor ip, final DoubleIntegralImage integral, final int blockRadiusX1, final int blockRadiusY1, final int blockRadiusX2, final int blockRadiusY2 )
	{
		final int w = ip.getWidth() - 1;
		final int h = ip.getHeight() - 1;
		
		for ( int y = 0; y <= h; ++y )
		{
			final int row = y * ip.getWidth();
			
			final int yMin1 = Math.max( -1, y - blockRadiusY1 - 1 );
			final int yMax1 = Math.min( h, y + blockRadiusY1 );
			final int bh1 = yMax1 - yMin1;
			
			final int yMin2 = Math.max( -1, y - blockRadiusY2 - 1 );
			final int yMax2 = Math.min( h, y + blockRadiusY2 );
			final int bh2 = yMax2 - yMin2;
			
			for ( int x = 0; x <= w; ++x )
			{
				final int xMin1 = Math.max( -1, x - blockRadiusX1 - 1 );
				final int xMax1 = Math.min( w, x + blockRadiusX1 );
				final float scale1 = 1.0f / ( xMax1 - xMin1 ) / bh1;
				
				final int xMin2 = Math.max( -1, x - blockRadiusX2 - 1 );
				final int xMax2 = Math.min( w, x + blockRadiusX2 );
				final float scale2 = 1.0f / ( xMax2 - xMin2 ) / bh2;
				
				ip.setf(
						row + x,
						( float )( integral.getDoubleSum( xMin1, yMin1, xMax1, yMax1 ) * scale1 - integral.getDoubleSum( xMin2, yMin2, xMax2, yMax2 ) * scale2 ) );
			}
		}
	}
	
	final static private void differenceOfMeanLong( final ImageProcessor ip, final LongIntegralImage integral, final int blockRadiusX1, final int blockRadiusY1, final int blockRadiusX2, final int blockRadiusY2, final int offset, final int min, final int max )
	{
		final int w = ip.getWidth() - 1;
		final int h = ip.getHeight() - 1;
		
		for ( int y = 0; y <= h; ++y )
		{
			final int row = y * ip.getWidth();
			
			final int yMin1 = Math.max( -1, y - blockRadiusY1 - 1 );
			final int yMax1 = Math.min( h, y + blockRadiusY1 );
			final int bh1 = yMax1 - yMin1;
			
			final int yMin2 = Math.max( -1, y - blockRadiusY2 - 1 );
			final int yMax2 = Math.min( h, y + blockRadiusY2 );
			final int bh2 = yMax2 - yMin2;
			
			for ( int x = 0; x <= w; ++x )
			{
				final int xMin1 = Math.max( -1, x - blockRadiusX1 - 1 );
				final int xMax1 = Math.min( w, x + blockRadiusX1 );
				final float scale1 = 1.0f / ( xMax1 - xMin1 ) / bh1;
				
				final int xMin2 = Math.max( -1, x - blockRadiusX2 - 1 );
				final int xMax2 = Math.min( w, x + blockRadiusX2 );
				final float scale2 = 1.0f / ( xMax2 - xMin2 ) / bh2;
				
				ip.set(
						row + x,
						roundPositive( integral.getLongSum( xMin1, yMin1, xMax1, yMax1 ) * scale1 - integral.getLongSum( xMin2, yMin2, xMax2, yMax2 ) * scale2 ) + offset );
			}
		}
	}
	
	final static private void differenceOfMeanLongRGB( final ColorProcessor ip, final LongRGBIntegralImage integral, final int blockRadiusX1, final int blockRadiusY1, final int blockRadiusX2, final int blockRadiusY2, final int offset )
	{
		final int w = ip.getWidth() - 1;
		final int h = ip.getHeight() - 1;
		
		final long[] rgb1 = new long[ 3 ];
		final long[] rgb2 = new long[ 3 ];
		
		for ( int y = 0; y <= h; ++y )
		{
			final int row = y * ip.getWidth();
			
			final int yMin1 = Math.max( -1, y - blockRadiusY1 - 1 );
			final int yMax1 = Math.min( h, y + blockRadiusY1 );
			final int bh1 = yMax1 - yMin1;
			
			final int yMin2 = Math.max( -1, y - blockRadiusY2 - 1 );
			final int yMax2 = Math.min( h, y + blockRadiusY2 );
			final int bh2 = yMax2 - yMin2;
			
			for ( int x = 0; x <= w; ++x )
			{
				final int xMin1 = Math.max( -1, x - blockRadiusX1 - 1 );
				final int xMax1 = Math.min( w, x + blockRadiusX1 );
				final float scale1 = 1.0f / ( xMax1 - xMin1 ) / bh1;
				
				final int xMin2 = Math.max( -1, x - blockRadiusX2 - 1 );
				final int xMax2 = Math.min( w, x + blockRadiusX2 );
				final float scale2 = 1.0f / ( xMax2 - xMin2 ) / bh2;
				
				integral.longSums( rgb1, xMin1, yMin1, xMax1, yMax1 );
				integral.longSums( rgb2, xMin2, yMin2, xMax2, yMax2 );
				
				final int r = crop( roundPositive( rgb1[ 0 ] * scale1 - rgb2[ 0 ] * scale2 ) + offset, 0, 255 );
				final int g = crop( roundPositive( rgb1[ 1 ] * scale1 - rgb2[ 1 ] * scale2 ) + offset, 0, 255 );
				final int b = crop( roundPositive( rgb1[ 2 ] * scale1 - rgb2[ 2 ] * scale2 ) + offset, 0, 255 );
				
				ip.set(	row + x, ( ( ( r << 8 ) | g ) << 8 ) | b );
			}
		}
	}
	
	final public void differenceOfMean( final int blockRadiusX1, final int blockRadiusY1, final int blockRadiusX2, final int blockRadiusY2 )
	{
		switch ( type )
		{
		case ImagePlus.GRAY32:
			differenceOfMeanFloat( ( FloatProcessor )ip, ( DoubleIntegralImage )integral, blockRadiusX1, blockRadiusY1, blockRadiusX2, blockRadiusY2 );
			break;
		case ImagePlus.GRAY8:
			differenceOfMeanLong( ip, ( LongIntegralImage )integral, blockRadiusX1, blockRadiusY1, blockRadiusX2, blockRadiusY2, 127, 0, 255 );
			break;
		case ImagePlus.GRAY16:
			differenceOfMeanLong( ip, ( LongIntegralImage )integral, blockRadiusX1, blockRadiusY1, blockRadiusX2, blockRadiusY2, 32767, 0, 65535 );
			break;
		case ImagePlus.COLOR_RGB:
			differenceOfMeanLongRGB( ( ColorProcessor )ip, ( LongRGBIntegralImage )integral, blockRadiusX1, blockRadiusY1, blockRadiusX2, blockRadiusY2, 127 );
			break;
		}
	}
	
	final public void differenceOfMean( final int blockRadius1, final int blockRadius2 )
	{
		differenceOfMean( blockRadius1, blockRadius1, blockRadius2, blockRadius2 );
	}
	
	
	/**
	 * Factory method that decides on the type of <code>ip</code> which
	 * {@link DifferenceOfMean} constructor to call.  Returns <code>null</code>
	 * for unknown types.
	 * 
	 * @param ip
	 * @return
	 */
	final static public DifferenceOfMean create( final ImageProcessor ip )
	{
		if ( FloatProcessor.class.isInstance( ip ) )
			return new DifferenceOfMean( ( FloatProcessor )ip );
		else if ( ByteProcessor.class.isInstance( ip ) )
			return new DifferenceOfMean( ( ByteProcessor )ip );
		else if ( ShortProcessor.class.isInstance( ip ) )
			return new DifferenceOfMean( ( ShortProcessor )ip );
		else if ( ColorProcessor.class.isInstance( ip ) )
			return new DifferenceOfMean( ( ColorProcessor )ip );
		else
			return null;
	}
}
