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
 * {@link RealSum} implements a method to reduce numerical instabilities
 * when summing up a very large number of double precision numbers.  Numerical
 * problems occur when a small number is added to an already very large sum.
 * In such case, the reduced accuracy of the very large number may lead to the
 * small number being entirely ignored.  The method here stores and updates
 * intermediate sums for all power of two elements such that the final sum can
 * be generated from intermediate sums that result from equal number of
 * summands.
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
public class RealSum
{
	protected boolean[] flags;
	protected double[] sums;
	
	
	/**
	 * Create a new {@link RealSum}.  The fields for intermediate sums is
	 * initialized with a single element and expanded on demand as new
	 * elements are added.
	 */
	public RealSum()
	{
		flags = new boolean[ 1 ];
		sums = new double[ 1 ];
	}
	
	
	/**
	 * Create a new {@link RealSum}.  The fields for intermediate sums is
	 * initialized with a given number of elements and will only be expanded
	 * on demand as new elements are added and the number of existing elements
	 * is not sufficient.  This may be faster in cases where the required
	 * number of elements is known in prior.
	 * 
	 * @param capacity
	 */
	public RealSum( final int capacity )
	{
		final int ldu = Util.ldu( capacity ) + 1;
		flags = new boolean[ ldu ];
		sums = new double[ ldu ];
	}
	
	
	/**
	 * Get the current sum by summing up all intermediate sums.  Do not call
	 * this method repeatedly when the sum has not changed.
	 * 
	 * @return
	 */
	final public double getSum()
	{
		double sum = 0;
		for ( final double s : sums )
			sum += s;
		
		return sum;
	}
	
	
	final protected void expand( final double s )
	{
		final double[] oldSums = sums;
		sums = new double[ oldSums.length + 1 ];
		System.arraycopy( oldSums, 0, sums, 0, oldSums.length );
		sums[ oldSums.length ] = s;
		
		final boolean[] oldFlags = flags;
		flags = new boolean[ sums.length ];
		System.arraycopy( oldFlags, 0, flags, 0, oldFlags.length );
		flags[ oldSums.length ] = true;
	}
	

	/**
	 * Add an element to the sum.  All intermediate sums are updated and
	 * the capacity is increased on demand.
	 * 
	 * @param a the summand to be added
	 */
	final public void add( final double a )
	{
		int i = 0;
		double s = a;
		try
		{
			while ( flags[ i ] )
			{
				flags[ i ] = false;
				s += sums[ i ];
				sums[ i ] =  0.0;
				++i;
			}
			flags[ i ] = true;
			sums[ i ] = s;
			return;
		}
		catch ( final IndexOutOfBoundsException e )
		{
			expand( s );
		}
	}
}
