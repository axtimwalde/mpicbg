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

import java.util.ArrayList;

/**
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1a
 */
public class RealSum
{
	final private ArrayList< Boolean > flags;
	final private ArrayList< Double > sums;
	
	public RealSum()
	{
		flags = new ArrayList< Boolean >();
		sums = new ArrayList< Double >();
	}
	public RealSum( final int capacity )
	{
		final int ldu = Util.ldu( capacity ) + 1;
		flags = new ArrayList< Boolean >( ldu );
		sums = new ArrayList< Double >( ldu );
	}
	
	final public double getSum()
	{
//		System.out.print( "0" );
//		for ( final double s : sums )
//			System.out.print( " + " + s );
		
		double sum = 0;
		for ( final double s : sums )
			sum += s;
		
//		System.out.println( " = " + sum );
		
		return sum;
	}
	
	final public void add( final double a )
	{
		int i = 0;
		double s = a;
		try
		{
			while ( flags.get( i ) )
			{
				flags.set( i, false );
				s += sums.get( i );
				sums.set( i, 0.0 );
				++i;
			}
			flags.set( i, true );
			sums.set( i, s );
			return;
		}
		catch ( IndexOutOfBoundsException e )
		{
			flags.add( true );
			sums.add( s );
		}
	}
}
