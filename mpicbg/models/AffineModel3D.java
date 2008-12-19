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
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de> and Stephan Preibisch and Johannes Schindelin
 *
 */
package mpicbg.models;

import java.util.Collection;

import mpicbg.util.Matrix3x3;

/**
 * 3d-affine transformation models to be applied to points in 3d-space.
 * This model includes the closed form weighted least squares solution as
 * described by \citet{SchaeferAl06} transferred to 3d  
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
 * @version 0.1b
 * 
 */
public class AffineModel3D extends InvertibleModel< AffineModel3D >
{
	static final protected int MIN_NUM_MATCHES = 4;
	
	private float
		m00 = 1.0f, m01 = 0.0f, m02 = 0.0f, m03 = 0.0f, 
		m10 = 0.0f, m11 = 1.0f, m12 = 0.0f, m13 = 0.0f, 
		m20 = 0.0f, m21 = 0.0f, m22 = 1.0f, m23 = 0.0f;
	
	private float
		i00 = 1.0f, i01 = 0.0f, i02 = 0.0f, i03 = 0.0f, 
		i10 = 0.0f, i11 = 1.0f, i12 = 0.0f, i13 = 0.0f, 
		i20 = 0.0f, i21 = 0.0f, i22 = 1.0f, i23 = 0.0f;

	private boolean isInvertible = true;
	
	@Override
	final public int getMinNumMatches(){ return MIN_NUM_MATCHES; }
	
	//@Override
	final public float[] apply( final float[] l )
	{
		final float[] transformed = l.clone();
		applyInPlace( transformed );
		return transformed;
	}
	
	//@Override
	final public void applyInPlace( final float[] l )
	{
		assert l.length == 3 : "3d affine transformations can be applied to 3d points only.";
		
		final float l0 = l[ 0 ];
		final float l1 = l[ 1 ];
		l[ 0 ] = l0 * m00 + l1 * m01 + l[ 2 ] * m02 + m03;
		l[ 1 ] = l0 * m10 + l1 * m11 + l[ 2 ] * m12 + m13;
		l[ 2 ] = l0 * m20 + l1 * m21 + l[ 2 ] * m22 + m23;
	}
	
	//@Override
	final public float[] applyInverse( final float[] l ) throws NoninvertibleModelException
	{
		final float[] transformed = l.clone();
		applyInverseInPlace( transformed );
		return transformed;
	}


	//@Override
	final public void applyInverseInPlace( final float[] l ) throws NoninvertibleModelException
	{
		assert l.length == 3 : "3d affine transformations can be applied to 3d points only.";
		
		if ( isInvertible )
		{
			final float l0 = l[ 0 ];
			final float l1 = l[ 1 ];
			l[ 0 ] = l0 * i00 + l1 * i01 + l[ 2 ] * i02 + i03;
			l[ 1 ] = l0 * i10 + l1 * i11 + l[ 2 ] * i12 + i13;
			l[ 2 ] = l0 * i20 + l1 * i21 + l[ 2 ] * i22 + i23;
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
		
		float pcx = 0, pcy = 0, pcz = 0;
		float qcx = 0, qcy = 0, qcz = 0;
		
		double ws = 0.0;
		
		for ( final PointMatch m : matches )
		{
			final float[] p = m.getP1().getL(); 
			final float[] q = m.getP2().getW(); 
			
			final float w = m.getWeight();
			ws += w;
			
			pcx += w * p[ 0 ];
			pcy += w * p[ 1 ];
			pcz += w * p[ 2 ];
			qcx += w * q[ 0 ];
			qcy += w * q[ 1 ];
			qcz += w * q[ 2 ];
		}
		pcx /= ws;
		pcy /= ws;
		pcz /= ws;
		qcx /= ws;
		qcy /= ws;
		qcz /= ws;
		
		float
			a00, a01, a02,
			     a11, a12,
			          a22;
		float
			b00, b01, b02,
			b10, b11, b12,
			b20, b21, b22;
		
		a00 = a01 = a02 = a11 = a12 = a22 = b00 = b01 = b02 = b10 = b11 = b12 = b20 = b21 = b22 = 0;
		for ( final PointMatch m : matches )
		{
			final float[] p = m.getP1().getL();
			final float[] q = m.getP2().getW();
			final float w = m.getWeight();
			
			final float px = p[ 0 ] - pcx, py = p[ 1 ] - pcy, pz = p[ 2 ] - pcz;
			final float qx = q[ 0 ] - qcx, qy = q[ 1 ] - qcy, qz = q[ 2 ] - qcz;
			a00 += w * px * px;
			a01 += w * px * py;
			a02 += w * px * pz;
			a11 += w * py * py;
			a12 += w * py * pz;
			a22 += w * pz * pz;
			
			b00 += w * px * qx;
			b01 += w * px * qy;
			b02 += w * px * qz;	
			b10 += w * py * qx;
			b11 += w * py * qy;
			b12 += w * py * qz;
			b20 += w * pz * qx;
			b21 += w * pz * qy;
			b22 += w * pz * qz;
		}
		
		final float det =
			a00 * a11 * a22 +
			a01 * a12 * a02 +
			a02 * a01 * a12 -
			a02 * a11 * a02 -
			a12 * a12 * a00 -
			a22 * a01 * a01;
		
		if ( det == 0 )
			throw new IllDefinedDataPointsException();
		
		final float idet = 1f / det;
		
		final float ai00 = ( a11 * a22 - a12 * a12 ) * idet;
		final float ai01 = ( a02 * a12 - a01 * a22 ) * idet;
		final float ai02 = ( a01 * a12 - a02 * a11 ) * idet;
		final float ai11 = ( a00 * a22 - a02 * a02 ) * idet;
		final float ai12 = ( a02 * a01 - a00 * a12 ) * idet;
		final float ai22 = ( a00 * a11 - a01 * a01 ) * idet;
		
		m00 = ai00 * b00 + ai01 * b10 + ai02 * b20;
		m01 = ai01 * b00 + ai11 * b10 + ai12 * b20;
		m02 = ai02 * b00 + ai12 * b10 + ai22 * b20;
		
		m10 = ai00 * b01 + ai01 * b11 + ai02 * b21;
		m11 = ai01 * b01 + ai11 * b11 + ai12 * b21;
		m12 = ai02 * b01 + ai12 * b11 + ai22 * b21;
		
		m20 = ai00 * b02 + ai01 * b12 + ai02 * b22;
		m21 = ai01 * b02 + ai11 * b12 + ai12 * b22;
		m22 = ai02 * b02 + ai12 * b12 + ai22 * b22;
		
		m03 = qcx - m00 * pcx - m01 * pcy - m02 * pcz;
		m13 = qcy - m10 * pcx - m11 * pcy - m12 * pcz;
		m23 = qcz - m20 * pcx - m21 * pcy - m22 * pcz;
		
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
	final public void set( final AffineModel3D m )
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
	final public AffineModel3D clone()
	{
		AffineModel3D m = new AffineModel3D();
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
		final float det = Matrix3x3.det( m00, m01, m02, m10, m11, m12, m20, m21, m22 );
		if ( det == 0 )
		{
			isInvertible = false;
			return;
		}
		
		isInvertible = true;
		
		final float idet = 1f / det;
		
		i00 = ( m11 * m22 - m12 * m21 ) * idet;
		i01 = ( m02 * m21 - m01 * m22 ) * idet;
		i02 = ( m01 * m12 - m02 * m11 ) * idet;
		i10 = ( m12 * m20 - m10 * m22 ) * idet;
		i11 = ( m00 * m22 - m02 * m20 ) * idet;
		i12 = ( m02 * m10 - m00 * m12 ) * idet;
		i20 = ( m10 * m21 - m11 * m20 ) * idet;
		i21 = ( m01 * m20 - m00 * m21 ) * idet;
		i22 = ( m00 * m11 - m01 * m10 ) * idet;
		
		i03 = -i00 * m03 - i01 * m13 - i02 * m23;
		i13 = -i10 * m03 - i11 * m13 - i12 * m23;
		i23 = -i20 * m03 - i21 * m13 - i22 * m23;
	}
	
//	@Override
//	final public void preConcatenate( final AffineModel3D model )
//	{
//		final float a00 = model.m00 * m00 + model.m01 * m10;
//		final float a01 = model.m00 * m01 + model.m01 * m11;
//		final float a02 = model.m00 * m02 + model.m01 * m12 + model.m02;
//		
//		final float a10 = model.m10 * m00 + model.m11 * m10;
//		final float a11 = model.m10 * m01 + model.m11 * m11;
//		final float a12 = model.m10 * m02 + model.m11 * m12 + model.m12;
//		
//		m00 = a00;
//		m01 = a01;
//		m02 = a02;
//		
//		m10 = a10;
//		m11 = a11;
//		m12 = a12;
//		
//		invert();
//	}
//	
//	@Override
//	final public void concatenate( final AffineModel3D model )
//	{
//		final float a00 = m00 * model.m00 + m01 * model.m10;
//		final float a01 = m00 * model.m01 + m01 * model.m11;
//		final float a02 = m00 * model.m02 + m01 * model.m12 + m02;
//		
//		final float a10 = m10 * model.m00 + m11 * model.m10;
//		final float a11 = m10 * model.m01 + m11 * model.m11;
//		final float a12 = m10 * model.m02 + m11 * model.m12 + m12;
//		
//		m00 = a00;
//		m01 = a01;
//		m02 = a02;
//		
//		m10 = a10;
//		m11 = a11;
//		m12 = a12;
//		
//		invert();
//	}
	
	/**
	 * Initialize the model such that the respective affine transform is:
	 * 
	 * <pre>
	 * m00 m01 m02 m03
	 * m10 m11 m12 m13
	 * m20 m21 m22 m23
	 * 0   0   0   1
	 * </pre>
	 * 
	 * @param m00
	 * @param m01
	 * @param m02
	 * @param m03
	 * 
	 * @param m10
	 * @param m11
	 * @param m12
	 * @param m13
	 * 
	 * @param m20
	 * @param m21
	 * @param m22
	 * @param m23
	 */
	final public void set(
			final float m00, final float m01, final float m02, final float m03,
			final float m10, final float m11, final float m12, final float m13,
			final float m20, final float m21, final float m22, final float m23 )
	{
		this.m00 = m00;
		this.m01 = m01;
		this.m02 = m02;
		this.m03 = m03;
		
		this.m10 = m10;
		this.m11 = m11;
		this.m12 = m12;
		this.m13 = m13;
		
		this.m20 = m20;
		this.m21 = m21;
		this.m22 = m22;
		this.m23 = m23;

		invert();
	}
	
	final public String toString()
	{
		return
			"3d-affine: (" +
			m00 + ", " + m01 + ", " + m02 + ", " + m03 + ", " +
			m10 + ", " + m11 + ", " + m12 + ", " + m13 + ", " +
			m20 + ", " + m21 + ", " + m22 + ", " + m23 + ")";
	}
}
