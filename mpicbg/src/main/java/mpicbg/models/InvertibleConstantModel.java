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
 * Invertible specialization of {@link ConstantModel}.
 *
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
public class InvertibleConstantModel<
		A extends Model< A > & InvertibleCoordinateTransform,
		M extends InvertibleConstantModel< A, M > > extends ConstantModel< A, M > implements InvertibleCoordinateTransform
{
	private static final long serialVersionUID = -8772404418680698608L;

	public InvertibleConstantModel( final A model )
	{
		super( model );
	}

	@Override
	public M copy()
	{
		@SuppressWarnings( "unchecked" )
		final M copy = ( M )new InvertibleConstantModel< A, M >( model.copy() );
		copy.cost = cost;
		return copy;
	}

	@Override
	public double[] applyInverse( final double[] location ) throws NoninvertibleModelException
	{
		final double[] copy = location.clone();
		applyInverseInPlace( copy );
		return copy;
	}

	@Override
	public void applyInverseInPlace( final double[] location ) throws NoninvertibleModelException
	{
		model.applyInPlace( location );
	}

	@Override
	public InvertibleCoordinateTransform createInverse()
	{
		@SuppressWarnings( "unchecked" )
		final InvertibleConstantModel< A, M > inverse = new InvertibleConstantModel< A, M >( ( A )model.createInverse() );
		inverse.cost = cost;
		return inverse;
	}
}
