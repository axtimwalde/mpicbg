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

/**
 * 2d-affine transformation models to be applied to points in 2d-space.
 * This model includes the closed form weighted least squares solution as
 * described by \citet{SchaeferAl06} and implemented by Johannes Schindelin.  
 * 
 * BibTeX:
 * <pre>
 * &#64;article{SchaeferAl06,
 *   author    = {Scott Schaefer and Travis McPhail and Joe Warren},
 *   title     = {Image deformation using moving least squares},
 *   journal   = {ACM Transactions on Graphics},
 *   volume    = {25},
 *   number    = {3},
 *   year      = {2006},
 *   pages     = {533--540},
 *   publisher = {ACM},
 *   address   = {New York, NY, USA},
 *   url       = {http://faculty.cs.tamu.edu/schaefer/research/mls.pdf},
 * }
 * </pre>
 * 
 * @version 0.4b
 * 
 */
public class AffineModel2D extends AbstractAffineModel2D< AffineModel2D >
{
	static final protected int MIN_NUM_MATCHES = 3;
	
	private float m00 = 1.0f, m10 = 0.0f, m01 = 0.0f, m11 = 1.0f, m02 = 0.0f, m12 = 0.0f;
	private float i00 = 1.0f, i10 = 0.0f, i01 = 0.0f, i11 = 1.0f, i02 = 0.0f, i12 = 0.0f;
	
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
		
		final float[] transformed = l.clone();
		applyInPlace( transformed );
		return transformed;
	}
	
	//@Override
	final public void applyInPlace( final float[] l )
	{
		assert l.length == 2 : "2d affine transformations can be applied to 2d points only.";
		
		final float l0 = l[ 0 ];
		l[ 0 ] = l0 * m00 + l[ 1 ] * m01 + m02;
		l[ 1 ] = l0 * m10 + l[ 1 ] * m11 + m12;
	}
	
	//@Override
	final public float[] applyInverse( final float[] l ) throws NoninvertibleModelException
	{
		assert l.length == 2 : "2d affine transformations can be applied to 2d points only.";
		
		final float[] transformed = l.clone();
		applyInverseInPlace( transformed );
		return transformed;
	}


	//@Override
	final public void applyInverseInPlace( final float[] l ) throws NoninvertibleModelException
	{
		assert l.length == 2 : "2d affine transformations can be applied to 2d points only.";
		
		if ( isInvertible )
		{
			final float l0 = l[ 0 ];
			l[ 0 ] = l0 * i00 + l[ 1 ] * i01 + i02;
			l[ 1 ] = l0 * i10 + l[ 1 ] * i11 + i12;
		}
		else
			throw new NoninvertibleModelException( "Model not invertible." );
	}
	
	/**
	 * Closed form weighted least squares solution as described by
	 * \citet{SchaeferAl06} and implemented by Johannes Schindelin.
	 */
	@Override
	final public void fit( final Collection< PointMatch > matches )
		throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		if ( matches.size() < MIN_NUM_MATCHES )
			throw new NotEnoughDataPointsException( matches.size() + " data points are not enough to estimate a 2d affine model, at least " + MIN_NUM_MATCHES + " data points required." );
		
		float pcx = 0, pcy = 0;
		float qcx = 0, qcy = 0;
		
		double ws = 0.0;
		
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
		
		float a00, a01, a11;
		float b00, b01, b10, b11;
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
		
		final float det = a00 * a11 - a01 * a01;
		
		if ( det == 0 )
			throw new IllDefinedDataPointsException();
		
		m00 = ( a11 * b00 - a01 * b10 ) / det;
		m01 = ( a00 * b10 - a01 * b00 ) / det;
		m10 = ( a11 * b01 - a01 * b11 ) / det;
		m11 = ( a00 * b11 - a01 * b01 ) / det;
		
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
		
		invert();

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
		
		m.invert();

		return m;
	}
	
	final private void invert()
	{
		final float det = m00 * m11 - m01 * m10;
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
		final float a00 = model.m00 * m00 + model.m01 * m10;
		final float a01 = model.m00 * m01 + model.m01 * m11;
		final float a02 = model.m00 * m02 + model.m01 * m12 + model.m02;
		
		final float a10 = model.m10 * m00 + model.m11 * m10;
		final float a11 = model.m10 * m01 + model.m11 * m11;
		final float a12 = model.m10 * m02 + model.m11 * m12 + model.m12;
		
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
		final float a00 = m00 * model.m00 + m01 * model.m10;
		final float a01 = m00 * model.m01 + m01 * model.m11;
		final float a02 = m00 * model.m02 + m01 * model.m12 + m02;
		
		final float a10 = m10 * model.m00 + m11 * model.m10;
		final float a11 = m10 * model.m01 + m11 * model.m11;
		final float a12 = m10 * model.m02 + m11 * model.m12 + m12;
		
		m00 = a00;
		m01 = a01;
		m02 = a02;
		
		m10 = a10;
		m11 = a11;
		m12 = a12;
		
		invert();
	}
	
	/**
	 * Initialize the model such that the respective affine transform is:
	 * 
	 * m00 m01 m02
	 * m10 m11 m12
	 * 0   0   1
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
		this.m00 = m00;
		this.m10 = m10;
		
		this.m01 = m01;
		this.m11 = m11;
		
		this.m02 = m02;
		this.m12 = m12;
		
		invert();
	}
	
	/**
	 * Initialize the model with the parameters of an {@link AffineTransform}.
	 * 
	 * @param a
	 */
	final public void set( final AffineTransform a )
	{
		m00 = ( float )a.getScaleX();
		m10 = ( float )a.getShearY();
		
		m01 = ( float )a.getShearX();
		m11 = ( float )a.getScaleY();
		
		m02 = ( float )a.getTranslateX();
		m12 = ( float )a.getTranslateY();
		
		invert();
	}
}
