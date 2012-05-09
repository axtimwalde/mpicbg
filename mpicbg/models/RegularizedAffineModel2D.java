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
 * 
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
public class RegularizedAffineModel2D< M extends AbstractAffineModel2D< M >, R extends AbstractAffineModel2D< R > > extends AbstractModel< RegularizedAffineModel2D< M, R > > implements InvertibleCoordinateTransform
{
	final protected M model;
	final protected R regularizer;
	final protected AffineModel2D affine = new AffineModel2D();
	final protected float[] a = new float[ 6 ];
	final protected float[] r = new float[ 6 ];
	protected float lambda;
	protected float l1;
	
	public RegularizedAffineModel2D( final M model, final R regularizer, final float lambda )
	{
		this.model = model;
		this.regularizer = regularizer;
		this.lambda = lambda;
		l1 = 1.0f - lambda;
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
		return Math.max( model.getMinNumMatches(), regularizer.getMinNumMatches() );
	}

	@Override
	public < P extends PointMatch > void fit( final Collection< P > matches ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		model.fit( matches );
		regularizer.fit( matches );
		
		model.toArray( a );
		regularizer.toArray( r );
		for ( int i = 0; i < a.length; ++i )
			a[ i ] = a[ i ] * l1 + r[ i ] * lambda;
		
		affine.set( a[ 0 ], a[ 1 ], a[ 2 ], a[ 3 ], a[ 4 ], a[ 5 ] );
	}

	@Override
	public void set( final RegularizedAffineModel2D< M, R > m )
	{
		model.set( m.model );
		regularizer.set( m.regularizer );
		affine.set( m.affine );
		lambda = m.lambda;
		l1 = m.l1;
		cost = m.cost;
	}

	@Override
	public RegularizedAffineModel2D< M, R > copy()
	{
		final RegularizedAffineModel2D< M, R > copy = new RegularizedAffineModel2D< M, R >( model.copy(), regularizer.copy(), lambda );
		copy.affine.set( affine );
		copy.cost = cost;
		return copy;
	}

	@Override
	public float[] apply( final float[] location )
	{
		final float[] a = location.clone();
		applyInPlace( a );
		return a;
	}

	@Override
	public void applyInPlace( final float[] location )
	{
//		final float[] a = regularizer.apply( location );
//		model.applyInPlace( location );
//		
//		final float dx = a[ 0 ] - location[ 0 ];
//		final float dy = a[ 1 ] - location[ 1 ];
//		
//		location[ 0 ] += lambda * dx;
//		location[ 1 ] += lambda * dy;
		
		affine.applyInPlace( location );
	}

	@Override
	public float[] applyInverse( final float[] point ) throws NoninvertibleModelException
	{
		final float[] a = point.clone();
		applyInverseInPlace( a );
		return a;
	}

	@Override
	public void applyInverseInPlace( final float[] point ) throws NoninvertibleModelException
	{
//		final float[] a = regularizer.applyInverse( point );
//		model.applyInverseInPlace( point );
//		
//		final float dx = a[ 0 ] - point[ 0 ];
//		final float dy = a[ 1 ] - point[ 1 ];
//		
//		point[ 0 ] += lambda * dx;
//		point[ 1 ] += lambda * dy;
		
		affine.applyInverseInPlace( point );
	}

	@Override
	public InvertibleCoordinateTransform createInverse()
	{
		final RegularizedAffineModel2D< M, R > inverse = new RegularizedAffineModel2D< M, R >( model.createInverse(), regularizer.createInverse(), lambda );
		inverse.cost = cost;
		return inverse;
	}

	public AffineModel2D createAffineModel2D()
	{
		return affine.copy(); 
	}
}
