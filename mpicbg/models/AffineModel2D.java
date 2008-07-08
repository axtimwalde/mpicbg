package mpicbg.models;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.util.Collection;

/**
 * 2d-affine transformation models to be applied to points in 2d-space.
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 *
 */
public class AffineModel2D extends Model
{
	static final protected int MIN_SET_SIZE = 3;
	
	@Override
	public int getMinSetSize(){ return MIN_SET_SIZE; }
	
	final protected AffineTransform affine = new AffineTransform();
	public AffineTransform getAffine(){	return affine; }
	
	@Override
	public float[] apply( float[] point )
	{
		float[] transformed = new float[ 2 ];
		affine.transform( point, 0, transformed, 0, 1 );
		return transformed;
	}
	
	
	@Override
	public void applyInPlace( float[] point )
	{
		affine.transform( point, 0, point, 0, 1 );
	}
	
	
	@Override
	public float[] applyInverse( float[] point ) throws NoninvertibleModelException
	{
		// the brilliant java.awt.geom.AffineTransform implements transform for float[] but inverseTransform for double[] only...
		double[] double_point = new double[]{ point[ 0 ], point[ 1 ] };
		double[] transformed = new double[ 2 ];
		
		try
		{
			affine.inverseTransform( double_point, 0, transformed, 0, 1 );
		}
		catch ( NoninvertibleTransformException e )
		{
			throw new NoninvertibleModelException( e );
		}
		
		return new float[]{ ( float )transformed[ 0 ], ( float )transformed[ 1 ] };
	}


	@Override
	public void applyInverseInPlace( float[] point ) throws NoninvertibleModelException
	{
		float[] temp_point = applyInverse( point );
		point[ 0 ] = temp_point[ 0 ];
		point[ 1 ] = temp_point[ 1 ];
	}
	
	@Override
	public void fit( Collection< PointMatch > matches ) throws NotEnoughDataPointsException
	{
		if ( matches.size() < MIN_SET_SIZE ) throw new NotEnoughDataPointsException( matches.size() + " data points are not enough to estimate a 2d affine model, at least " + MIN_SET_SIZE + " data points required." );
		
		// center of mass:
		float pcx = 0, pcy = 0;
		float qcx = 0, qcy = 0;
		
		double ws = 0.0;
		//int length = matches.size();
		
		for ( PointMatch m : matches )
		{
			float[] p = m.getP1().getL(); 
			float[] q = m.getP2().getW(); 
			
			float w = m.getWeight();
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
		for ( PointMatch m : matches )
		{
			float[] p = m.getP1().getL();
			float[] q = m.getP2().getW();
			float w = m.getWeight();
			
			float px = p[ 0 ] - pcx, py = p[ 1 ] - pcy;
			float qx = q[ 0 ] - qcx, qy = q[ 1 ] - qcy;
			a11 += w * px * px;
			a12 += w * px * py;
			a22 += w * py * py;
			b11 += w * px * qx;
			b12 += w * px * qy;
			b21 += w * py * qx;
			b22 += w * py * qy;
		}
		
		// invert M
		float det = ( float )( a11 * a22 - a12 * a12 );
		float m11 = ( float )( a22 * b11 - a12 * b21 ) / det;
		float m12 = ( float )( a11 * b21 - a12 * b11 ) / det;
		float m21 = ( float )( a22 * b12 - a12 * b22 ) / det;
		float m22 = ( float )( a11 * b22 - a12 * b12 ) / det;
		
		float tx = qcx - m11 * pcx - m12 * pcy;
		float ty = qcy - m21 * pcx - m22 * pcy;
		
		affine.setTransform( m11, m21, m12, m22, tx, ty );
	}

	
	@Override
	public void shake( Collection< PointMatch > matches, float scale, float[] center )
	{
		// TODO Auto-generated method stub
	}

	@Override
	public String toString()
	{
		return ( "[3,3](" + affine + ") " + cost );
	}
	
	public AffineModel2D clone()
	{
		AffineModel2D trm = new AffineModel2D();
		trm.affine.setTransform( affine );
		trm.cost = cost;
		return trm;
	}

}
