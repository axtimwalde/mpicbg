package mpicbg.models;

import java.util.Collection;

/**
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.2b
 */
public class TranslationModel3D extends AbstractAffineModel3D< TranslationModel3D > implements InvertibleBoundable
{
	private static final long serialVersionUID = -2559703154416788897L;
	
	static final protected int MIN_NUM_MATCHES = 1;
	
	@Override
	final public int getMinNumMatches(){ return MIN_NUM_MATCHES; }

	final protected float[] translation = new float[ 3 ];
	final public float[] getTranslation(){ return translation; }
	
	@Override
	final public float[] apply( final float[] point )
	{
		assert point.length >= 3 : "3d translations can be applied to 3d points only.";
		
		return new float[]{
			point[ 0 ] + translation[ 0 ],
			point[ 1 ] + translation[ 1 ],
			point[ 2 ] + translation[ 2 ] };
	}
	
	@Override
	final public void applyInPlace( final float[] point )
	{
		assert point.length >= 3 : "3d translations can be applied to 3d points only.";
		
		point[ 0 ] += translation[ 0 ];
		point[ 1 ] += translation[ 1 ];
		point[ 2 ] += translation[ 2 ];
	}
	
	@Override
	final public float[] applyInverse( final float[] point )
	{
		assert point.length >= 3 : "3d translations can be applied to 3d points only.";
		
		return new float[]{
				point[ 0 ] - translation[ 0 ],
				point[ 1 ] - translation[ 1 ],
				point[ 2 ] - translation[ 2 ] };
	}

	@Override
	final public void applyInverseInPlace( final float[] point )
	{
		assert point.length >= 3 : "3d translations can be applied to 3d points only.";
		
		point[ 0 ] -= translation[ 0 ];
		point[ 1 ] -= translation[ 1 ];
		point[ 2 ] -= translation[ 2 ];
	}

	
	@Override
	final public String toString()
	{
		return ( "[1,3](" + translation[ 0 ] + "," + translation[ 1 ] + "," + translation[ 2 ] + ") " + cost );
	}

	@Override
	final public < P extends PointMatch >void fit( final Collection< P > matches ) throws NotEnoughDataPointsException
	{
		if ( matches.size() < MIN_NUM_MATCHES ) throw new NotEnoughDataPointsException( matches.size() + " data points are not enough to estimate a 3d translation model, at least " + MIN_NUM_MATCHES + " data points required." );
		
		// center of mass:
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

		translation[ 0 ] = qcx - pcx;
		translation[ 1 ] = qcy - pcy;
		translation[ 2 ] = qcz - pcz;
	}
	
//	/**
//	 * TODO Not yet implemented ...
//	 */
//	@Override
//	final public void shake( final float amount )
//	{
//		// TODO If you ever need it, please implement it...
//	}
	
	final public void set( final float tx, final float ty, final float tz )
	{
		translation[ 0 ] = tx;
		translation[ 1 ] = ty;
		translation[ 2 ] = tz;
	}
	
	@Override
	final public void set( final TranslationModel3D m )
	{
		translation[ 0 ] = m.translation[ 0 ];
		translation[ 1 ] = m.translation[ 1 ];
		translation[ 2 ] = m.translation[ 2 ];
		cost = m.getCost();
	}
	
	@Override
	public TranslationModel3D copy()
	{
		final TranslationModel3D m = new TranslationModel3D();
		m.translation[ 0 ] = translation[ 0 ];
		m.translation[ 1 ] = translation[ 1 ];
		m.translation[ 2 ] = translation[ 2 ];
		m.cost = cost;
		return m;
	}
	
	/**
	 * TODO Not yet tested
	 */
	//@Override
	@Override
	public TranslationModel3D createInverse()
	{
		final TranslationModel3D ict = new TranslationModel3D();
		
		ict.translation[ 0 ] = -translation[ 0 ];
		ict.translation[ 1 ] = -translation[ 1 ];
		ict.translation[ 2 ] = -translation[ 2 ];
		
		ict.cost = cost;
		
		return ict;
	}

	@Override
	public void estimateBounds( final float[] min, final float[] max )
	{
		applyInPlace( min );
		applyInPlace( max );
	}

	@Override
	public void estimateInverseBounds( final float[] min, final float[] max ) throws NoninvertibleModelException
	{
		applyInverseInPlace( min );
		applyInverseInPlace( max );		
	}

	@Override
	public void preConcatenate( final TranslationModel3D model )  { concatenate( model ); }

	@Override
	public void concatenate( final TranslationModel3D model ) 
	{
		translation[ 0 ] += model.translation[ 0 ];
		translation[ 1 ] += model.translation[ 1 ];
		translation[ 2 ] += model.translation[ 2 ];
	}

	@Override
	public void toArray( final float[] data ) 
	{
		data[ 0 ] = 1;
		data[ 1 ] = 0;
		data[ 2 ] = 0;
		data[ 3 ] = 0;
		data[ 4 ] = 1;
		data[ 5 ] = 0;
		data[ 6 ] = 0;
		data[ 7 ] = 0;
		data[ 8 ] = 1;
		data[ 9 ] = translation[ 0 ];
		data[ 10 ] = translation[ 1 ];
		data[ 11 ] = translation[ 2 ];
	}

	@Override
	public void toArray( final double[] data ) 
	{
		data[ 0 ] = 1;
		data[ 1 ] = 0;
		data[ 2 ] = 0;
		data[ 3 ] = 0;
		data[ 4 ] = 1;
		data[ 5 ] = 0;
		data[ 6 ] = 0;
		data[ 7 ] = 0;
		data[ 8 ] = 1;
		data[ 9 ] = translation[ 0 ];
		data[ 10 ] = translation[ 1 ];
		data[ 11 ] = translation[ 2 ];
	}
	
	@Override
	public float[] getMatrix( final float[] m )
	{
		final float[] a;
		if ( m == null || m.length != 12 )
			a = new float[ 12 ];
		else
			a = m;
		
		a[ 0 ] = 1;
		a[ 1 ] = 0;
		a[ 2 ] = 0;
		a[ 3 ] = translation[ 0 ];
		
		a[ 4 ] = 0;
		a[ 5 ] = 1;
		a[ 6 ] = 0;
		a[ 7 ] = translation[ 1 ];
		
		a[ 8 ] = 0;
		a[ 9 ] = 0;
		a[ 10 ] = 1;
		a[ 11 ] = translation[ 2 ];
		
		return a;
	}

	@Override
	public void toMatrix( final float[][] data ) 
	{
		data[ 0 ][ 0 ] = 1;
		data[ 0 ][ 1 ] = 0;
		data[ 0 ][ 2 ] = 0;
		data[ 0 ][ 3 ] = translation[ 0 ];
		data[ 1 ][ 0 ] = 0;
		data[ 1 ][ 1 ] = 1;
		data[ 1 ][ 2 ] = 0;
		data[ 1 ][ 3 ] = translation[ 1 ];
		data[ 2 ][ 0 ] = 0;
		data[ 2 ][ 1 ] = 0;
		data[ 2 ][ 2 ] = 1;
		data[ 2 ][ 3 ] = translation[ 2 ];
	}

	@Override
	public void toMatrix( final double[][] data ) 
	{
		data[ 0 ][ 0 ] = 1;
		data[ 0 ][ 1 ] = 0;
		data[ 0 ][ 2 ] = 0;
		data[ 0 ][ 3 ] = translation[ 0 ];
		data[ 1 ][ 0 ] = 0;
		data[ 1 ][ 1 ] = 1;
		data[ 1 ][ 2 ] = 0;
		data[ 1 ][ 3 ] = translation[ 1 ];
		data[ 2 ][ 0 ] = 0;
		data[ 2 ][ 1 ] = 0;
		data[ 2 ][ 2 ] = 1;
		data[ 2 ][ 3 ] = translation[ 2 ];
	}

}
