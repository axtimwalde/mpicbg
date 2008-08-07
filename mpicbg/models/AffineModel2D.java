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
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 *
 */
package mpicbg.models;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.util.Collection;

/**
 * 2d-affine transformation models to be applied to points in 2d-space.
 * 
 * @version 0.3b
 * 
 */
public class AffineModel2D extends AbstractAffineModel2D< AffineModel2D >
{
	static final protected int MIN_NUM_MATCHES = 3;
	
	@Override
	final public int getMinNumMatches(){ return MIN_NUM_MATCHES; }
	
	final protected AffineTransform affine = new AffineTransform();
	final protected AffineTransform inverseAffine = new AffineTransform();
	final public AffineTransform getAffine(){ return affine; }
	final public AffineTransform getInverseAffine(){ return inverseAffine; }
	
	private AffineTransform inverseAffineRef = inverseAffine;
	
	//@Override
	final public float[] apply( final float[] point )
	{
		assert point.length == 2 : "2d affine transformations can be applied to 2d points only.";
		
		final float[] transformed = new float[ 2 ];
		affine.transform( point, 0, transformed, 0, 1 );
		return transformed;
	}
	
	//@Override
	final public void applyInPlace( final float[] point )
	{
		assert point.length == 2 : "2d affine transformations can be applied to 2d points only.";
		
		affine.transform( point, 0, point, 0, 1 );
	}
	
	//@Override
	final public float[] applyInverse( final float[] point ) throws NoninvertibleModelException
	{
		assert point.length == 2 : "2d affine transformations can be applied to 2d points only.";
		
		final float[] transformed = new float[ 2 ];
		try
		{
			inverseAffineRef.transform( point, 0, transformed, 0, 1 );
		}
		catch ( NullPointerException e )
		{
			throw new NoninvertibleModelException( e );
		}
		return transformed;
	}


	//@Override
	final public void applyInverseInPlace( final float[] point ) throws NoninvertibleModelException
	{
		assert point.length == 2 : "2d affine transformations can be applied to 2d points only.";
		
		try
		{
			inverseAffineRef.transform( point, 0, point, 0, 1 );
		}
		catch ( NullPointerException e )
		{
			throw new NoninvertibleModelException( e );
		}
	}
	
	@Override
	final public void fit( final Collection< PointMatch > matches )
		throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		if ( matches.size() < MIN_NUM_MATCHES )
			throw new NotEnoughDataPointsException( matches.size() + " data points are not enough to estimate a 2d affine model, at least " + MIN_NUM_MATCHES + " data points required." );
		
		// center of mass:
		float pcx = 0, pcy = 0;
		float qcx = 0, qcy = 0;
		
		double ws = 0.0;
		//int length = matches.size();
		
		for ( final PointMatch m : matches )
		{
			final float[] p = m.getP1().getL(); 
			final float[] q = m.getP2().getW(); 
			
			final float w = m.getWeight();
			ws += w;
			
			pcx += w * p[ 0 ];
			pcy += w * p[ 1 ];
			qcx += w * q[ 0 ];
			qcy += w * q[ 1 ];
		}
		pcx /= ws;
		pcy /= ws;
		qcx /= ws;
		qcy /= ws;
		
		// Closed form solution for M as implemented by Johannes Schindelin
		double a11, a12, a22;
		double b11, b12, b21, b22;
		a11 = a12 = a22 = b11 = b12 = b21 = b22 = 0;
		for ( final PointMatch m : matches )
		{
			final float[] p = m.getP1().getL();
			final float[] q = m.getP2().getW();
			final float w = m.getWeight();
			
			final float px = p[ 0 ] - pcx, py = p[ 1 ] - pcy;
			final float qx = q[ 0 ] - qcx, qy = q[ 1 ] - qcy;
			a11 += w * px * px;
			a12 += w * px * py;
			a22 += w * py * py;
			b11 += w * px * qx;
			b12 += w * px * qy;
			b21 += w * py * qx;
			b22 += w * py * qy;
		}
		
		// invert M
		final float det = ( float )( a11 * a22 - a12 * a12 );
		
		if ( det == 0 )
			throw new IllDefinedDataPointsException();
		
		final float m11 = ( float )( a22 * b11 - a12 * b21 ) / det;
		final float m12 = ( float )( a11 * b21 - a12 * b11 ) / det;
		final float m21 = ( float )( a22 * b12 - a12 * b22 ) / det;
		final float m22 = ( float )( a11 * b22 - a12 * b12 ) / det;
		
		final float tx = qcx - m11 * pcx - m12 * pcy;
		final float ty = qcy - m21 * pcx - m22 * pcy;
		
		affine.setTransform( m11, m21, m12, m22, tx, ty );
		invert();
	}

	/**
	 * TODO Not yet implemented ...
	 */
	@Override
	final public void shake( final float amount )
	{
		// TODO If you ever need it, please implement it...
	}

	@Override
	final public void set( final AffineModel2D m )
	{
		this.affine.setTransform( m.getAffine() );
		this.cost = m.getCost();
	}

	@Override
	final public AffineModel2D clone()
	{
		AffineModel2D m = new AffineModel2D();
		m.affine.setTransform( affine );
		m.cost = cost;
		return m;
	}
	
	final private void invert()
	{
		try
		{
			inverseAffine.setTransform( affine );
			inverseAffine.invert();
			inverseAffineRef = inverseAffine;
		}
		catch ( NoninvertibleTransformException e )
		{
			inverseAffineRef = null;
		}
	}
	
	@Override
	final public void preConcatenate( final AffineModel2D model )
	{
		affine.preConcatenate( model.getAffine() );
	}
	
	@Override
	final public void concatenate( final AffineModel2D model )
	{
		affine.concatenate( model.getAffine() );
	}
}
