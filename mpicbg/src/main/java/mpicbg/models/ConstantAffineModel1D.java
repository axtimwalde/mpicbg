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
 * 1D affine specialization of {@link ConstantModel}.
 *
 * No multiple inheritance in Java, so it cannot be an AffineModel1D
 * by itself.
 *
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
final public class ConstantAffineModel1D< A extends Model< A > & Affine1D< A > & InvertibleBoundable >
	extends InvertibleConstantModel< A, ConstantAffineModel1D< A > >
	implements Affine1D< ConstantAffineModel1D< A > >, InvertibleBoundable
{
	private static final long serialVersionUID = -3540327692126579857L;

	public ConstantAffineModel1D( final A model )
	{
		super( model );
	}

	@Override
	public ConstantAffineModel1D< A > copy()
	{
		final ConstantAffineModel1D< A > copy = new ConstantAffineModel1D< A >( model.copy() );
		copy.cost = cost;
		return copy;
	}

	@Override
	public ConstantAffineModel1D< A > createInverse()
	{
		final ConstantAffineModel1D< A > inverse = new ConstantAffineModel1D< A >( model.createInverse() );
		inverse.cost = cost;
		return inverse;
	}

	public AffineModel1D createAffineModel1D()
	{
		final AffineModel1D copy = new AffineModel1D();
		final double[] data = new double[ 2 ];
		copy.set( data[ 0 ], data[ 1 ] );
		return copy;
	}

	@Override
	public void preConcatenate( final ConstantAffineModel1D< A > affine1d )
	{
		model.preConcatenate( affine1d.model );
	}

	@Override
	public void concatenate( final ConstantAffineModel1D< A > affine1d )
	{
		model.concatenate( affine1d.model );
	}

	@Override
	public void toArray( final double[] data )
	{
		model.toArray( data );
	}

	@Override
	public void toMatrix( final double[][] data )
	{
		model.toMatrix( data );
	}

	@Override
	public void estimateBounds( final double[] min, final double[] max )
	{
		model.estimateBounds( min, max );
	}

	@Override
	public void estimateInverseBounds( final double[] min, final double[] max ) throws NoninvertibleModelException
	{
		model.estimateInverseBounds( min, max );
	}
}
