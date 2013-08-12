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
 * 1D affine specialization of {@link InterpolatedModel}.  Implements
 * interpolation directly by linear matrix interpolation.
 * 
 * No multiple inheritance in Java, so it cannot be an AffineModel1D
 * by itself. 
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
final public class InterpolatedAffineModel1D<
		A extends Model< A > & Affine1D< A >,
		B extends Model< B > & Affine1D< B > >
	extends InvertibleInterpolatedModel< A, B, InterpolatedAffineModel1D< A, B > >
	implements Affine1D< InterpolatedAffineModel1D< A, B > >, InvertibleBoundable
{
	private static final long serialVersionUID = 6872754667674831584L;
	
	final protected AffineModel1D affine = new AffineModel1D();
	final protected float[] afs = new float[ 2 ];
	final protected float[] bfs = new float[ 2 ];
	
	public InterpolatedAffineModel1D( final A model, final B regularizer, final float lambda )
	{
		super( model, regularizer, lambda );
		interpolate();
	}
	
	protected void interpolate()
	{
		a.toArray( afs );
		b.toArray( bfs );
		
		affine.set(
				afs[ 0 ] * l1 + bfs[ 0 ] * lambda,
				afs[ 1 ] * l1 + bfs[ 1 ] * lambda );
	}
	
	@Override
	public < P extends PointMatch > void fit( final Collection< P > matches ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		super.fit( matches );
		interpolate();
	}

	@Override
	public void set( final InterpolatedAffineModel1D< A, B > m )
	{
		super.set( m );
		if ( InterpolatedAffineModel1D.class.isInstance( m ) )
			affine.set( ( ( InterpolatedAffineModel1D< A, B > ) m ).affine );
	}

	@Override
	public InterpolatedAffineModel1D< A, B > copy()
	{
		final InterpolatedAffineModel1D< A, B > copy = new InterpolatedAffineModel1D< A, B >( a.copy(), b.copy(), lambda );
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
	public InterpolatedAffineModel1D< A, B > createInverse()
	{
		final InterpolatedAffineModel1D< A, B > inverse = new InterpolatedAffineModel1D< A, B >( a.createInverse(), b.createInverse(), lambda );
		inverse.cost = cost;
		return inverse;
	}

	public AffineModel1D createAffineModel1D()
	{
		return affine.copy();
	}

	@Override
	public void preConcatenate( final InterpolatedAffineModel1D< A, B > affine1d )
	{
		affine.preConcatenate( affine1d.affine );
	}

	public void concatenate( final AffineModel1D affine1d )
	{
		affine.concatenate( affine1d );
	}

	public void preConcatenate( final AffineModel1D affine1d )
	{
		affine.preConcatenate( affine1d );
	}

	@Override
	public void concatenate( final InterpolatedAffineModel1D< A, B > affine1d )
	{
		affine.concatenate( affine1d.affine );
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
	 * m0 m1
	 * 0   1
	 * </pre>
	 * 
	 * @param m0
	 * @param m1
	 */
	final public void set( final float m0, final float m1 )
	{
		affine.set( m0, m1 );
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
