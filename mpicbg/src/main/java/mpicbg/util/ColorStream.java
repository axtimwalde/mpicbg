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
package mpicbg.util;



/**
 * Generate a stream of `random' saturated RGB colors with all colors being
 * maximally distinct from each other.  
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
public class ColorStream
{
	final static protected double goldenRatio = 1.0 / ( 0.5 * Math.sqrt( 5 ) + 0.5 );
	final static protected double[] rs = new double[]{ 1, 1, 0, 0, 0, 1, 1 };
	final static protected double[] gs = new double[]{ 0, 1, 1, 1, 0, 0, 0 };
	final static protected double[] bs = new double[]{ 0, 0, 0, 1, 1, 1, 0 };

	static long i = -1;
	
	final static protected int interpolate( final double[] xs, final int k, final int l, final double u, final double v )
	{
		return ( int )( ( v * xs[ k ] + u * xs[ l ] ) * 255.0 + 0.5 );
	}
	
	final static protected int argb( final int r, final int g, final int b )
	{
		return ( ( ( r << 8 ) | g ) << 8 ) | b | 0xff000000;
	}
	
	final static int get( final long index )
	{
		double x = goldenRatio * index;
		x -= ( long )x;
		x *= 6.0;
		final int k = ( int )x;
		final int l = k + 1;
		final double u = x - k;
		final double v = 1.0 - u;
		
		final int r = interpolate( rs, k, l, u, v );
		final int g = interpolate( gs, k, l, u, v );
		final int b = interpolate( bs, k, l, u, v );
		
		return argb( r, g, b );
	}
	
	final static public int next()
	{
		return get( ++i );
	}
}
