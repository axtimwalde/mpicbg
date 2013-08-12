package mpicbg.models;

import java.util.Collection;

import mpicbg.util.Matrix3x3;
import Jama.Matrix;
import Jama.SingularValueDecomposition;

/**
 * 2d-homography {@link AbstractModel} to be applied to points in 2d-space.
 * 
 * This code is partially based on the following book:
 * 
 * BibTeX:
 * <pre>
 * &#64;book{BurgerB05,
 *	 author    = {Wilhelm Burger and Mark James Burge},
 *   title     = {Digital image processing: An algorithmic introduction using Java},
 *   year      = {2008},
 *   isbn      = {978-1-84628-379-6},
 *   pages     = {560},
 *   publisher = {Springer},
 *   url       = {http://imagingbook.com/},
 * }
 * </pre>
 * 
 * and the lecture notes:
 * 
 * CSE 252B: Computer Vision II
 * Lecturer: Serge Belongie
 * Scribe: Dave Berlin, Jefferson Ng
 * LECTURE 2
 * Homogeneous Linear Least Squares
 * Problems, Two View Geometry
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.3b
 * 
 */
public class HomographyModel2D extends AbstractModel< HomographyModel2D > implements InvertibleBoundable
{
	private static final long serialVersionUID = -1730876468690649135L;
	
	static final protected int MIN_NUM_MATCHES = 4;
	
	@Override
	final public int getMinNumMatches(){ return MIN_NUM_MATCHES; }
	
	protected float
			m00 = 1, m01 = 0, m02 = 0,
			m10 = 0, m11 = 1, m12 = 0,
			m20 = 0, m21 = 0, m22 = 1;
	
	public void set(
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
		
		invert();
	}
	
	protected float
			i00 = 1, i01 = 0, i02 = 0,
			i10 = 0, i11 = 1, i12 = 0,
			i20 = 0, i21 = 0, i22 = 1;
	
	final private void invert()
	{
		final float det = Matrix3x3.det(
				m00, m01, m02,
				m10, m11, m12,
				m20, m21, m22 );
		
		i00 = ( m11 * m22 - m12 * m21 ) / det;
		i01 = ( m02 * m21 - m01 * m22 ) / det;
		i02 = ( m01 * m12 - m02 * m11 ) / det;
		
		i10 = ( m12 * m20 - m10 * m22 ) / det;
		i11 = ( m00 * m22 - m02 * m20 ) / det;
		i12 = ( m02 * m10 - m00 * m12 ) / det;
		
		i20 = ( m10 * m21 - m11 * m20 ) / det;
		i21 = ( m01 * m20 - m00 * m21 ) / det;
		i22 = ( m00 * m11 - m01 * m10 ) / det;
	}
	
	final static private float[] fitToUnitSquare(
		final float[] p1,
		final float[] p2,
		final float[] p3,
		final float[] p4 )
	{
		final float x1 = p1[ 0 ];
		final float x2 = p2[ 0 ];
		final float x3 = p3[ 0 ];
		final float x4 = p4[ 0 ];
		
		final float y1 = p1[ 1 ];
		final float y2 = p2[ 1 ];
		final float y3 = p3[ 1 ];
		final float y4 = p4[ 1 ];
		
		final float s = ( x2 - x3 ) * ( y4 - y3 ) - ( x4 - x3 ) * ( y2 - y3 );
		
		final float
				b00, b01, b02,
				b10, b11, b12,
				b20, b21, b22;
		
		b20 = ( ( x1 - x2 + x3 - x4 ) * ( y4 - y3 ) - ( y1 - y2 + y3 - y4 ) * ( x4 - x3 ) ) / s;
		b21 = ( ( y1 - y2 + y3 - y4 ) * ( x2 - x3 ) - ( x1 - x2 + x3 - x4 ) * ( y2 - y3 ) ) / s;
		b00 = x2 - x1 + b20 * x2;
		b01 = x4 - x1 + b21 * x4;
		b02 = x1;
		b10 = y2 - y1 + b20 * y2;
		b11 = y4 - y1 + b21 * y4;
		b12 = y1;
		b22 = 1;
		
		return new float[]{
				b00, b01, b02,
				b10, b11, b12,
				b20, b21, b22 };
	}
	
	//@Override
	@Override
	final public float[] apply( final float[] point )
	{
		assert point.length >= 2 : "2d homographies can be applied to 2d points only.";
		
		final float[] t = point.clone();
		applyInPlace( t );
		return t;
	}

	//@Override
	@Override
	final public void applyInPlace( final float[] point )
	{
		assert point.length >= 2 : "2d homographies can be applied to 2d points only.";
		
		final double s = m20 * point[ 0 ] + m21 * point[ 1 ] + m22;
		final double t0 = m00 * point[ 0 ] + m01 * point[ 1 ] + m02;
		final double t1 = m10 * point[ 0 ] + m11 * point[ 1 ] + m12;
		
		point[ 0 ] = ( float )( t0 / s );
		point[ 1 ] = ( float )( t1 / s );
	}

	//@Override
	@Override
	final public float[] applyInverse( final float[] point ) throws NoninvertibleModelException
	{
		assert point.length >= 2 : "2d homographies can be applied to 2d points only.";
		
		final float[] t = point.clone();
		applyInPlace( t );
		return null;
	}

	//@Override
	@Override
	final public void applyInverseInPlace( final float[] point ) throws NoninvertibleModelException
	{
		assert point.length >= 2 : "2d homographies can be applied to 2d points only.";
		
		final double s = i20 * point[ 0 ] + i21 * point[ 1 ] + i22;
		final double t0 = i00 * point[ 0 ] + i01 * point[ 1 ] + i02;
		final double t1 = i10 * point[ 0 ] + i11 * point[ 1 ] + i12;
		
		point[ 0 ] = ( float )( t0 / s );
		point[ 1 ] = ( float )( t1 / s);
	}
	
	@Override
	final public void set( final HomographyModel2D m )
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
		
		
		i00 = m.i00;
		i01 = m.i01;
		i02 = m.i02;
		
		i10 = m.i10;
		i11 = m.i11;
		i12 = m.i12;
		
		i20 = m.i20;
		i21 = m.i21;
		i22 = m.i22;
		
		cost = m.getCost();
	}
	
	@Override
	public HomographyModel2D copy()
	{
		final HomographyModel2D m = new HomographyModel2D();
		
		m.m00 = m00;
		m.m01 = m01;
		m.m02 = m02;
		
		m.m10 = m10;
		m.m11 = m11;
		m.m12 = m12;
		
		m.m20 = m20;
		m.m21 = m21;
		m.m22 = m22;
		
		
		m.i00 = i00;
		m.i01 = i01;
		m.i02 = i02;
		
		m.i10 = i10;
		m.i11 = i11;
		m.i12 = i12;
		
		m.i20 = i20;
		m.i21 = i21;
		m.i22 = i22;
		
		m.cost = getCost();
		
		return m;
	}

	@Override
	final public < P extends PointMatch >void fit( final Collection< P > matches ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		if ( matches.size() < MIN_NUM_MATCHES ) throw new NotEnoughDataPointsException( matches.size() + " data points are not enough to estimate a 2d homography model, at least " + MIN_NUM_MATCHES + " data points required." );
		
		// Having a minimal set of data points, the homography can be estimated
		// exactly in a direct manner.
		if ( matches.size() == MIN_NUM_MATCHES )
		{
			final PointMatch[] p = new PointMatch[ MIN_NUM_MATCHES ];
			matches.toArray( p );
			
			final float[] h1 = fitToUnitSquare(
					p[ 0 ].getP1().getL(),
					p[ 1 ].getP1().getL(),
					p[ 2 ].getP1().getL(),
					p[ 3 ].getP1().getL() );
			final float[] h2 = fitToUnitSquare(
					p[ 0 ].getP2().getW(),
					p[ 1 ].getP2().getW(),
					p[ 2 ].getP2().getW(),
					p[ 3 ].getP2().getW() );
			
			try
			{
				Matrix3x3.invert( h1 );
				
				m00 = h2[ 0 ] * h1[ 0 ] + h2[ 1 ] * h1[ 3 ] + h2[ 2 ] * h1[ 6 ];
				m01 = h2[ 0 ] * h1[ 1 ] + h2[ 1 ] * h1[ 4 ] + h2[ 2 ] * h1[ 7 ];
				m02 = h2[ 0 ] * h1[ 2 ] + h2[ 1 ] * h1[ 5 ] + h2[ 2 ] * h1[ 8 ];
				
				m10 = h2[ 3 ] * h1[ 0 ] + h2[ 4 ] * h1[ 3 ] + h2[ 5 ] * h1[ 6 ];
				m11 = h2[ 3 ] * h1[ 1 ] + h2[ 4 ] * h1[ 4 ] + h2[ 5 ] * h1[ 7 ];
				m12 = h2[ 3 ] * h1[ 2 ] + h2[ 4 ] * h1[ 5 ] + h2[ 5 ] * h1[ 8 ];
				
				m20 = h2[ 6 ] * h1[ 0 ] + h2[ 7 ] * h1[ 3 ] + h2[ 8 ] * h1[ 6 ];
				m21 = h2[ 6 ] * h1[ 1 ] + h2[ 7 ] * h1[ 4 ] + h2[ 8 ] * h1[ 7 ];
				m22 = h2[ 6 ] * h1[ 2 ] + h2[ 7 ] * h1[ 5 ] + h2[ 8 ] * h1[ 8 ];
				
				invert();
			}
			catch ( final NoninvertibleModelException e )
			{
				throw new IllDefinedDataPointsException();
			}
		}
		else
		{
			final int n = matches.size() * 2;
			final double[][] a = new double[ n ][ 9 ];
			int i = 0;
			
			for ( final P pm : matches )
			{	
				final float[] p = pm.getP1().getL();
				final float[] q = pm.getP2().getW();
				
				double px = p[ 0 ];
				double py = p[ 1 ];
				double qx = q[ 0 ];
				double qy = q[ 1 ];
				
				// without testing that, it will send the SingularValueDecomposition
				// into an infinite loop if one of them is NaN or Infinite
				if ( Double.isInfinite( px ) || Double.isNaN( px ) )
					px = 1;

				if ( Double.isInfinite( py ) || Double.isNaN( py ))
					py = 1;

				if ( Double.isInfinite( qx ) || Double.isNaN( qx ))
					qx = 1;

				if ( Double.isInfinite( qy ) || Double.isNaN( qy ))
					qy = 1;
				
				a[ i ][ 0 ] = -px;
				a[ i ][ 1 ] = -py;
				a[ i ][ 2 ] = -1;
				a[ i ][ 6 ] = qx * px;
				a[ i ][ 7 ] = qx * py;
				a[ i++ ][ 8 ] = qx;
				
				a[ i ][ 3 ] = -px;
				a[ i ][ 4 ] = -py;
				a[ i ][ 5 ] = -1;
				a[ i ][ 6 ] = qy * px;
				a[ i ][ 7 ] = qy * py;
				a[ i++ ][ 8 ] = qy;
			}
			
			final Matrix mA = new Matrix( a );
			final SingularValueDecomposition svd = new SingularValueDecomposition( mA );
			final Matrix s = svd.getS();
			final Matrix v = svd.getV();
			
			cost = s.get( 8, 8 );
			
			m00 = ( float )v.get( 0, 8 );
			m01 = ( float )v.get( 1, 8 );
			m02 = ( float )v.get( 2, 8 );
			m10 = ( float )v.get( 3, 8 );
			m11 = ( float )v.get( 4, 8 );
			m12 = ( float )v.get( 5, 8 );
			m20 = ( float )v.get( 6, 8 );
			m21 = ( float )v.get( 7, 8 );
			m22 = ( float )v.get( 8, 8 );
			
			invert();
			
//			throw new NotEnoughDataPointsException( "Sorry---we did not implement an optimal homography solver for more than four correspondences.  If you have time, sit down and do it ;)" );
		}
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
	public String toString()
	{
//		final double[][] b = a.getArray();
//		return (
//				"| " + b[ 0 ][ 0 ] + " " + b[ 1 ][ 0 ] + " " + b[ 2 ][ 0 ] + " |\n" +
//				"| " + b[ 0 ][ 1 ] + " " + b[ 1 ][ 1 ] + " " + b[ 2 ][ 1 ] + " |\n" +
//				"| " + b[ 0 ][ 2 ] + " " + b[ 1 ][ 2 ] + " " + b[ 2 ][ 2 ] + " |" );
		return 
				"| " + m00 + " " + m01 + " " + m02 + " |\n" +
				"| " + m10 + " " + m11 + " " + m12 + " |\n" +
				"| " + m20 + " " + m21 + " " + m22 + " |";
	}
	
	@Override
	public void estimateBounds( final float[] min, final float[] max )
	{
		assert min.length >= 2 && max.length >= 2 : "2d homographies can be applied to 2d points only.";
		
		float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
		
		final float[] l = min.clone();
		applyInPlace( l );
		
		if ( l[ 0 ] < minX ) minX = l[ 0 ];
		if ( l[ 0 ] > maxX ) maxX = l[ 0 ];
		if ( l[ 1 ] < minY ) minY = l[ 1 ];
		if ( l[ 1 ] > maxY ) maxY = l[ 1 ];
		
		l[ 0 ] = min[ 0 ];
		l[ 1 ] = max[ 1 ];
		applyInPlace( l );
		
		if ( l[ 0 ] < minX ) minX = l[ 0 ];
		else if ( l[ 0 ] > maxX ) maxX = l[ 0 ];
		if ( l[ 1 ] < minY ) minY = l[ 1 ];
		else if ( l[ 1 ] > maxY ) maxY = l[ 1 ];
		
		l[ 0 ] = max[ 0 ];
		l[ 1 ] = max[ 1 ];
		applyInPlace( l );
		
		if ( l[ 0 ] < minX ) minX = l[ 0 ];
		else if ( l[ 0 ] > maxX ) maxX = l[ 0 ];
		if ( l[ 1 ] < minY ) minY = l[ 1 ];
		else if ( l[ 1 ] > maxY ) maxY = l[ 1 ];
		
		l[ 0 ] = max[ 0 ];
		l[ 1 ] = min[ 1 ];
		applyInPlace( l );
		
		if ( l[ 0 ] < minX ) minX = l[ 0 ];
		else if ( l[ 0 ] > maxX ) maxX = l[ 0 ];
		if ( l[ 1 ] < minY ) minY = l[ 1 ];
		else if ( l[ 1 ] > maxY ) maxY = l[ 1 ];
		
		min[ 0 ] = minX;
		min[ 1 ] = minY;
		max[ 0 ] = maxX;
		max[ 1 ] = maxY;
	}
	
	@Override
	public void estimateInverseBounds( final float[] min, final float[] max ) throws NoninvertibleModelException
	{
		assert min.length >= 2 && max.length >= 2 : "2d affine transformations can be applied to 2d points only.";
		
		float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
		
		final float[] l = min.clone();
		applyInverseInPlace( l );
		
		if ( l[ 0 ] < minX ) minX = l[ 0 ];
		if ( l[ 0 ] > maxX ) maxX = l[ 0 ];
		if ( l[ 1 ] < minY ) minY = l[ 1 ];
		if ( l[ 1 ] > maxY ) maxY = l[ 1 ];
		
		l[ 0 ] = min[ 0 ];
		l[ 1 ] = max[ 1 ];
		applyInverseInPlace( l );
		
		if ( l[ 0 ] < minX ) minX = l[ 0 ];
		else if ( l[ 0 ] > maxX ) maxX = l[ 0 ];
		if ( l[ 1 ] < minY ) minY = l[ 1 ];
		else if ( l[ 1 ] > maxY ) maxY = l[ 1 ];
		
		l[ 0 ] = max[ 0 ];
		l[ 1 ] = max[ 1 ];
		applyInverseInPlace( l );
		
		if ( l[ 0 ] < minX ) minX = l[ 0 ];
		else if ( l[ 0 ] > maxX ) maxX = l[ 0 ];
		if ( l[ 1 ] < minY ) minY = l[ 1 ];
		else if ( l[ 1 ] > maxY ) maxY = l[ 1 ];
		
		l[ 0 ] = max[ 0 ];
		l[ 1 ] = min[ 1 ];
		applyInverseInPlace( l );
		
		if ( l[ 0 ] < minX ) minX = l[ 0 ];
		else if ( l[ 0 ] > maxX ) maxX = l[ 0 ];
		if ( l[ 1 ] < minY ) minY = l[ 1 ];
		else if ( l[ 1 ] > maxY ) maxY = l[ 1 ];
		
		min[ 0 ] = minX;
		min[ 1 ] = minY;
		max[ 0 ] = maxX;
		max[ 1 ] = maxY;
	}
	
	/**
	 * TODO Not yet tested
	 */
	//@Override
	@Override
	final public HomographyModel2D createInverse()
	{
		final HomographyModel2D m = new HomographyModel2D();
		
		m.m00 = i00;
		m.m01 = i01;
		m.m02 = i02;
		
		m.m10 = i00;
		m.m11 = i11;
		m.m12 = i12;
		
		m.m20 = i20;
		m.m21 = i21;
		m.m22 = i22;
		
		
		m.i00 = m00;
		m.i01 = m01;
		m.i02 = m02;
		
		m.i10 = m00;
		m.i11 = m11;
		m.i12 = m12;
		
		m.i20 = m20;
		m.i21 = m21;
		m.i22 = m22;
		
		m.cost = getCost();
		
		return m;
	}
}
