package mpicbg.models;

import java.awt.geom.AffineTransform;
import java.util.Collection;

/**
 * 2d-rigid transformation models to be applied to points in 2d-space.
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
 * @version 0.1b
 * 
 */
public class RigidModel2D extends AbstractAffineModel2D< RigidModel2D >
{
	private static final long serialVersionUID = 1897446303972523930L;

	static final protected int MIN_NUM_MATCHES = 2;
	
	protected float cos = 1.0f, sin = 0.0f, tx = 0.0f, ty = 0.0f;
	protected float itx = 0.0f, ity = 0.0f;
	
	
	@Override
	final public int getMinNumMatches(){ return MIN_NUM_MATCHES; }
	
	@Override
	final public AffineTransform createAffine(){ return new AffineTransform( cos, sin, -sin, cos, tx, ty ); }
	
	@Override
	final public AffineTransform createInverseAffine(){ return new AffineTransform( cos, -sin, sin, cos, itx, ity ); }
	
	//@Override
	@Override
	final public float[] apply( final float[] l )
	{
		assert l.length >= 2 : "2d rigid transformations can be applied to 2d points only.";
		
		final float[] transformed = l.clone();
		applyInPlace( transformed );
		return transformed;
	}
	
	//@Override
	@Override
	final public void applyInPlace( final float[] l )
	{
		assert l.length >= 2 : "2d rigid transformations can be applied to 2d points only.";
		
		final float l0 = l[ 0 ];
		l[ 0 ] = cos * l0 - sin * l[ 1 ] + tx;
		l[ 1 ] = sin * l0 + cos * l[ 1 ] + ty;
	}
	
	//@Override
	@Override
	final public float[] applyInverse( final float[] l )
	{
		assert l.length >= 2 : "2d rigid transformations can be applied to 2d points only.";
		
		final float[] transformed = l.clone();
		applyInverseInPlace( transformed );
		return transformed;
	}

	//@Override
	@Override
	final public void applyInverseInPlace( final float[] l )
	{
		assert l.length >= 2 : "2d rigid transformations can be applied to 2d points only.";
		
		final float l0 = l[ 0 ];
		l[ 0 ] = cos * l0  + sin * l[ 1 ] + itx;
		l[ 1 ] = -sin * l0 + cos * l[ 1 ] + ity;		
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
			q.length >= 2 : "2d rigid transformations can be applied to 2d points only.";
	
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
		
		double cosd = 0;
		double sind = 0;
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
			sind += ww * ( x1 * y2 - y1 * x2 ); //   x1 * y2 - x2 * y1 // assuming p1 is x1,x2 and p2 is y1,y2
			cosd += ww * ( x1 * x2 + y1 * y2 ); //   x1 * y1 + x2 * y2
		}
		final double norm = Math.sqrt( cosd * cosd + sind * sind );
		cosd /= norm;
		sind /= norm;
		
		cos = ( float )cosd;
		sin = ( float )sind;
		
		tx = ( float )( qcx - cosd * pcx + sind * pcy );
		ty = ( float )( qcy - sind * pcx - cosd * pcy );
		
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
		
		double cosd = 0;
		double sind = 0;
		for ( final P m : matches )
		{
			final float[] p = m.getP1().getL(); 
			final float[] q = m.getP2().getW();
			final double w = m.getWeight();

			final double x1 = p[ 0 ] - pcx; // x1
			final double y1 = p[ 1 ] - pcy; // x2
			final double x2 = q[ 0 ] - qcx + dx; // y1
			final double y2 = q[ 1 ] - qcy + dy; // y2
			sind += w * ( x1 * y2 - y1 * x2 ); //   x1 * y2 - x2 * y1 // assuming p1 is x1,x2 and p2 is y1,y2
			cosd += w * ( x1 * x2 + y1 * y2 ); //   x1 * y1 + x2 * y2
		}
		final double norm = Math.sqrt( cosd * cosd + sind * sind );
		cosd /= norm;
		sind /= norm;
		
		cos = ( float )cosd;
		sin = ( float )sind;
		
		tx = ( float )( qcx - cosd * pcx + sind * pcy );
		ty = ( float )( qcy - sind * pcx - cosd * pcy );
		
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
	public RigidModel2D copy()
	{
		final RigidModel2D m = new RigidModel2D();
		m.cos = cos;
		m.sin = sin;
		m.tx = tx;
		m.ty = ty;
		m.itx = itx;
		m.ity = ity;
		m.cost = cost;
		return m;
	}
	
	@Override
	final public void set( final RigidModel2D m )
	{
		cos = m.cos;
		sin = m.sin;
		tx = m.tx;
		ty = m.ty;
		itx = m.itx;
		ity = m.ity;
		cost = m.cost;
	}
	
	final private void invert()
	{
		itx = -sin * ty - cos * tx;
		ity = sin * tx - cos * ty;
	}


	@Override
	final public void preConcatenate( final RigidModel2D model )
	{
		final double a = model.cos * cos - model.sin * sin;
		final double b = model.sin * cos + model.cos * sin;
		final double c = model.cos * tx - model.sin * ty + model.tx;
		final double d = model.sin * tx + model.cos * ty + model.ty;
		
		cos = ( float )a;
		sin = ( float )b;
		tx = ( float )c;
		ty = ( float )d;
		
		invert();
	}
	
	@Override
	final public void concatenate( final RigidModel2D model )
	{
		final double a = cos * model.cos - sin * model.sin;
		final double b = sin * model.cos + cos * model.sin;
		final double c = cos * model.tx - sin * model.ty + tx;
		final double d = sin * model.tx + cos * model.ty + ty;
		
		cos = ( float )a;
		sin = ( float )b;
		tx = ( float )c;
		ty = ( float )d;
		
		invert();
	}
	
	/**
	 * Initialize the model such that the respective affine transform is:
	 * 
	 * cos(&theta;) -sin(&theta;) tx
	 * sin(&theta;)  cos(&theta;) ty
	 * 0       0      1
	 * 
	 * @param theta &theta;
	 * @param tx
	 * @param ty
	 */
	final public void set( final float theta, final float tx, final float ty )
	{
		set( ( float )Math.cos( theta ), ( float )Math.sin( theta ), tx, ty );
	}
	
	/**
	 * Initialize the model such that the respective affine transform is:
	 * 
	 * cos -sin tx
	 * sin  cos ty
	 * 0    0   1
	 * 
	 * @param cos
	 * @param sin
	 * @param tx
	 * @param ty
	 */
	final public void set( final float cos, final float sin, final float tx, final float ty )
	{
		this.cos = cos;
		this.sin = sin;
		this.tx = tx;
		this.ty = ty;
		
		invert();
	}
	
	/**
	 * TODO Not yet tested
	 */
	//@Override
	@Override
	public RigidModel2D createInverse()
	{
		final RigidModel2D ict = new RigidModel2D();
		
		ict.cos = cos;
		ict.sin = -sin;
		ict.tx = itx;
		ict.ty = ity;
		ict.itx = tx;
		ict.ity = ty;
		ict.cost = cost;
		
		return ict;
	}

	@Override
	public void toArray( final float[] data )
	{
		data[ 0 ] = cos;
		data[ 1 ] = sin;
		data[ 2 ] = -sin;
		data[ 3 ] = cos;
		data[ 4 ] = tx;
		data[ 5 ] = ty;
	}

	@Override
	public void toArray( final double[] data )
	{
		data[ 0 ] = cos;
		data[ 1 ] = sin;
		data[ 2 ] = -sin;
		data[ 3 ] = cos;
		data[ 4 ] = tx;
		data[ 5 ] = ty;
	}

	@Override
	public void toMatrix( final float[][] data )
	{
		data[ 0 ][ 0 ] = cos;
		data[ 0 ][ 1 ] = -sin;
		data[ 0 ][ 2 ] = tx;
		data[ 1 ][ 0 ] = sin;
		data[ 1 ][ 1 ] = cos;
		data[ 1 ][ 1 ] = ty;
	}

	@Override
	public void toMatrix( final double[][] data )
	{
		data[ 0 ][ 0 ] = cos;
		data[ 0 ][ 1 ] = -sin;
		data[ 0 ][ 2 ] = tx;
		data[ 1 ][ 0 ] = sin;
		data[ 1 ][ 1 ] = cos;
		data[ 1 ][ 1 ] = ty;
	}
}
