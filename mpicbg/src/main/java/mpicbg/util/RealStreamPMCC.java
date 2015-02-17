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
 * Calculate the PMMC of a stream of double pairs by tracking the
 * required sums.  Uses {@link RealSum} for optimal precision.
 *
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
public class RealStreamPMCC
{
	final protected RealSum sumX = new RealSum();
	final protected RealSum sumXX = new RealSum();
	final protected RealSum sumY = new RealSum();
	final protected RealSum sumYY = new RealSum();
	final protected RealSum sumXY = new RealSum();
	
	protected long n = 0;
	
	public long getN()
	{
		return n;
	}
	
	public void add(final double a, final double b)
	{
		++n;
		sumX.add( a );
		sumXX.add( a * a );
		sumY.add( b );
		sumYY.add( b * b );
		sumXY.add( a * b );
	}
	
	public double getPMCC()
	{
		final double suma = sumX.getSum();
		final double sumaa = sumXX.getSum();
		final double sumb = sumY.getSum();
		final double sumbb = sumYY.getSum();
		final double sumab = sumXY.getSum();
		
		return (n * sumab - suma * sumb) / Math.sqrt(n * sumaa - suma * suma) / Math.sqrt(n * sumbb - sumb * sumb);
	}
	
	public double getMeanX()
	{
		return sumX.getSum() / n;
	}
	
	public double getMeanY()
	{
		return sumY.getSum() / n;
	}
	
	public double getVarX()
	{
		final double sumXNormalized = sumX.getSum() / n;
		return sumXX.getSum() / n - sumXNormalized * sumXNormalized;
	}
	
	public double getVarY()
	{
		final double sumYNormalized = sumY.getSum() / n;
		return sumYY.getSum() / n - sumYNormalized * sumYNormalized;
	}
}
