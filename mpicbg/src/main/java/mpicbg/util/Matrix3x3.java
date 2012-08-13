package mpicbg.util;

import mpicbg.models.NoninvertibleModelException;

/**
 * Basic operations on 3x3 float matrices.
 * 
 * <pre>
 * m00 m01 m02
 * m10 m11 m12
 * m20 m21 m22
 * </pre>
 * 
 * The class provides most of the methods as static variants for matrices
 * passed as float[].  They are always meant to be in a row after row sequence:
 * 
 * <pre>
 * m00, m01, m02, m10, m11, m12, m20, m21, m22
 * </pre>
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.3b
 */
final public class Matrix3x3
{
	public float
		m00 = 1, m01 = 0, m02 = 0,
		m10 = 0, m11 = 1, m12 = 0,
		m20 = 0, m21 = 0, m22 = 1;
	
	/**
	 * Initialize and identity matrix.
	 */
	public Matrix3x3(){}
	
	/**
	 * Initialize a matrix.
	 * 
	 * @param m00
	 * @param m01
	 * @param m02
	 * @param m10
	 * @param m11
	 * @param m12
	 * @param m20
	 * @param m21
	 * @param m22
	 */
	public Matrix3x3(
			final float m00, final float m01, final float m02,
			final float m10, final float m11, final float m12,
			final float m20, final float m21, final float m22 )
	{
		this.m00 = m00;
		this.m01 = m01;
		this.m02 = m02;
		
		this.m10 = m10;
		this.m11 = m11;
		this.m12 = m12;
		
		this.m20 = m20;
		this.m21 = m21;
		this.m22 = m22;
	}
	
	
	/**
	 * Initialize a matrix with a float[].
	 * 
	 * Note that the float[] elements do not reflect the content of the matrix
	 * when performing operations.  They are just used to initialize it.  For
	 * performing operations on the float[] directly, use the static methods
	 * of {@link Matrix3x3}.
	 *  
	 * @param m
	 */
	public Matrix3x3( final float[] m )
	{
		this.m00 = m[ 0 ];
		this.m01 = m[ 1 ];
		this.m02 = m[ 2 ];
		
		this.m10 = m[ 3 ];
		this.m11 = m[ 4 ];
		this.m12 = m[ 5 ];
		
		this.m20 = m[ 6 ];
		this.m21 = m[ 7 ];
		this.m22 = m[ 8 ];
	}
	
	
	/**
	 * Calculate the determinant.
	 * 
	 * @return determinant
	 */
	final public float det()
	{
		return
			m00 * m11 * m22 +
			m10 * m21 * m02 +
			m20 * m01 * m12 -
			m02 * m11 * m20 -
			m12 * m21 * m00 -
			m22 * m01 * m10;
	}
	
	
	/**
	 * Calculate the determinant of a matrix given as a float[] (row after row).
	 * 
	 * @param a matrix given row by row
	 * 
	 * @return determinant
	 */
	final static public float det( final float[] a )
	{
		assert a.length == 9 : "Matrix3x3 supports 3x3 float[][] only.";
		
		return
			a[ 0 ] * a[ 4 ] * a[ 8 ] +
			a[ 3 ] * a[ 7 ] * a[ 2 ] +
			a[ 6 ] * a[ 1 ] * a[ 5 ] -
			a[ 2 ] * a[ 4 ] * a[ 6 ] -
			a[ 5 ] * a[ 7 ] * a[ 0 ] -
			a[ 8 ] * a[ 1 ] * a[ 3 ];
	}
	
	
	/**
	 * Calculate the determinant of a matrix given by values.
	 * 
	 * @param m00
	 * @param m01
	 * @param m02
	 * @param m10
	 * @param m11
	 * @param m12
	 * @param m20
	 * @param m21
	 * @param m22
	 * 
	 * @return
	 */
	final static public float det(
			final float m00, final float m01, final float m02,
			final float m10, final float m11, final float m12,
			final float m20, final float m21, final float m22 )
	{
		return
			m00 * m11 * m22 +
			m10 * m21 * m02 +
			m20 * m01 * m12 -
			m02 * m11 * m20 -
			m12 * m21 * m00 -
			m22 * m01 * m10;
	}
	
	
	final public void invert() throws NoninvertibleModelException
	{
		final float det = det();
		if ( det == 0 ) throw new NoninvertibleModelException( "Matrix not invertible." );
		
		final float i00 = ( m11 * m22 - m12 * m21 ) / det;
		final float i01 = ( m02 * m21 - m01 * m22 ) / det;
		final float i02 = ( m01 * m12 - m02 * m11 ) / det;
		
		final float i10 = ( m12 * m20 - m10 * m22 ) / det;
		final float i11 = ( m00 * m22 - m02 * m20 ) / det;
		final float i12 = ( m02 * m10 - m00 * m12 ) / det;
		
		final float i20 = ( m10 * m21 - m11 * m20 ) / det;
		final float i21 = ( m01 * m20 - m00 * m21 ) / det;
		final float i22 = ( m00 * m11 - m01 * m10 ) / det;
		
		m00 = i00;
		m01 = i01;
		m02 = i02;

		m10 = i10;
		m11 = i11;
		m12 = i12;

		m20 = i20;
		m21 = i21;
		m22 = i22;
	}
	
	
	final static public void invert( float[] m ) throws NoninvertibleModelException
	{
		assert m.length == 9 : "Matrix3x3 supports 3x3 float[][] only.";
		
		final float det = det( m );
		if ( det == 0 ) throw new NoninvertibleModelException( "Matrix not invertible." );
		
		final float i00 = ( m[ 4 ] * m[ 8 ] - m[ 5 ] * m[ 7 ] ) / det;
		final float i01 = ( m[ 2 ] * m[ 7 ] - m[ 1 ] * m[ 8 ] ) / det;
		final float i02 = ( m[ 1 ] * m[ 5 ] - m[ 2 ] * m[ 4 ] ) / det;
		
		final float i10 = ( m[ 5 ] * m[ 6 ] - m[ 3 ] * m[ 8 ] ) / det;
		final float i11 = ( m[ 0 ] * m[ 8 ] - m[ 2 ] * m[ 6 ] ) / det;
		final float i12 = ( m[ 2 ] * m[ 3 ] - m[ 0 ] * m[ 5 ] ) / det;
		
		final float i20 = ( m[ 3 ] * m[ 7 ] - m[ 4 ] * m[ 6 ] ) / det;
		final float i21 = ( m[ 1 ] * m[ 6 ] - m[ 0 ] * m[ 7 ] ) / det;
		final float i22 = ( m[ 0 ] * m[ 4 ] - m[ 1 ] * m[ 3 ] ) / det;
		
		m[ 0 ] = i00;
		m[ 1 ] = i01;
		m[ 2 ] = i02;

		m[ 3 ] = i10;
		m[ 4 ] = i11;
		m[ 5 ] = i12;

		m[ 6 ] = i20;
		m[ 7 ] = i21;
		m[ 8 ] = i22;
	}

	final static public float[] createInverse(
			final float m00, final float m01, final float m02,
			final float m10, final float m11, final float m12,
			final float m20, final float m21, final float m22 ) throws NoninvertibleModelException
	{
		final float det = det( m00, m01, m02, m10, m11, m12, m20, m21, m22 );
		if ( det == 0 ) throw new NoninvertibleModelException( "Matrix not invertible." );
		
		return new float[]{
				( m11 * m22 - m12 * m21 ) / det, ( m02 * m21 - m01 * m22 ) / det, ( m01 * m12 - m02 * m11 ) / det,
				( m12 * m20 - m10 * m22 ) / det, ( m00 * m22 - m02 * m20 ) / det, ( m02 * m10 - m00 * m12 ) / det,
				( m10 * m21 - m11 * m20 ) / det, ( m01 * m20 - m00 * m21 ) / det, ( m00 * m11 - m01 * m10 ) / det };
	}
	
	
	/**
	 * Concatenate a matrix:
	 * 
	 * this = this &times; m
	 * 
	 * @param m
	 */
	final public void concatenate( final Matrix3x3 m )
	{
		final float a00 = m00 * m.m00 + m01 * m.m10 + m02 * m.m20;
		final float a01 = m00 * m.m01 + m01 * m.m11 + m02 * m.m21;
		final float a02 = m00 * m.m02 + m01 * m.m12 + m02 * m.m22;
		
		final float a10 = m10 * m.m00 + m11 * m.m10 + m12 * m.m20;
		final float a11 = m10 * m.m01 + m11 * m.m11 + m12 * m.m21;
		final float a12 = m10 * m.m02 + m11 * m.m12 + m12 * m.m22;
		
		final float a20 = m20 * m.m00 + m21 * m.m10 + m22 * m.m20;
		final float a21 = m20 * m.m01 + m21 * m.m11 + m22 * m.m21;
		final float a22 = m20 * m.m02 + m21 * m.m12 + m22 * m.m22;
		
		m00 = a00;
		m01 = a01;
		m02 = a02;
		
		m10 = a10;
		m11 = a11;
		m12 = a12;
		
		m20 = a20;
		m21 = a21;
		m22 = a22;
	}
	
	
	/**
	 * Pre-concatenate a matrix:
	 * 
	 * this = m &times; this
	 * 
	 * @param m
	 */
	final public void preConcatenate( final Matrix3x3 m )
	{
		final float a00 = m.m00 * m00 + m.m01 * m10 + m.m02 * m20;
		final float a01 = m.m00 * m01 + m.m01 * m11 + m.m02 * m21;
		final float a02 = m.m00 * m02 + m.m01 * m12 + m.m02 * m22;
		
		final float a10 = m.m10 * m00 + m.m11 * m10 + m.m12 * m20;
		final float a11 = m.m10 * m01 + m.m11 * m11 + m.m12 * m21;
		final float a12 = m.m10 * m02 + m.m11 * m12 + m.m12 * m22;
		
		final float a20 = m.m20 * m00 + m.m21 * m10 + m.m22 * m20;
		final float a21 = m.m20 * m01 + m.m21 * m11 + m.m22 * m21;
		final float a22 = m.m20 * m02 + m.m21 * m12 + m.m22 * m22;
		
		m00 = a00;
		m01 = a01;
		m02 = a02;
		
		m10 = a10;
		m11 = a11;
		m12 = a12;
		
		m20 = a20;
		m21 = a21;
		m22 = a22;
	}
	
	
	final public void set( final Matrix3x3 m )
	{
		m00 = m.m00;
		m01 = m.m01;
		m02 = m.m02;
		
		m10 = m.m10;
		m11 = m.m11;
		m12 = m.m12;
		
		m20 = m.m20;
		m21 = m.m21;
		m22 = m.m22;
	}
	
	/**
	 * Set to identity.
	 */
	final public void reset()
	{
		m00 = 1;
		m01 = 0;
		m02 = 0;
		
		m10 = 0;
		m11 = 1;
		m12 = 0;
		
		m20 = 0;
		m21 = 0;
		m22 = 1;
	}
	
	@Override
	final public Matrix3x3 clone()
	{
		return new Matrix3x3(
				m00, m01, m02,
				m10, m11, m12,
				m20, m21, m22 );
	}
}
