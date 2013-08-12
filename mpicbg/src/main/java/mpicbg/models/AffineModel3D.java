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
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de> and Johannes Schindelin
 * @version 0.1b
 * 
 */
public class AffineModel3D extends AbstractAffineModel3D< AffineModel3D > implements InvertibleBoundable
{
	private static final long serialVersionUID = -5157831406137373994L;

	static final protected int MIN_NUM_MATCHES = 4;
	
	protected float
		m00 = 1.0f, m01 = 0.0f, m02 = 0.0f, m03 = 0.0f, 
		m10 = 0.0f, m11 = 1.0f, m12 = 0.0f, m13 = 0.0f, 
		m20 = 0.0f, m21 = 0.0f, m22 = 1.0f, m23 = 0.0f;
	
	protected float
		i00 = 1.0f, i01 = 0.0f, i02 = 0.0f, i03 = 0.0f, 
		i10 = 0.0f, i11 = 1.0f, i12 = 0.0f, i13 = 0.0f, 
		i20 = 0.0f, i21 = 0.0f, i22 = 1.0f, i23 = 0.0f;
	
	@Override
	public float[] getMatrix( final float[] m )
	{
		final float[] a;
		if ( m == null || m.length != 12 )
			a = new float[ 12 ];
		else
			a = m;
		
		a[ 0 ] = m00;
		a[ 1 ] = m01;
		a[ 2 ] = m02;
		a[ 3 ] = m03;
		
		a[ 4 ] = m10;
		a[ 5 ] = m11;
		a[ 6 ] = m12;
		a[ 7 ] = m13;
		
		a[ 8 ] = m20;
		a[ 9 ] = m21;
		a[ 10 ] = m22;
		a[ 11 ] = m23;
		
		return a;
	}

	protected boolean isInvertible = true;
	
	@Override
	final public int getMinNumMatches(){ return MIN_NUM_MATCHES; }
	
	@Override
	final public float[] apply( final float[] l )
	{
		final float[] transformed = l.clone();
		applyInPlace( transformed );
		return transformed;
	}
	
	@Override
	final public void applyInPlace( final float[] l )
	{
		assert l.length >= 3 : "3d affine transformations can be applied to 3d points only.";
		
		final float l0 = l[ 0 ];
		final float l1 = l[ 1 ];
		l[ 0 ] = l0 * m00 + l1 * m01 + l[ 2 ] * m02 + m03;
		l[ 1 ] = l0 * m10 + l1 * m11 + l[ 2 ] * m12 + m13;
		l[ 2 ] = l0 * m20 + l1 * m21 + l[ 2 ] * m22 + m23;
	}
	
	@Override
	final public float[] applyInverse( final float[] l ) throws NoninvertibleModelException
	{
		final float[] transformed = l.clone();
		applyInverseInPlace( transformed );
		return transformed;
	}


	@Override
	final public void applyInverseInPlace( final float[] l ) throws NoninvertibleModelException
	{
		assert l.length >= 3 : "3d affine transformations can be applied to 3d points only.";
		
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
	 * \citet{SchaeferAl06}.
	 */
	@Override
	final public void fit(
			final float[][] p,
			final float[][] q,
			final float[] w )
		throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		assert
		p.length >= 3 &&
		q.length >= 3 : "3d affine transformations can be applied to 3d points only.";
	
		assert
			p[ 0 ].length == p[ 1 ].length &&
			p[ 0 ].length == q[ 0 ].length &&
			p[ 0 ].length == q[ 1 ].length &&
			p[ 0 ].length == w.length : "Array lengths do not match.";
			
		final int l = p[ 0 ].length;
		
		if ( l < MIN_NUM_MATCHES )
			throw new NotEnoughDataPointsException( l + " data points are not enough to estimate a 2d affine model, at least " + MIN_NUM_MATCHES + " data points required." );
		
		float pcx = 0, pcy = 0, pcz = 0;
		float qcx = 0, qcy = 0, qcz = 0;
		
		double ws = 0.0;
		
		for ( int i = 0; i < l; ++i )
		{
			final float[] pX = p[ 0 ];
			final float[] pY = p[ 1 ];
			final float[] pZ = p[ 2 ];
			final float[] qX = q[ 0 ];
			final float[] qY = q[ 1 ];
			final float[] qZ = q[ 2 ];

			final float ww = w[ i ];
			ws += ww;
			
			pcx += ww * pX[ i ];
			pcy += ww * pY[ i ];
			pcz += ww * pZ[ i ];
			qcx += ww * qX[ i ];
			qcy += ww * qY[ i ];
			qcz += ww * qZ[ i ];
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
		for ( int i = 0; i < l; ++i )
		{
			final float[] pX = p[ 0 ];
			final float[] pY = p[ 1 ];
			final float[] pZ = p[ 2 ];
			final float[] qX = q[ 0 ];
			final float[] qY = q[ 1 ];
			final float[] qZ = q[ 2 ];

			final float ww = w[ i ];
			
			final float px = pX[ i ] - pcx, py = pY[ i ] - pcy, pz = pZ[ i ] - pcz;
			final float qx = qX[ i ] - qcx, qy = qY[ i ] - qcy, qz = qZ[ i ] - qcz;
			a00 += ww * px * px;
			a01 += ww * px * py;
			a02 += ww * px * pz;
			a11 += ww * py * py;
			a12 += ww * py * pz;
			a22 += ww * pz * pz;
			
			b00 += ww * px * qx;
			b01 += ww * px * qy;
			b02 += ww * px * qz;	
			b10 += ww * py * qx;
			b11 += ww * py * qy;
			b12 += ww * py * qz;
			b20 += ww * pz * qx;
			b21 += ww * pz * qy;
			b22 += ww * pz * qz;
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
	 * Closed form weighted least squares solution as described by
	 * \citet{SchaeferAl06}.
	 */
	@Override
	final public < P extends PointMatch >void fit( final Collection< P > matches )
		throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		if ( matches.size() < MIN_NUM_MATCHES )
			throw new NotEnoughDataPointsException( matches.size() + " data points are not enough to estimate a 2d affine model, at least " + MIN_NUM_MATCHES + " data points required." );
		
		float pcx = 0, pcy = 0, pcz = 0;
		float qcx = 0, qcy = 0, qcz = 0;
		
		double ws = 0.0;
		
		for ( final P m : matches )
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
		for ( final P m : matches )
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

//	/**
//	 * TODO Not yet implemented ...
//	 */
//	@Override
//	final public void shake( final float amount )
//	{
//		// TODO If you ever need it, please implement it...
//	}

	@Override
	final public void set( final AffineModel3D m )
	{
		m00 = m.m00;
		m10 = m.m10;
		m20 = m.m20;
		m01 = m.m01;
		m11 = m.m11;
		m21 = m.m21;
		m02 = m.m02;
		m12 = m.m12;
		m22 = m.m22;
		m03 = m.m03;
		m13 = m.m13;
		m23 = m.m23;
		
		cost = m.cost;
		
		invert();
	}

	@Override
	public AffineModel3D copy()
	{
		final AffineModel3D m = new AffineModel3D();
		m.m00 = m00;
		m.m10 = m10;
		m.m20 = m20;
		m.m01 = m01;
		m.m11 = m11;
		m.m21 = m21;
		m.m02 = m02;
		m.m12 = m12;
		m.m22 = m22;
		m.m03 = m03;
		m.m13 = m13;
		m.m23 = m23;
		
		m.cost = cost;
		
		m.invert();

		return m;
	}
	
	protected void invert()
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
	
	@Override
	final public void preConcatenate( final AffineModel3D model )
	{
		final float a00 = model.m00 * m00 + model.m01 * m10 + model.m02 * m20;
		final float a01 = model.m00 * m01 + model.m01 * m11 + model.m02 * m21;
		final float a02 = model.m00 * m02 + model.m01 * m12 + model.m02 * m22;
		final float a03 = model.m00 * m03 + model.m01 * m13 + model.m02 * m23 + model.m03;
		
		final float a10 = model.m10 * m00 + model.m11 * m10 + model.m12 * m20;
		final float a11 = model.m10 * m01 + model.m11 * m11 + model.m12 * m21;
		final float a12 = model.m10 * m02 + model.m11 * m12 + model.m12 * m22;
		final float a13 = model.m10 * m03 + model.m11 * m13 + model.m12 * m23 + model.m13;
		
		final float a20 = model.m20 * m00 + model.m21 * m10 + model.m22 * m20;
		final float a21 = model.m20 * m01 + model.m21 * m11 + model.m22 * m21;
		final float a22 = model.m20 * m02 + model.m21 * m12 + model.m22 * m22;
		final float a23 = model.m20 * m03 + model.m21 * m13 + model.m22 * m23 + model.m23;
		
		m00 = a00;
		m01 = a01;
		m02 = a02;
		m03 = a03;
		
		m10 = a10;
		m11 = a11;
		m12 = a12;
		m13 = a13;
		
		m20 = a20;
		m21 = a21;
		m22 = a22;
		m23 = a23;
		
		invert();
	}
	
	@Override
	final public void concatenate( final AffineModel3D model )
	{
		final float a00 = m00 * model.m00 + m01 * model.m10 + m02 * model.m20;
		final float a01 = m00 * model.m01 + m01 * model.m11 + m02 * model.m21;
		final float a02 = m00 * model.m02 + m01 * model.m12 + m02 * model.m22;
		final float a03 = m00 * model.m03 + m01 * model.m13 + m02 * model.m23 + m03;
		
		final float a10 = m10 * model.m00 + m11 * model.m10 + m12 * model.m20;
		final float a11 = m10 * model.m01 + m11 * model.m11 + m12 * model.m21;
		final float a12 = m10 * model.m02 + m11 * model.m12 + m12 * model.m22;
		final float a13 = m10 * model.m03 + m11 * model.m13 + m12 * model.m23 + m13;
		
		final float a20 = m20 * model.m00 + m21 * model.m10 + m22 * model.m20;
		final float a21 = m20 * model.m01 + m21 * model.m11 + m22 * model.m21;
		final float a22 = m20 * model.m02 + m21 * model.m12 + m22 * model.m22;
		final float a23 = m20 * model.m03 + m21 * model.m13 + m22 * model.m23 + m23;
		
		m00 = a00;
		m01 = a01;
		m02 = a02;
		m03 = a03;
		
		m10 = a10;
		m11 = a11;
		m12 = a12;
		m13 = a13;
		
		m20 = a20;
		m21 = a21;
		m22 = a22;
		m23 = a23;
		
		invert();
	}
	
	final public void concatenate( final TranslationModel3D model )
	{
		final float[] t = model.getTranslation();
		
		m03 = m00 * t[ 0 ] + m01 * t[ 1 ] + m02 * t[ 2 ] + m03;
		m13 = m10 * t[ 0 ] + m11 * t[ 1 ] + m12 * t[ 2 ] + m13;
		m23 = m20 * t[ 0 ] + m21 * t[ 1 ] + m22 * t[ 2 ] + m23;
		
		invert();
	}
	
	final public void preConcatenate( final TranslationModel3D model )
	{
		final float[] t = model.getTranslation();
		
		m03 += t[ 0 ];
		m13 += t[ 1 ];
		m23 += t[ 2 ];
		
		invert();
	}
	
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
	
	@Override
	final public String toString()
	{
		return
			"3d-affine: (" +
			m00 + ", " + m01 + ", " + m02 + ", " + m03 + ", " +
			m10 + ", " + m11 + ", " + m12 + ", " + m13 + ", " +
			m20 + ", " + m21 + ", " + m22 + ", " + m23 + ")";
	}
	
	/**
	 * TODO Not yet tested
	 */
	@Override
	public AffineModel3D createInverse()
	{
		final AffineModel3D ict = new AffineModel3D();
		
		ict.m00 = i00;
		ict.m10 = i10;
		ict.m20 = i20;
		ict.m01 = i01;
		ict.m11 = i11;
		ict.m21 = i21;
		ict.m02 = i02;
		ict.m12 = i12;
		ict.m22 = i22;
		ict.m03 = i03;
		ict.m13 = i13;
		ict.m23 = i23;
		
		ict.i00 = m00;
		ict.i10 = m10;
		ict.i20 = m20;
		ict.i01 = m01;
		ict.i11 = m11;
		ict.i21 = m21;
		ict.i02 = m02;
		ict.i12 = m12;
		ict.i22 = m22;
		ict.i03 = m03;
		ict.i13 = m13;
		ict.i23 = m23;
		
		ict.cost = cost;
		
		ict.isInvertible = isInvertible;
		
		return ict;
	}
	
	/**
	 * Rotate
	 * 
	 * @param axis 0=x, 1=y, 2=z
	 * @param d angle in radians
	 * 
	 * TODO Don't be lazy and do it directly on the values instead of creating another transform
	 */
	public void rotate( final int axis, final float d )
	{
		final float dcos = ( float )Math.cos( d ); 
		final float dsin = ( float )Math.sin( d );
		final AffineModel3D dR = new AffineModel3D();
		
		switch ( axis )
		{
		case 0:
			dR.set(
					1.0f, 0.0f, 0.0f, 0.0f,
					0.0f, dcos, -dsin, 0.0f,
					0.0f, dsin, dcos, 0.0f );
			break;
		case 1:
			dR.set(
					dcos, 0.0f, dsin, 0.0f,
					0.0f, 1.0f, 0.0f, 0.0f,
					-dsin, 0.0f, dcos, 0.0f );
			break;
		default:
			dR.set(
					dcos, -dsin, 0.0f, 0.0f,
					dsin, dcos, 0.0f, 0.0f,
					0.0f, 0.0f, 1.0f, 0.0f );
			break;
		}
		
		preConcatenate( dR );
	}

	@Override
	public void toArray( final float[] data )
	{
		data[ 0 ] = m00;
		data[ 1 ] = m10;
		data[ 2 ] = m20;
		data[ 3 ] = m01;
		data[ 4 ] = m11;
		data[ 5 ] = m21;
		data[ 6 ] = m02;
		data[ 7 ] = m12;
		data[ 8 ] = m22;
		data[ 9 ] = m03;
		data[ 10 ] = m13;
		data[ 11 ] = m23;
	}

	@Override
	public void toArray( final double[] data )
	{
		data[ 0 ] = m00;
		data[ 1 ] = m10;
		data[ 2 ] = m20;
		data[ 3 ] = m01;
		data[ 4 ] = m11;
		data[ 5 ] = m21;
		data[ 6 ] = m02;
		data[ 7 ] = m12;
		data[ 8 ] = m22;
		data[ 9 ] = m03;
		data[ 10 ] = m13;
		data[ 11 ] = m23;
	}

	@Override
	public void toMatrix( final float[][] data )
	{
		data[ 0 ][ 0 ] = m00;
		data[ 0 ][ 1 ] = m01;
		data[ 0 ][ 2 ] = m02;
		data[ 0 ][ 3 ] = m03;
		data[ 1 ][ 0 ] = m10;
		data[ 1 ][ 1 ] = m11;
		data[ 1 ][ 2 ] = m12;
		data[ 1 ][ 3 ] = m13;
		data[ 2 ][ 0 ] = m20;
		data[ 2 ][ 1 ] = m21;
		data[ 2 ][ 2 ] = m22;
		data[ 2 ][ 3 ] = m23;
	}

	@Override
	public void toMatrix( final double[][] data )
	{
		data[ 0 ][ 0 ] = m00;
		data[ 0 ][ 1 ] = m01;
		data[ 0 ][ 2 ] = m02;
		data[ 0 ][ 3 ] = m03;
		data[ 1 ][ 0 ] = m10;
		data[ 1 ][ 1 ] = m11;
		data[ 1 ][ 2 ] = m12;
		data[ 1 ][ 3 ] = m13;
		data[ 2 ][ 0 ] = m20;
		data[ 2 ][ 1 ] = m21;
		data[ 2 ][ 2 ] = m22;
		data[ 2 ][ 3 ] = m23;
	}
}
