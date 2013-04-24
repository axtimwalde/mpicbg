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

import java.util.Collection;

/**
 * Linearly interpolates between two independent models.  We use this as a base
 * class for regularizing higher order models by lower order models in the
 * context of global optimization.
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
public class InterpolatedModel< A extends Model< A >, B extends Model< B >, M extends InterpolatedModel< A, B, M > > extends AbstractModel< M >
{
	private static final long serialVersionUID = 536894588954163993L;
	
	final protected A a;
	final protected B b;
	protected float lambda;
	protected float l1;
	
	public InterpolatedModel( final A a, final B b, final float lambda )
	{
		this.a = a;
		this.b = b;
		this.lambda = lambda;
		l1 = 1.0f - lambda;
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
		l1 = 1.0f - lambda;
	}
	
	@Override
	public int getMinNumMatches()
	{
		return Math.max( a.getMinNumMatches(), b.getMinNumMatches() );
	}

	@Override
	public < P extends PointMatch > void fit( final Collection< P > matches ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		a.fit( matches );
		b.fit( matches );
	}

	@Override
	public void set( final M m )
	{
		a.set( m.a );
		b.set( m.b );
		lambda = m.lambda;
		l1 = m.l1;
		cost = m.cost;
	}

	@Override
	public M copy()
	{
		@SuppressWarnings( "unchecked" )
		final M copy = ( M )new InterpolatedModel< A, B, M >( a.copy(), b.copy(), lambda );
		copy.cost = cost;
		return copy;
	}

	@Override
	public float[] apply( final float[] location )
	{
		final float[] copy = location.clone();
		applyInPlace( copy );
		return copy;
	}

	@Override
	public void applyInPlace( final float[] location )
	{
		final float[] copy = b.apply( location );
		a.applyInPlace( location );
		
		for ( int d = 0; d < location.length; ++d )
		{
			final float dd = copy[ d ] - location[ d ];
			location[ d ] += lambda * dd;
		}
	}
}
