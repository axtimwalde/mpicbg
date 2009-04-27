package mpicbg.models;

import java.util.Collection;
import Jama.Matrix;

/**
 * 2d-homography {@link Model} to be applied to points in 2d-space.
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
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.3b
 * 
 */
public class HomographyModel2D extends InvertibleModel< HomographyModel2D > implements InvertibleBoundable
{
	static final protected int MIN_NUM_MATCHES = 4;
	
	@Override
	final public int getMinNumMatches(){ return MIN_NUM_MATCHES; }

	Matrix a = new Matrix( 3, 3 );
	Matrix inverseA = new Matrix( 3, 3 );
	
	final private Matrix getInverse( final Matrix m )
	{
		final double[][] b = new double[ 3 ][ 3 ];
		
		final double[][] c = m.getArray();
		
		final double d =
				c[ 0 ][ 0 ] * c[ 1 ][ 1 ] * c[ 2 ][ 2 ] +
				c[ 1 ][ 0 ] * c[ 2 ][ 1 ] * c[ 0 ][ 2 ] +
				c[ 2 ][ 0 ] * c[ 0 ][ 1 ] * c[ 1 ][ 2 ] -
				c[ 0 ][ 0 ] * c[ 2 ][ 1 ] * c[ 1 ][ 2 ] -
				c[ 1 ][ 0 ] * c[ 0 ][ 1 ] * c[ 2 ][ 2 ] -
				c[ 2 ][ 0 ] * c[ 1 ][ 1 ] * c[ 0 ][ 2 ];
		
		b[ 0 ][ 0 ] = ( c[ 1 ][ 1 ] * c[ 2 ][ 2 ] - c[ 2 ][ 1 ] * c[ 1 ][ 2 ] ) / d; 
		b[ 1 ][ 0 ] = ( c[ 2 ][ 0 ] * c[ 1 ][ 2 ] - c[ 1 ][ 0 ] * c[ 2 ][ 2 ] ) / d; 
		b[ 2 ][ 0 ] = ( c[ 1 ][ 0 ] * c[ 2 ][ 1 ] - c[ 2 ][ 0 ] * c[ 1 ][ 1 ] ) / d; 
		
		b[ 0 ][ 1 ] = ( c[ 2 ][ 1 ] * c[ 0 ][ 2 ] - c[ 0 ][ 1 ] * c[ 2 ][ 2 ] ) / d; 
		b[ 1 ][ 1 ] = ( c[ 0 ][ 0 ] * c[ 2 ][ 2 ] - c[ 2 ][ 0 ] * c[ 0 ][ 2 ] ) / d; 
		b[ 2 ][ 1 ] = ( c[ 2 ][ 0 ] * c[ 0 ][ 1 ] - c[ 0 ][ 0 ] * c[ 2 ][ 1 ] ) / d;
		
		b[ 0 ][ 2 ] = ( c[ 0 ][ 1 ] * c[ 1 ][ 2 ] - c[ 1 ][ 1 ] * c[ 0 ][ 2 ] ) / d; 
		b[ 1 ][ 2 ] = ( c[ 1 ][ 0 ] * c[ 0 ][ 2 ] - c[ 0 ][ 0 ] * c[ 1 ][ 2 ] ) / d;
		b[ 2 ][ 2 ] = ( c[ 0 ][ 0 ] * c[ 1 ][ 1 ] - c[ 1 ][ 0 ] * c[ 0 ][ 1 ] ) / d;
		
		return new Matrix( b );
		//return m.inverse();
	}
	
	final private Matrix fitToUnitSquare(
		final float[] p1,
		final float[] p2,
		final float[] p3,
		final float[] p4 )
	{
		final double x1 = p1[ 0 ];
		final double x2 = p2[ 0 ];
		final double x3 = p3[ 0 ];
		final double x4 = p4[ 0 ];
		
		final double y1 = p1[ 1 ];
		final double y2 = p2[ 1 ];
		final double y3 = p3[ 1 ];
		final double y4 = p4[ 1 ];
		
		final double s = ( x2 - x3 ) * ( y4 - y3 ) - ( x4 - x3 ) * ( y2 - y3 );
		
		final double[][] b = new double[ 3 ][ 3 ];

		b[ 0 ][ 2 ] = ( ( x1 - x2 + x3 - x4 ) * ( y4 - y3 ) - ( y1 - y2 + y3 - y4 ) * ( x4 - x3 ) ) / s;
		b[ 1 ][ 2 ] = ( ( y1 - y2 + y3 - y4 ) * ( x2 - x3 ) - ( x1 - x2 + x3 - x4 ) * ( y2 - y3 ) ) / s;
		b[ 0 ][ 0 ] = x2 - x1 + b[ 0 ][ 2 ] * x2;
		b[ 1 ][ 0 ] = x4 - x1 + b[ 1 ][ 2 ] * x4;
		b[ 2 ][ 0 ] = x1;
		b[ 0 ][ 1 ] = y2 - y1 + b[ 0 ][ 2 ] * y2;
		b[ 1 ][ 1 ] = y4 - y1 + b[ 1 ][ 2 ] * y4;
		b[ 2 ][ 1 ] = y1;
		b[ 2 ][ 2 ] = 1.0;
		
		return new Matrix( b );
	}
	
	//@Override
	final public float[] apply( final float[] point )
	{
		assert point.length == 2 : "2d homographies can be applied to 2d points only.";
		
		final float[] t = point.clone();
		applyInPlace( t );
		return t;
	}

	//@Override
	final public void applyInPlace( final float[] point )
	{
		assert point.length == 2 : "2d homographies can be applied to 2d points only.";
		
		final double h[][] = a.getArray();
		final double s = h[ 0 ][ 2 ] * point[ 0 ] + h[ 1 ][ 2 ] * point[ 1 ] + h[ 2 ][ 2 ];
		final double t0 = h[ 0 ][ 0 ] * point[ 0 ] + h[ 1 ][ 0 ] * point[ 1 ] + h[ 2 ][ 0 ];
		final double t1 = h[ 0 ][ 1 ] * point[ 0 ] + h[ 1 ][ 1 ] * point[ 1 ] + h[ 2 ][ 1 ];
		
		point[ 0 ] = ( float )( t0 / s );
		point[ 1 ] = ( float )( t1 / s );
	}

	//@Override
	final public float[] applyInverse( final float[] point ) throws NoninvertibleModelException
	{
		assert point.length == 2 : "2d homographies can be applied to 2d points only.";
		
		final float[] t = point.clone();
		applyInPlace( t );
		return null;
	}

	//@Override
	final public void applyInverseInPlace( final float[] point ) throws NoninvertibleModelException
	{
		assert point.length == 2 : "2d homographies can be applied to 2d points only.";
		
		final double h[][] = inverseA.getArray();
		final double s = h[ 0 ][ 2 ] * point[ 0 ] + h[ 1 ][ 2 ] * point[ 1 ] + h[ 2 ][ 2 ];
		final double t0 = h[ 0 ][ 0 ] * point[ 0 ] + h[ 1 ][ 0 ] * point[ 1 ] + h[ 2 ][ 0 ];
		final double t1 = h[ 0 ][ 1 ] * point[ 0 ] + h[ 1 ][ 1 ] * point[ 1 ] + h[ 2 ][ 1 ];
		
		point[ 0 ] = ( float )( t0 / s );
		point[ 1 ] = ( float )( t1 / s);
	}
	
	@Override
	final public void set( final HomographyModel2D m )
	{
		a = ( Matrix )m.a.clone();
		inverseA = ( Matrix )m.inverseA.clone();
		cost = m.getCost();
	}
	
	@Override
	final public HomographyModel2D clone()
	{
		final HomographyModel2D m = new HomographyModel2D();
		m.a = ( Matrix )a.clone();
		m.inverseA = ( Matrix )inverseA.clone();
		m.cost = cost;
		return m;
	}

	@Override
	final public void fit( final Collection< PointMatch > matches ) throws NotEnoughDataPointsException
	{
		if ( matches.size() < MIN_NUM_MATCHES ) throw new NotEnoughDataPointsException( matches.size() + " data points are not enough to estimate a 2d homography model, at least " + MIN_NUM_MATCHES + " data points required." );
		
		// Having a minimal set of data points, the homography can be estimated
		// exactly in a direct manner.
		if ( matches.size() == MIN_NUM_MATCHES )
		{
			final PointMatch[] p = new PointMatch[ MIN_NUM_MATCHES ];
			matches.toArray( p );
			
			Matrix h1 = fitToUnitSquare(
					p[ 0 ].getP1().getL(),
					p[ 1 ].getP1().getL(),
					p[ 2 ].getP1().getL(),
					p[ 3 ].getP1().getL() );
			final Matrix h2 = fitToUnitSquare(
					p[ 0 ].getP2().getW(),
					p[ 1 ].getP2().getW(),
					p[ 2 ].getP2().getW(),
					p[ 3 ].getP2().getW() );
			
			h1 = getInverse( h1 );
			a = h1.times( h2 );
			inverseA = getInverse( a );
		}
		else throw new NotEnoughDataPointsException( "Sorry---we did not implement an optimal homography solver for more than four correspondences.  If you have time, sit down and do it ;)" );
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
	final public String toString()
	{
		final double[][] b = a.getArray();
		return (
				"| " + b[ 0 ][ 0 ] + " " + b[ 1 ][ 0 ] + " " + b[ 2 ][ 0 ] + " |\n" +
				"| " + b[ 0 ][ 1 ] + " " + b[ 1 ][ 1 ] + " " + b[ 2 ][ 1 ] + " |\n" +
				"| " + b[ 0 ][ 2 ] + " " + b[ 1 ][ 2 ] + " " + b[ 2 ][ 2 ] + " |" );
	}
	
	//@Override
	public void estimateBounds( final float[] min, final float[] max )
	{
		assert min.length == 2 && max.length == 2 : "2d affine transformations can be applied to 2d points only.";
		
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
	
	//@Override
	public void estimateInverseBounds( final float[] min, final float[] max ) throws NoninvertibleModelException
	{
		assert min.length == 2 && max.length == 2 : "2d affine transformations can be applied to 2d points only.";
		
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
}
