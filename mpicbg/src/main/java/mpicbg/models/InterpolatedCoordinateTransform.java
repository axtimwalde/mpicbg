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
package mpicbg.models;


/**
 * Linearly interpolates between two independent
 * {@link CoordinateTransform CoordinateTransforms}.
 *
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
public class InterpolatedCoordinateTransform< A extends CoordinateTransform, B extends CoordinateTransform > implements CoordinateTransform
{
	private static final long serialVersionUID = 8356592128041276188L;

	final protected A a;
	final protected B b;
	protected double lambda;

	public InterpolatedCoordinateTransform( final A a, final B b, final double lambda )
	{
		this.a = a;
		this.b = b;
		this.lambda = lambda;
	}

	public A getA()
	{
		return a;
	}

	public B getB()
	{
		return b;
	}

	public double getLambda()
	{
		return lambda;
	}

	public void setLambda( final float lambda )
	{
		this.lambda = lambda;
	}


	@Override
	public double[] apply( final double[] location )
	{
		final double[] copy = location.clone();
		applyInPlace( copy );
		return copy;
	}

	@Override
	public void applyInPlace( final double[] location )
	{
		final double[] copy = b.apply( location );
		a.applyInPlace( location );

		for ( int d = 0; d < location.length; ++d )
		{
			final double dd = copy[ d ] - location[ d ];
			location[ d ] += lambda * dd;
		}
	}
}
