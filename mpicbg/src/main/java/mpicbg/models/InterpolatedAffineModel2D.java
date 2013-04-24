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
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
final public class InterpolatedAffineModel2D<
		A extends Model< A > & Affine2D< A >,
		B extends Model< B > & Affine2D< B > >
	extends InvertibleInterpolatedModel< A, B, InterpolatedAffineModel2D< A, B > >
	implements Affine2D< InterpolatedAffineModel2D< A, B > >, InvertibleBoundable
{
	private static final long serialVersionUID = 3986603413957889626L;
	
	final protected AffineModel2D affine = new AffineModel2D();
	final protected float[] afs = new float[ 6 ];
	final protected float[] bfs = new float[ 6 ];
	
	public InterpolatedAffineModel2D( final A model, final B regularizer, final float lambda )
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
		if ( InterpolatedAffineModel2D.class.isInstance( m ) )
			affine.set( ( ( InterpolatedAffineModel2D< A, B > ) m ).affine );
	}

	@Override
	public InterpolatedAffineModel2D< A, B > copy()
	{
		final InterpolatedAffineModel2D< A, B > copy = new InterpolatedAffineModel2D< A, B >( a.copy(), b.copy(), lambda );
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
	
	/**
	 * Initialize the model such that the respective affine transform is:
	 * 
	 * <pre>
	 * m00 m01 m02
	 * m10 m11 m12
	 * 0   0   1
	 * </pre>
	 * 
	 * @param m00
	 * @param m10
	 * 
	 * @param m01
	 * @param m11
	 * 
	 * @param m02
	 * @param m12
	 */
	final public void set( final float m00, final float m10, final float m01, final float m11, final float m02, final float m12 )
	{
		affine.set( m00, m10, m01, m11, m02, m12 );
	}
	
	/**
	 * Initialize the model with the parameters of an {@link AffineTransform}.
	 * 
	 * @param a
	 */
	final public void set( final AffineTransform a )
	{
		affine.set( a );
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
