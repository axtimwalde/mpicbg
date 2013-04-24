package mpicbg.models;

import java.awt.geom.AffineTransform;
import java.util.Collection;

/**
 * 2d-similarity transformation models to be applied to points in 2d-space.
 * This model includes the closed form weighted least squares solution as
 * described by \citet{SchaeferAl06} and implemented by Johannes Schindelin.  
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
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.4b
 */
public class SimilarityModel2D extends AbstractAffineModel2D< SimilarityModel2D >
{
	private static final long serialVersionUID = -3951366523410108894L;

	static final protected int MIN_NUM_MATCHES = 2;
	
	protected float scos = 1.0f, ssin = 0.0f, tx = 0.0f, ty = 0.0f;
	private float iscos = 1.0f, issin = 0.0f, itx = 0.0f, ity = 0.0f;
	
	
	@Override
	final public int getMinNumMatches(){ return MIN_NUM_MATCHES; }
	
	@Override
	final public AffineTransform createAffine(){ return new AffineTransform( scos, ssin, -ssin, scos, tx, ty ); }
	
	@Override
	final public AffineTransform createInverseAffine(){ return new AffineTransform( iscos, issin, -issin, iscos, itx, ity ); }
	
	//@Override
	@Override
	final public float[] apply( final float[] l )
	{
		assert l.length >= 2 : "2d similarity transformations can be applied to 2d points only.";
		
		final float[] transformed = l.clone();
		applyInPlace( transformed );
		return transformed;
	}
	
	//@Override
	@Override
	final public void applyInPlace( final float[] l )
	{
		assert l.length >= 2 : "2d similarity transformations can be applied to 2d points only.";
		
		final float l0 = l[ 0 ];
		l[ 0 ] = scos * l0 - ssin * l[ 1 ] + tx;
		l[ 1 ] = ssin * l0 + scos * l[ 1 ] + ty;
	}
	
	//@Override
	@Override
	final public float[] applyInverse( final float[] l )
	{
		assert l.length >= 2 : "2d similarity transformations can be applied to 2d points only.";
		
		final float[] transformed = l.clone();
		applyInverseInPlace( transformed );
		return transformed;
	}

	//@Override
	@Override
	final public void applyInverseInPlace( final float[] l )
	{
		assert l.length >= 2 : "2d similarity transformations can be applied to 2d points only.";
		
		final float l0 = l[ 0 ];
		l[ 0 ] = iscos * l0  - issin * l[ 1 ] + itx;
		l[ 1 ] = issin * l0 + iscos * l[ 1 ] + ity;		
		
//		final AffineTransform a = new AffineTransform( scos, ssin, -ssin, scos, tx, ty );
//		try
//		{
//			a.invert();
//		}
//		catch ( NoninvertibleTransformException ex ){ ex.printStackTrace(); }
//		a.transform( l, 0, l, 0, 1 );
		
	}
	
	/**
	 * Closed form weighted least squares solution as described by
	 * \citet{SchaeferAl06} and implemented by Johannes Schindelin.
	 */
	@Override
	final public void fit(
			final float[][] p,
			final float[][] q,
			final float[] w )
		throws NotEnoughDataPointsException
	{
		assert
			p.length >= 2 &&
			q.length >= 2 : "2d similarity transformations can be applied to 2d points only.";
	
		assert
			p[ 0 ].length == p[ 1 ].length &&
			p[ 0 ].length == q[ 0 ].length &&
			p[ 0 ].length == q[ 1 ].length &&
			p[ 0 ].length == w.length : "Array lengths do not match.";
		
		final int l = p[ 0 ].length;
	
		if ( l < MIN_NUM_MATCHES )
			throw new NotEnoughDataPointsException( l + " data points are not enough to estimate a 2d rigid model, at least " + MIN_NUM_MATCHES + " data points required." );
		
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

		final double dx = pcx - qcx;
		final double dy = pcy - qcy;
		
		double scosd = 0;
		double ssind = 0;
		
		ws = 0.0f;
		
		for ( int i = 0; i < l; ++i )
		{
			final float[] pX = p[ 0 ];
			final float[] pY = p[ 1 ];
			final float[] qX = q[ 0 ];
			final float[] qY = q[ 1 ];
			
			final double ww = w[ i ];
			
			final double x1 = pX[ i ] - pcx; // x1
			final double y1 = pY[ i ] - pcy; // x2
			final double x2 = qX[ i ] - qcx + dx; // y1
			final double y2 = qY[ i ] - qcy + dy; // y2
			ssind += ww * ( x1 * y2 - y1 * x2 ); //   x1 * y2 - x2 * y1 // assuming p1 is x1,x2 and p2 is y1,y2
			scosd += ww * ( x1 * x2 + y1 * y2 ); //   x1 * y1 + x2 * y2
			
			ws += ww * ( x1 * x1 + y1 * y1 );
		}
		scosd /= ws;
		ssind /= ws;
		
		scos = ( float )scosd;
		ssin = ( float )ssind;
		
		tx = ( float )( qcx - scosd * pcx + ssind * pcy );
		ty = ( float )( qcy - ssind * pcx - scosd * pcy );
		
		invert();
	}

	/**
	 * Closed form weighted least squares solution as described by
	 * \citet{SchaeferAl06} and implemented by Johannes Schindelin.
	 */
	@Override
	final public < P extends PointMatch >void fit( final Collection< P > matches )
		throws NotEnoughDataPointsException
	{
		if ( matches.size() < MIN_NUM_MATCHES ) throw new NotEnoughDataPointsException( matches.size() + " data points are not enough to estimate a 2d rigid model, at least " + MIN_NUM_MATCHES + " data points required." );
		
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

		final double dx = pcx - qcx;
		final double dy = pcy - qcy;
		
		double scosd = 0;
		double ssind = 0;
		
		ws = 0.0f;
		
		for ( final P m : matches )
		{
			final float[] p = m.getP1().getL(); 
			final float[] q = m.getP2().getW();
			final double w = m.getWeight();

			final double x1 = p[ 0 ] - pcx; // x1
			final double y1 = p[ 1 ] - pcy; // x2
			final double x2 = q[ 0 ] - qcx + dx; // y1
			final double y2 = q[ 1 ] - qcy + dy; // y2
			ssind += w * ( x1 * y2 - y1 * x2 ); //   x1 * y2 - x2 * y1 // assuming p1 is x1,x2 and p2 is y1,y2
			scosd += w * ( x1 * x2 + y1 * y2 ); //   x1 * y1 + x2 * y2
			
			ws += w * ( x1 * x1 + y1 * y1 );
		}
		scosd /= ws;
		ssind /= ws;
		
		scos = ( float )scosd;
		ssin = ( float )ssind;
		
		tx = ( float )( qcx - scosd * pcx + ssind * pcy );
		ty = ( float )( qcy - ssind * pcx - scosd * pcy );
		
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
	public SimilarityModel2D copy()
	{
		final SimilarityModel2D m = new SimilarityModel2D();
		m.scos = scos;
		m.ssin = ssin;
		m.tx = tx;
		m.ty = ty;
		m.iscos = iscos;
		m.issin = issin;
		m.itx = itx;
		m.ity = ity;
		m.cost = cost;
		return m;
	}
	
	@Override
	final public void set( final SimilarityModel2D m )
	{
		scos = m.scos;
		ssin = m.ssin;
		tx = m.tx;
		ty = m.ty;
		iscos = m.iscos;
		issin = m.issin;
		itx = m.itx;
		ity = m.ity;
		cost = m.cost;
	}
	
	final protected void invert()
	{
		final double det = scos * scos + ssin * ssin;
		
		iscos = ( float )( scos / det );
		issin = ( float )( -ssin / det );
		
		itx = ( float )( ( -ssin * ty - scos * tx ) / det );
		ity = ( float )( ( ssin * tx - scos * ty ) / det );
	}


	@Override
	final public void preConcatenate( final SimilarityModel2D model )
	{
		final double a = model.scos * scos - model.ssin * ssin;
		final double b = model.ssin * scos + model.scos * ssin;
		final double c = model.scos * tx - model.ssin * ty + model.tx;
		final double d = model.ssin * tx + model.scos * ty + model.ty;
		
		scos = ( float )a;
		ssin = ( float )b;
		tx = ( float )c;
		ty = ( float )d;
		
		invert();
	}
	
	@Override
	final public void concatenate( final SimilarityModel2D model )
	{
		final double a = scos * model.scos - ssin * model.ssin;
		final double b = ssin * model.scos + scos * model.ssin;
		final double c = scos * model.tx - ssin * model.ty + tx;
		final double d = ssin * model.tx + scos * model.ty + ty;
		
		scos = ( float )a;
		ssin = ( float )b;
		tx = ( float )c;
		ty = ( float )d;
		
		invert();
	}
	
	/**
	 * Initialize the model such that the respective affine transform is:
	 * 
	 * <pre>
	 * s * cos(&theta;)	-sin(&theta;)		tx
	 * sin(&theta;)		s * cos(&theta;)	ty
	 * 0		0		1
	 * </pre>
	 * 
	 * @param theta &theta; in radians
	 * @param tx
	 * @param ty
	 */
	final public void setScaleRotationTranslation( final float s, final float theta, final float tx, final float ty )
	{
		set( s * ( float )Math.cos( theta ), s * ( float )Math.sin( theta ), tx, ty );
	}
	
	/**
	 * Initialize the model such that the respective affine transform is:
	 * 
	 * <pre>
	 * scos -sin  tx
	 * sin   scos ty
	 * 0     0    1
	 * </pre>
	 * 
	 * @param scos
	 * @param ssin
	 * @param tx
	 * @param ty
	 */
	final public void set( final float scos, final float ssin, final float tx, final float ty )
	{
		this.scos = scos;
		this.ssin = ssin;
		this.tx = tx;
		this.ty = ty;
		invert();
	}
	
	/**
	 * TODO Not yet tested
	 */
	//@Override
	@Override
	public SimilarityModel2D createInverse()
	{
		final SimilarityModel2D ict = new SimilarityModel2D();
		
		ict.scos = iscos;
		ict.ssin = issin;
		ict.tx = itx;
		ict.ty = ity;
		
		ict.iscos = scos;
		ict.issin = ssin;
		ict.itx = tx;
		ict.ity = ty;
		
		ict.cost = cost;
		
		return ict;
	}
	
	@Override
	public void toArray( final float[] data )
	{
		data[ 0 ] = scos;
		data[ 1 ] = ssin;
		data[ 2 ] = -ssin;
		data[ 3 ] = scos;
		data[ 4 ] = tx;
		data[ 5 ] = ty;
	}

	@Override
	public void toArray( final double[] data )
	{
		data[ 0 ] = scos;
		data[ 1 ] = ssin;
		data[ 2 ] = -ssin;
		data[ 3 ] = scos;
		data[ 4 ] = tx;
		data[ 5 ] = ty;
	}

	@Override
	public void toMatrix( final float[][] data )
	{
		data[ 0 ][ 0 ] = scos;
		data[ 0 ][ 1 ] = -ssin;
		data[ 0 ][ 2 ] = tx;
		data[ 1 ][ 0 ] = ssin;
		data[ 1 ][ 1 ] = scos;
		data[ 1 ][ 1 ] = ty;
	}

	@Override
	public void toMatrix( final double[][] data )
	{
		data[ 0 ][ 0 ] = scos;
		data[ 0 ][ 1 ] = -ssin;
		data[ 0 ][ 2 ] = tx;
		data[ 1 ][ 0 ] = ssin;
		data[ 1 ][ 1 ] = scos;
		data[ 1 ][ 1 ] = ty;
	}
}
