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
import java.util.Collection;

import Jama.Matrix;

import mpicbg.util.Matrix3x3;

/**
 * 2d-affine transformation models to be applied to points in 2d-space.
 * 
 * @version 0.3b
 * 
 */
public class AffineModel2D extends AbstractAffineModel2D< AffineModel2D >
{
	static final protected int MIN_NUM_MATCHES = 3;
	
	private double m00 = 1.0;
	private double m10 = 0.0;
	private double m01 = 0.0;
	private double m11 = 1.0;
	private double m02 = 0.0;
	private double m12 = 0.0;
	
	private double i00 = 1.0;
	private double i10 = 0.0;
	private double i01 = 0.0;
	private double i11 = 1.0;
	private double i02 = 0.0;
	private double i12 = 0.0;
	
	private boolean isInvertible = true;
	
	@Override
	final public int getMinNumMatches(){ return MIN_NUM_MATCHES; }
	
	@Override
	final public AffineTransform createAffine(){ return new AffineTransform( m00, m10, m01, m11, m02, m12 ); }
	
	@Override
	final public AffineTransform createInverseAffine(){ return new AffineTransform( i00, i10, i01, i11, i02, i12 ); }
	
	//@Override
	final public float[] apply( final float[] l )
	{
		assert l.length == 2 : "2d affine transformations can be applied to 2d points only.";
		
		final float[] transformed = new float[ 2 ];
		applyInPlace( transformed );
		return transformed;
	}
	
	//@Override
	final public void applyInPlace( final float[] l )
	{
		assert l.length == 2 : "2d affine transformations can be applied to 2d points only.";
		
		l[ 0 ] = ( float )( l[ 0 ] * m00 + l[ 1 ] * m01 + m02 );
		l[ 1 ] = ( float )( l[ 0 ] * m10 + l[ 1 ] * m11 + m12 );
	}
	
	//@Override
	final public float[] applyInverse( final float[] l ) throws NoninvertibleModelException
	{
		assert l.length == 2 : "2d affine transformations can be applied to 2d points only.";
		
		final float[] transformed = new float[ 2 ];
		applyInverseInPlace( transformed );
		return transformed;
	}


	//@Override
	final public void applyInverseInPlace( final float[] l ) throws NoninvertibleModelException
	{
		assert l.length == 2 : "2d affine transformations can be applied to 2d points only.";
		
		if ( isInvertible )
		{
			l[ 0 ] = ( float )( l[ 0 ] * i00 + l[ 1 ] * i01 + i02 );
			l[ 1 ] = ( float )( l[ 0 ] * i10 + l[ 1 ] * i11 + i12 );
		}
		else
			throw new NoninvertibleModelException( "Model not invertible." );
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
		double a00, a01, a11;
		double b00, b01, b10, b11;
		a00 = a01 = a11 = b00 = b01 = b10 = b11 = 0;
		for ( final PointMatch m : matches )
		{
			final float[] p = m.getP1().getL();
			final float[] q = m.getP2().getW();
			final float w = m.getWeight();
			
			final float px = p[ 0 ] - pcx, py = p[ 1 ] - pcy;
			final float qx = q[ 0 ] - qcx, qy = q[ 1 ] - qcy;
			a00 += w * px * px;
			a01 += w * px * py;
			a11 += w * py * py;
			b00 += w * px * qx;
			b01 += w * px * qy;
			b10 += w * py * qx;
			b11 += w * py * qy;
		}
		
		// invert M
		final float det = ( float )( a00 * a11 - a01 * a01 );
		
		if ( det == 0 )
			throw new IllDefinedDataPointsException();
		
		m00 = ( float )( a11 * b00 - a01 * b10 ) / det;
		m01 = ( float )( a00 * b10 - a01 * b00 ) / det;
		m10 = ( float )( a11 * b01 - a01 * b11 ) / det;
		m11 = ( float )( a00 * b11 - a01 * b01 ) / det;
		
		m02 = qcx - m00 * pcx - m01 * pcy;
		m12 = qcy - m10 * pcx - m11 * pcy;
		
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
		m00 = m.m00;
		m01 = m.m01;
		m10 = m.m10;
		m11 = m.m11;

		m02 = m.m02;
		m12 = m.m12;

		cost = m.getCost();
	}

	@Override
	final public AffineModel2D clone()
	{
		AffineModel2D m = new AffineModel2D();
		m.m00 = m00;
		m.m01 = m01;
		m.m10 = m10;
		m.m11 = m11;

		m.m02 = m02;
		m.m12 = m12;

		m.cost = cost;
		return m;
	}
	
	final private void invert()
	{
		final double det = m00 * m11 - m01 * m10;
		if ( det == 0 )
		{
			isInvertible = false;
			return;
		}
		
		isInvertible = true;
		
		i00 = m11 / det;
		i01 = -m01 / det;
		i02 = ( m01 * m12 - m02 * m11 ) / det;
		
		i10 = -m10 / det;
		i11 = m00 / det;
		i12 = ( m02 * m10 - m00 * m12 ) / det;		
	}
	
	@Override
	final public void preConcatenate( final AffineModel2D model )
	{
		final double a00 = model.m00 * m00 + model.m01 * m10;
		final double a01 = model.m00 * m01 + model.m01 * m11;
		final double a02 = model.m00 * m02 + model.m01 * m12 + model.m02;
		
		final double a10 = model.m10 * m10 + model.m11 * m10;
		final double a11 = model.m10 * m11 + model.m11 * m11;
		final double a12 = model.m10 * m12 + model.m11 * m12 + model.m12;
		
		m00 = a00;
		m01 = a01;
		m02 = a02;
		
		m10 = a10;
		m11 = a11;
		m12 = a12;
		
		invert();
	}
	
	@Override
	final public void concatenate( final AffineModel2D model )
	{
		final double a00 = m00 * model.m00 + m01 * model.m10;
		final double a01 = m00 * model.m01 + m01 * model.m11;
		final double a02 = m00 * model.m02 + m01 * model.m12 + m02;
		
		final double a10 = m10 * model.m10 + m11 * model.m10;
		final double a11 = m10 * model.m11 + m11 * model.m11;
		final double a12 = m10 * model.m12 + m11 * model.m12 + m12;
		
		m00 = a00;
		m01 = a01;
		m02 = a02;
		
		m10 = a10;
		m11 = a11;
		m12 = a12;
		
		invert();
	}
}
