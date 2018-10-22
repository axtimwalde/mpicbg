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

import java.awt.geom.AffineTransform;
import java.util.Collection;

/**
 * 2D affine specialization of {@link InterpolatedModel}.  Implements
 * interpolation directly by linear matrix interpolation.
 *
 * No multiple inheritance in Java, so it cannot be an AffineModel2D
 * by itself.
 *
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
final public class InterpolatedAffineModel2D<
		A extends Model< A > & Affine2D< A >,
		B extends Model< B > & Affine2D< B > >
	extends InvertibleInterpolatedModel< A, B, InterpolatedAffineModel2D< A, B > >
	implements Affine2D< InterpolatedAffineModel2D< A, B > >, InvertibleBoundable
{
	private static final long serialVersionUID = -5646709386458263066L;

	final protected AffineModel2D affine = new AffineModel2D();
	final protected double[] afs = new double[ 6 ];
	final protected double[] bfs = new double[ 6 ];

	public InterpolatedAffineModel2D( final A model, final B regularizer, final double lambda )
	{
		super( model, regularizer, lambda );
		interpolate();
	}

	protected void interpolate()
	{
		a.toArray( afs );
		b.toArray( bfs );
		for ( int i = 0; i < afs.length; ++i )
			afs[ i ] = afs[ i ] * l1 + bfs[ i ] * lambda;

		affine.set( afs[ 0 ], afs[ 1 ], afs[ 2 ], afs[ 3 ], afs[ 4 ], afs[ 5 ] );
	}

	@Override
	public < P extends PointMatch > void fit( final Collection< P > matches ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		super.fit( matches );
		interpolate();
	}

	@Override
	public void set( final InterpolatedAffineModel2D< A, B > m )
	{
		super.set( m );
		affine.set( m.affine );
	}

	@Override
	public void reset()
	{
		super.reset();
		affine.reset();
	}

	@Override
	public InterpolatedAffineModel2D< A, B > copy()
	{
		final InterpolatedAffineModel2D< A, B > copy = new InterpolatedAffineModel2D< A, B >( a.copy(), b.copy(), lambda );
		copy.cost = cost;
		return copy;
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
		affine.applyInPlace( location );
	}

	@Override
	public double[] applyInverse( final double[] point ) throws NoninvertibleModelException
	{
		final double[] copy = point.clone();
		applyInverseInPlace( copy );
		return copy;
	}

	@Override
	public void applyInverseInPlace( final double[] point ) throws NoninvertibleModelException
	{
		affine.applyInverseInPlace( point );
	}

	@Override
	public InterpolatedAffineModel2D< A, B > createInverse()
	{
		final InterpolatedAffineModel2D< A, B > inverse = new InterpolatedAffineModel2D< A, B >( a.createInverse(), b.createInverse(), lambda );
		inverse.cost = cost;
		return inverse;
	}

	public AffineModel2D createAffineModel2D()
	{
		return affine.copy();
	}

	@Override
	public AffineTransform createAffine()
	{
		return affine.createAffine();
	}

	@Override
	public AffineTransform createInverseAffine()
	{
		return affine.createInverseAffine();
	}

	@Override
	public void preConcatenate( final InterpolatedAffineModel2D< A, B > affine2d )
	{
		affine.preConcatenate( affine2d.affine );
	}

	public void concatenate( final AffineModel2D affine2d )
	{
		affine.concatenate( affine2d );
	}

	public void preConcatenate( final AffineModel2D affine2d )
	{
		affine.preConcatenate( affine2d );
	}

	@Override
	public void concatenate( final InterpolatedAffineModel2D< A, B > affine2d )
	{
		affine.concatenate( affine2d.affine );
	}

	@Override
	public void toArray( final double[] data )
	{
		affine.toArray( data );
	}

	@Override
	public void toMatrix( final double[][] data )
	{
		affine.toMatrix( data );
	}

	@Override
	public void estimateBounds( final double[] min, final double[] max )
	{
		affine.estimateBounds( min, max );
	}

	@Override
	public void estimateInverseBounds( final double[] min, final double[] max ) throws NoninvertibleModelException
	{
		affine.estimateInverseBounds( min, max );
	}
}
