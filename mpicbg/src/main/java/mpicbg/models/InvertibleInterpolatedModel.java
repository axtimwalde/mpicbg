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
 * Invertible specialization of {@link InterpolatedModel}. 
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
public class InvertibleInterpolatedModel<
		A extends Model< A > & InvertibleCoordinateTransform,
		B extends Model< B > & InvertibleCoordinateTransform,
		M extends InvertibleInterpolatedModel< A, B, M > > extends InterpolatedModel< A, B, M > implements InvertibleCoordinateTransform
{
	private static final long serialVersionUID = -8474223426611769525L;

	public InvertibleInterpolatedModel( final A a, final B b, final float lambda )
	{
		super( a, b, lambda );
	}
	
	@Override
	public M copy()
	{
		@SuppressWarnings( "unchecked" )
		final M copy = ( M )new InvertibleInterpolatedModel< A, B, M >( a.copy(), b.copy(), lambda );
		copy.cost = cost;
		return copy;
	}

	@Override
	public float[] applyInverse( final float[] point ) throws NoninvertibleModelException
	{
		final float[] copy = point.clone();
		applyInverseInPlace( copy );
		return copy;
	}

	@Override
	public void applyInverseInPlace( final float[] point ) throws NoninvertibleModelException
	{
		final float[] copy = b.applyInverse( point );
		a.applyInverseInPlace( point );
		
		for ( int d = 0; d < point.length; ++d )
		{
			final float dd = copy[ d ] - point[ d ];
			point[ d ] += lambda * dd;
		}
	}

	@Override
	public InvertibleCoordinateTransform createInverse()
	{
		@SuppressWarnings( "unchecked" )
		final InvertibleInterpolatedModel< A, B, M > inverse = new InvertibleInterpolatedModel< A, B, M >( ( A )a.createInverse(), ( B )b.createInverse(), lambda );
		inverse.cost = cost;
		return inverse;
	}
}
