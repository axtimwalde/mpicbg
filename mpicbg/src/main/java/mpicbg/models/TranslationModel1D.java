package mpicbg.models;

import java.util.Collection;

/**
 * 1d-translation {@link AbstractModel} to be applied to points in 1d-space.
 * 
 * @version 0.6
 */
public class TranslationModel1D extends AbstractAffineModel1D< TranslationModel1D >
{
	private static final long serialVersionUID = 7109240016066264945L;

	static final protected int MIN_NUM_MATCHES = 1;
	
	protected float t = 0;
	
	final public float getTranslation()
	{
		return t;
	}
	
	@Override
	final public int getMinNumMatches(){ return MIN_NUM_MATCHES; }
	
	@Override
	final public float[] apply( final float[] l )
	{
		assert l.length >= 1 : "1d translation transformations can be applied to 1d points only.";
		
		return new float[]{ l[ 0 ] + t };
	}
	
	@Override
	final public void applyInPlace( final float[] l )
	{
		assert l.length >= 1 : "1d translation transformations can be applied to 1d points only.";
		
		l[ 0 ] += t;
	}
	
	@Override
	final public float[] applyInverse( final float[] l )
	{
		assert l.length >= 1 : "1d translation transformations can be applied to 1d points only.";
		
		return new float[]{ l[ 0 ] - t };
	}

	@Override
	final public void applyInverseInPlace( final float[] l )
	{
		assert l.length >= 1 : "1d translation transformations can be applied to 1d points only.";
		
		l[ 0 ] -= t;
	}
	
	@Override
	final public void fit(
			final float[][] p,
			final float[][] q,
			final float[] w )
		throws NotEnoughDataPointsException
	{
		assert
			p.length >= 1 &&
			q.length >= 1 : "1d translations can be applied to 1d points only.";
	
		assert
			p[ 0 ].length == q[ 0 ].length &&
			p[ 0 ].length == w.length : "Array lengths do not match.";
			
		final int l = p[ 0 ].length;
		
		if ( l < MIN_NUM_MATCHES )
			throw new NotEnoughDataPointsException( l + " data points are not enough to estimate a 1d translation model, at least " + MIN_NUM_MATCHES + " data points required." );
		
		// center of mass:
		double pcx = 0;
		double qcx = 0;
		
		double ws = 0.0f;
		
		for ( int i = 0; i < l; ++i )
		{
			final float[] pX = p[ 0 ];
			final float[] qX = q[ 0 ];
			
			final double ww = w[ i ];
			ws += ww;
			
			pcx += ww * pX[ i ];
			qcx += ww * qX[ i ];
		}
		pcx /= ws;
		qcx /= ws;

		t = ( float )( qcx - pcx );
	}
	
	@Override
	final public < P extends PointMatch >void fit( final Collection< P > matches ) throws NotEnoughDataPointsException
	{
		if ( matches.size() < MIN_NUM_MATCHES ) throw new NotEnoughDataPointsException( matches.size() + " data points are not enough to estimate a 1d translation model, at least " + MIN_NUM_MATCHES + " data points required." );
		
		// center of mass:
		double pcx = 0;
		double qcx = 0;
		
		double ws = 0.0f;
		
		for ( final P m : matches )
		{
			final float[] p = m.getP1().getL(); 
			final float[] q = m.getP2().getW(); 
			
			final double w = m.getWeight();
			ws += w;
			
			pcx += w * p[ 0 ];
			qcx += w * q[ 0 ];
		}
		pcx /= ws;
		qcx /= ws;

		t = ( float )( qcx - pcx );
	}
	
//	@Override
//	final public void shake( final float amount )
//	{
//		tx += rnd.nextGaussian() * amount;
//		ty += rnd.nextGaussian() * amount;
//	}

	@Override
	public TranslationModel1D copy()
	{
		final TranslationModel1D m = new TranslationModel1D();
		m.t = t;
		m.cost = cost;
		return m;
	}
	
	@Override
	final public void set( final TranslationModel1D m )
	{
		t = m.t;
		cost = m.getCost();
	}

	@Override
	final public void preConcatenate( final TranslationModel1D m )
	{
		t += m.t;
	}
	
	@Override
	final public void concatenate( final TranslationModel1D m )
	{
		t += m.t;
	}
	
	/**
	 * Initialize the model with an offset
	 * 
	 * @param t
	 */
	final public void set( final float t )
	{
		this.t = t;
	}
	
	/**
	 * TODO Not yet tested
	 */
	//@Override
	@Override
	public TranslationModel1D createInverse()
	{
		final TranslationModel1D ict = new TranslationModel1D();
		
		ict.t = -t;
		
		ict.cost = cost;
		
		return ict;
	}
	
	@Override
	public void toArray( final float[] data )
	{
		data[ 0 ] = 1;
		data[ 1 ] = t;
	}

	@Override
	public void toArray( final double[] data )
	{
		data[ 0 ] = 1;
		data[ 1 ] = t;
	}

	@Override
	public void toMatrix( final float[][] data )
	{
		data[ 0 ][ 0 ] = 1;
		data[ 0 ][ 1 ] = t;
	}

	@Override
	public void toMatrix( final double[][] data )
	{
		data[ 0 ][ 0 ] = 1;
		data[ 0 ][ 1 ] = t;
	}
	
	@Override
	public float[] getMatrix( final float[] m )
	{
		final float[] a;
		if ( m == null || m.length != 2 )
			a = new float[ 2 ];
		else
			a = m;

		a[ 0 ] = 1;
		a[ 1 ] = t;

		return a;
	}
}
