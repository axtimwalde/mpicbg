package mpicbg.models;

import java.util.Collection;
import Jama.Matrix;

public class HomographyModel2D extends Model
{
	static final protected int MIN_SET_SIZE = 4;
	
	@Override
	public int getMinSetSize(){ return MIN_SET_SIZE; }

	Matrix a = new Matrix( 3, 3 );
	Matrix a_inverse = new Matrix( 3, 3 );
	
	private Matrix getInverse( Matrix m )
	{
		double[][] b = new double[][]{
			new double[ 3 ],
			new double[ 3 ],
			new double[ 3 ]	};
		
		double[][] c = m.getArray();
		
		double d =
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
	
	/**
	 * This code is based on the following book:
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
	 */
	private Matrix fitToUnitSquare(
		Point p1,
		Point p2,
		Point p3,
		Point p4 )
	{
		float[] l1 = p1.getL();
		float[] l2 = p2.getL();
		float[] l3 = p3.getL();
		float[] l4 = p4.getL();
		
		double x1 = l1[ 0 ];
		double x2 = l2[ 0 ];
		double x3 = l3[ 0 ];
		double x4 = l4[ 0 ];
		
		double y1 = l1[ 1 ];
		double y2 = l2[ 1 ];
		double y3 = l3[ 1 ];
		double y4 = l4[ 1 ];
		
		double s = ( x2 - x3 ) * ( y4 - y3 ) - ( x4 - x3 ) * ( y2 - y3 );
		
		double[][] b = new double[][]{
				new double[ 3 ],
				new double[ 3 ],
				new double[ 3 ]	};

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
	
	@Override
	public float[] apply( float[] point )
	{
		float[] t = point.clone();
		applyInPlace( t );
		return t;
	}

	@Override
	public void applyInPlace( float[] point )
	{
		double h[][] = a.getArray();
		double s = h[ 0 ][ 2 ] * point[ 0 ] + h[ 1 ][ 2 ] * point[ 1 ] + h[ 2 ][ 2 ];
		double t0 = h[ 0 ][ 0 ] * point[ 0 ] + h[ 1 ][ 0 ] * point[ 1 ] + h[ 2 ][ 0 ];
		double t1 = h[ 0 ][ 1 ] * point[ 0 ] + h[ 1 ][ 1 ] * point[ 1 ] + h[ 2 ][ 1 ];
		
		point[ 0 ] = ( float )( t0 / s );
		point[ 1 ] = ( float )( t1 / s );
	}

	@Override
	public float[] applyInverse( float[] point ) throws NoninvertibleModelException
	{
		float[] t = point.clone();
		applyInPlace( t );
		return null;
	}

	@Override
	public void applyInverseInPlace( float[] point ) throws NoninvertibleModelException
	{
		double h[][] = a_inverse.getArray();
		double s = h[ 0 ][ 2 ] * point[ 0 ] + h[ 1 ][ 2 ] * point[ 1 ] + h[ 2 ][ 2 ];
		double t0 = h[ 0 ][ 0 ] * point[ 0 ] + h[ 1 ][ 0 ] * point[ 1 ] + h[ 2 ][ 0 ];
		double t1 = h[ 0 ][ 1 ] * point[ 0 ] + h[ 1 ][ 1 ] * point[ 1 ] + h[ 2 ][ 1 ];
		
		point[ 0 ] = ( float )( t0 / s );
		point[ 1 ] = ( float )( t1 / s);
	}

	@Override
	public HomographyModel2D clone()
	{
		HomographyModel2D trm = new HomographyModel2D();
		trm.a = ( Matrix )a.clone();
		trm.a_inverse = ( Matrix )a_inverse.clone();
		trm.error = error;
		return trm;
	}

	@Override
	public void fit( Collection< PointMatch > matches ) throws NotEnoughDataPointsException
	{
		if ( matches.size() < MIN_SET_SIZE ) throw new NotEnoughDataPointsException( matches.size() + " correspondences are not enough to estimate a 2d homography model, at least " + MIN_SET_SIZE + " correspondences required." );
		
		// Having a minimal set of data points, the homography can be estimated
		// exactly in a direct manner.
		if ( matches.size() == MIN_SET_SIZE )
		{
			PointMatch[] p = new PointMatch[ MIN_SET_SIZE ];
			matches.toArray( p );
			
			Matrix h1 = fitToUnitSquare( p[ 0 ].getP1(), p[ 1 ].getP1(), p[ 2 ].getP1(), p[ 3 ].getP1() );
			Matrix h2 = fitToUnitSquare( p[ 0 ].getP2(), p[ 1 ].getP2(), p[ 2 ].getP2(), p[ 3 ].getP2() );
			
			h1 = getInverse( h1 );
			a = h1.times( h2 );
			a_inverse = getInverse( a );
		}
		else throw new NotEnoughDataPointsException( "Sorry---we did not implement an optimal homography solver for more than four correspondences.  If you have time, sit down and do it ;)" );
	}

	@Override
	public void shake( Collection< PointMatch > matches, float scale, float[] center )
	{
		// TODO Auto-generated method stub

	}

	@Override
	public String toString()
	{
		double[][] b = a.getArray();
		return (
				"| " + b[ 0 ][ 0 ] + " " + b[ 1 ][ 0 ] + " " + b[ 2 ][ 0 ] + " |\n" +
				"| " + b[ 0 ][ 1 ] + " " + b[ 1 ][ 1 ] + " " + b[ 2 ][ 1 ] + " |\n" +
				"| " + b[ 0 ][ 2 ] + " " + b[ 1 ][ 2 ] + " " + b[ 2 ][ 2 ] + " |" );
	}

}
