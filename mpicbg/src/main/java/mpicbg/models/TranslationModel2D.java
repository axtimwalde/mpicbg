package mpicbg.models;

import java.awt.geom.AffineTransform;
import java.util.Collection;

/**
 * 2d-translation {@link AbstractModel} to be applied to points in 2d-space.
 * 
 * @version 0.2b
 */
public class TranslationModel2D extends AbstractAffineModel2D< TranslationModel2D >
{
	private static final long serialVersionUID = -6720116001832897767L;

	static final protected int MIN_NUM_MATCHES = 1;
	
	protected float tx = 0, ty = 0;
	
	@Override
	final public int getMinNumMatches(){ return MIN_NUM_MATCHES; }
	
	@Override
	final public AffineTransform createAffine(){ return new AffineTransform( 1, 0, 0, 1, tx, ty ); }
	
	@Override
	final public AffineTransform createInverseAffine(){ return new AffineTransform( 1, 0, 0, 1, -tx, -ty ); }
	
	@Override
	final public float[] apply( final float[] l )
	{
		assert l.length >= 2 : "2d translation transformations can be applied to 2d points only.";
		
		return new float[]{ l[ 0 ] + tx, l[ 1 ] + ty };
	}
	
	@Override
	final public void applyInPlace( final float[] l )
	{
		assert l.length >= 2 : "2d translation transformations can be applied to 2d points only.";
		
		l[ 0 ] += tx;
		l[ 1 ] += ty;
	}
	
	@Override
	final public float[] applyInverse( final float[] l )
	{
		assert l.length >= 2 : "2d translation transformations can be applied to 2d points only.";
		
		return new float[]{ l[ 0 ] - tx, l[ 1 ] - ty };
	}

	@Override
	final public void applyInverseInPlace( final float[] l )
	{
		assert l.length >= 2 : "2d translation transformations can be applied to 2d points only.";
		
		l[ 0 ] -= tx;
		l[ 1 ] -= ty;
	}
	
	@Override
	final public void fit(
			final float[][] p,
			final float[][] q,
			final float[] w )
		throws NotEnoughDataPointsException
	{
		assert
			p.length >= 2 &&
			q.length >= 2 : "2d translations can be applied to 2d points only.";
	
		assert
			p[ 0 ].length == p[ 1 ].length &&
			p[ 0 ].length == q[ 0 ].length &&
			p[ 0 ].length == q[ 1 ].length &&
			p[ 0 ].length == w.length : "Array lengths do not match.";
			
		final int l = p[ 0 ].length;
		
		if ( l < MIN_NUM_MATCHES )
			throw new NotEnoughDataPointsException( l + " data points are not enough to estimate a 2d translation model, at least " + MIN_NUM_MATCHES + " data points required." );
		
		// center of mass:
		double pcx = 0, pcy = 0;
		double qcx = 0, qcy = 0;
		
		double ws = 0.0f;
		
		for ( int i = 0; i < l; ++i )
		{
			final float[] pX = p[ 0 ];
			final float[] pY = p[ 1 ];
			final float[] qX = q[ 0 ];
			final float[] qY = q[ 1 ];
			
			final double ww = w[ i ];
			ws += ww;
			
			pcx += ww * pX[ i ];
			pcy += ww * pY[ i ];
			qcx += ww * qX[ i ];
			qcy += ww * qY[ i ];
		}
		pcx /= ws;
		pcy /= ws;
		qcx /= ws;
		qcy /= ws;

		tx = ( float )( qcx - pcx );
		ty = ( float )( qcy - pcy );
	}
	
	@Override
	final public < P extends PointMatch >void fit( final Collection< P > matches ) throws NotEnoughDataPointsException
	{
		if ( matches.size() < MIN_NUM_MATCHES ) throw new NotEnoughDataPointsException( matches.size() + " data points are not enough to estimate a 2d translation model, at least " + MIN_NUM_MATCHES + " data points required." );
		
		// center of mass:
		double pcx = 0, pcy = 0;
		double qcx = 0, qcy = 0;
		
		double ws = 0.0f;
		
		for ( final P m : matches )
		{
			final float[] p = m.getP1().getL(); 
			final float[] q = m.getP2().getW(); 
			
			final double w = m.getWeight();
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

		tx = ( float )( qcx - pcx );
		ty = ( float )( qcy - pcy );
	}
	
//	@Override
//	final public void shake( final float amount )
//	{
//		tx += rnd.nextGaussian() * amount;
//		ty += rnd.nextGaussian() * amount;
//	}

	@Override
	public TranslationModel2D copy()
	{
		final TranslationModel2D m = new TranslationModel2D();
		m.tx = tx;
		m.ty = ty;
		m.cost = cost;
		return m;
	}
	
	@Override
	final public void set( final TranslationModel2D m )
	{
		tx = m.tx;
		ty = m.ty;
		cost = m.getCost();
	}

	@Override
	final public void preConcatenate( final TranslationModel2D m )
	{
		tx += m.tx;
		ty += m.ty;
	}
	
	@Override
	final public void concatenate( final TranslationModel2D m )
	{
		tx += m.tx;
		ty += m.ty;
	}
	
	/**
	 * Initialize the model such that the respective affine transform is:
	 * 
	 * 1 0 tx
	 * 0 1 ty
	 * 0 0 1
	 * 
	 * @param tx
	 * @param ty
	 */
	final public void set( final float tx, final float ty )
	{
		this.tx = tx;
		this.ty = ty;
	}
	
	/**
	 * TODO Not yet tested
	 */
	//@Override
	@Override
	public TranslationModel2D createInverse()
	{
		final TranslationModel2D ict = new TranslationModel2D();
		
		ict.tx = -tx;
		ict.ty = -ty;
		
		ict.cost = cost;
		
		return ict;
	}
	
	@Override
	public void toArray( final float[] data )
	{
		data[ 0 ] = 1;
		data[ 1 ] = 0;
		data[ 2 ] = 0;
		data[ 3 ] = 1;
		data[ 4 ] = tx;
		data[ 5 ] = ty;
	}

	@Override
	public void toArray( final double[] data )
	{
		data[ 0 ] = 1;
		data[ 1 ] = 0;
		data[ 2 ] = 0;
		data[ 3 ] = 1;
		data[ 4 ] = tx;
		data[ 5 ] = ty;
	}

	@Override
	public void toMatrix( final float[][] data )
	{
		data[ 0 ][ 0 ] = 1;
		data[ 0 ][ 1 ] = 0;
		data[ 0 ][ 2 ] = tx;
		data[ 1 ][ 0 ] = 0;
		data[ 1 ][ 1 ] = 1;
		data[ 1 ][ 1 ] = ty;
	}

	@Override
	public void toMatrix( final double[][] data )
	{
		data[ 0 ][ 0 ] = 1;
		data[ 0 ][ 1 ] = 0;
		data[ 0 ][ 2 ] = tx;
		data[ 1 ][ 0 ] = 0;
		data[ 1 ][ 1 ] = 1;
		data[ 1 ][ 1 ] = ty;
	}
}
