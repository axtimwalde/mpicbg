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
 * 3D affine specialization of {@link InterpolatedModel}.  Implements
 * interpolation directly by linear matrix interpolation.
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
final public class InterpolatedAffineModel3D<
		A extends Model< A > & Affine3D< A >,
		B extends Model< B > & Affine3D< B > >
	extends InvertibleInterpolatedModel< A, B, InterpolatedAffineModel3D< A, B > >
	implements Affine3D< InterpolatedAffineModel3D< A, B > >, InvertibleBoundable
{
	private static final long serialVersionUID = 3523544383713333901L;
	
	final protected AffineModel3D affine = new AffineModel3D();
	final protected float[] afs = new float[ 12 ];
	final protected float[] bfs = new float[ 12 ];
	
	public InterpolatedAffineModel3D( final A model, final B regularizer, final float lambda )
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
		
		affine.set(
				afs[ 0 ], afs[ 3 ], afs[ 6 ], afs[ 9 ],
				afs[ 1 ], afs[ 4 ], afs[ 7 ], afs[ 10 ],
				afs[ 2 ], afs[ 5 ], afs[ 8 ], afs[ 11 ] );
	}
	
	@Override
	public < P extends PointMatch > void fit( final Collection< P > matches ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		super.fit( matches );
		interpolate();
	}

	@Override
	public void set( final InterpolatedAffineModel3D< A, B > m )
	{
		super.set( m );
		if ( InterpolatedAffineModel3D.class.isInstance( m ) )
			affine.set( ( ( InterpolatedAffineModel3D< A, B > ) m ).affine );
	}

	@Override
	public InterpolatedAffineModel3D< A, B > copy()
	{
		final InterpolatedAffineModel3D< A, B > copy = new InterpolatedAffineModel3D< A, B >( a.copy(), b.copy(), lambda );
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
		affine.applyInPlace( location );
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
		affine.applyInverseInPlace( point );
	}

	@Override
	public InterpolatedAffineModel3D< A, B > createInverse()
	{
		final InterpolatedAffineModel3D< A, B > inverse = new InterpolatedAffineModel3D< A, B >( a.createInverse(), b.createInverse(), lambda );
		inverse.cost = cost;
		return inverse;
	}

	public AffineModel3D createAffineModel3D()
	{
		return affine.copy();
	}

	@Override
	public void preConcatenate( final InterpolatedAffineModel3D< A, B > affine3d )
	{
		affine.preConcatenate( affine3d.affine );
	}

	@Override
	public void concatenate( final InterpolatedAffineModel3D< A, B > affine3d )
	{
		affine.concatenate( affine3d.affine );
	}

	@Override
	public void toArray( final float[] data )
	{
		affine.toArray( data );
	}

	@Override
	public void toArray( final double[] data )
	{
		affine.toArray( data );
	}

	@Override
	public void toMatrix( final float[][] data )
	{
		affine.toMatrix( data );
	}

	@Override
	public void toMatrix( final double[][] data )
	{
		affine.toMatrix( data );
	}

	@Override
	public void estimateBounds( final float[] min, final float[] max )
	{
		affine.estimateBounds( min, max );
	}

	@Override
	public void estimateInverseBounds( final float[] min, final float[] max ) throws NoninvertibleModelException
	{
		affine.estimateInverseBounds( min, max );
	}
}
