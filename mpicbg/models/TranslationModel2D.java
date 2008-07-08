package mpicbg.models;

import java.util.Collection;

public class TranslationModel2D extends AffineModel2D {

	static final protected int MIN_SET_SIZE = 1;
	
	@Override
	final public int getMinSetSize(){ return MIN_SET_SIZE; }

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
	public float[] applyInverse( float[] point )
	{
		// the brilliant java.awt.geom.AffineTransform implements transform for float[] but inverseTransform for double[] only...
		double[] double_point = new double[]{ point[ 0 ], point[ 1 ] };
		double[] transformed = new double[ 2 ];
		try
		{
			affine.inverseTransform( double_point, 0, transformed, 0, 1 );
		}
		catch ( Exception e )
		{
			System.err.println( "Noninvertible transformation." );
		}
		return new float[]{ ( float )transformed[ 0 ], ( float )transformed[ 1 ] };
	}

	@Override
	public void applyInverseInPlace( float[] point )
	{
		float[] temp_point = applyInverse( point );
		point[ 0 ] = temp_point[ 0 ];
		point[ 1 ] = temp_point[ 1 ];
	}

	
	@Override
	public String toString()
	{
		return ( "[3,3](" + affine + ") " + cost );
	}

	final public void fit( Collection< PointMatch > matches ) throws NotEnoughDataPointsException
	{
		if ( matches.size() < MIN_SET_SIZE ) throw new NotEnoughDataPointsException( matches.size() + " data points are not enough to estimate a 2d translation model, at least " + MIN_SET_SIZE + " data points required." );
		
		// center of mass:
		float pcx = 0, pcy = 0;
		float qcx = 0, qcy = 0;
		
		double ws = 0.0;
		
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

		float dx = pcx - qcx;
		float dy = pcy - qcy;
		
		affine.setToIdentity();
		affine.translate( -dx, -dy );
	}
	
	/**
	 * change the model a bit
	 * 
	 * estimates the necessary amount of shaking for each single dimensional
	 * distance in the set of matches
	 * 
	 * @param matches point matches
	 * @param scale gives a multiplicative factor to each dimensional distance (scales the amount of shaking)
	 * @param center local pivot point for centered shakes (e.g. rotation)
	 */
	final public void shake(
			Collection< PointMatch > matches,
			float scale,
			float[] center )
	{
		double xd = 0.0;
		double yd = 0.0;
		
		int num_matches = matches.size();
		if ( num_matches > 0 )
		{
			for ( PointMatch m : matches )
			{
				float[] m_p1 = m.getP1().getW(); 
				float[] m_p2 = m.getP2().getW(); 
				
				xd += Math.abs( m_p1[ 0 ] - m_p2[ 0 ] );;
				yd += Math.abs( m_p1[ 1 ] - m_p2[ 1 ] );;
			}
			xd /= matches.size();
			yd /= matches.size();			
		}
		
		affine.translate(
				rnd.nextGaussian() * ( float )xd * scale,
				rnd.nextGaussian() * ( float )yd );
	}

	public TranslationModel2D clone()
	{
		TranslationModel2D tm = new TranslationModel2D();
		tm.affine.setTransform( affine );
		tm.cost = cost;
		return tm;
	}
	
	public RigidModel2D toTRModel2D()
	{
		RigidModel2D trm = new RigidModel2D();
		trm.getAffine().setTransform( affine );
		trm.cost = cost;
		return trm;
	}
}
