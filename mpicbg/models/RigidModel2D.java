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
	static final protected int MIN_NUM_MATCHES = 2;
	
	protected float cos = 1.0f, sin = 0.0f, tx = 0.0f, ty = 0.0f;
	private float itx = 0.0f, ity = 0.0f;
	
	
	@Override
	final public int getMinNumMatches(){ return MIN_NUM_MATCHES; }
	
	@Override
	final public AffineTransform createAffine(){ return new AffineTransform( cos, sin, -sin, cos, tx, ty ); }
	
	@Override
	final public AffineTransform createInverseAffine(){ return new AffineTransform( cos, -sin, sin, cos, itx, ity ); }
	
	//@Override
	final public float[] apply( final float[] l )
	{
		assert l.length == 2 : "2d rigid transformations can be applied to 2d points only.";
		
		final float[] transformed = l.clone();
		applyInPlace( transformed );
		return transformed;
	}
	
	//@Override
	final public void applyInPlace( final float[] l )
	{
		assert l.length == 2 : "2d rigid transformations can be applied to 2d points only.";
		
		final float l0 = l[ 0 ];
		l[ 0 ] = cos * l0 - sin * l[ 1 ] + tx;
		l[ 1 ] = sin * l0 + cos * l[ 1 ] + ty;
	}
	
	//@Override
	final public float[] applyInverse( final float[] l )
	{
		assert l.length == 2 : "2d rigid transformations can be applied to 2d points only.";
		
		final float[] transformed = l.clone();
		applyInverseInPlace( transformed );
		return transformed;
	}

	//@Override
	final public void applyInverseInPlace( final float[] l )
	{
		assert l.length == 2 : "2d rigid transformations can be applied to 2d points only.";
		
		final float l0 = l[ 0 ];
		l[ 0 ] = cos * l0  + sin * l[ 1 ] + itx;
		l[ 1 ] = -sin * l0 + cos * l[ 1 ] + ity;		
	}

	/**
	 * Closed form weighted least squares solution as described by
	 * \citet{SchaeferAl06} and implemented by Johannes Schindelin.
	 */
	@Override
	final public void fit( final Collection< PointMatch > matches )
		throws NotEnoughDataPointsException
	{
		if ( matches.size() < MIN_NUM_MATCHES ) throw new NotEnoughDataPointsException( matches.size() + " data points are not enough to estimate a 2d rigid model, at least " + MIN_NUM_MATCHES + " data points required." );
		
		float pcx = 0, pcy = 0;
		float qcx = 0, qcy = 0;
		
		double ws = 0.0f;
		
		for ( final PointMatch m : matches )
		{
			final float[] p = m.getP1().getL(); 
			final float[] q = m.getP2().getW();
			
			final float w = m.getWeight();
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

		final float dx = pcx - qcx;
		final float dy = pcy - qcy;
		
		cos = 0;
		sin = 0;
		for ( final PointMatch m : matches )
		{
			final float[] p = m.getP1().getL(); 
			final float[] q = m.getP2().getW();
			final float w = m.getWeight();

			final float x1 = p[ 0 ] - pcx; // x1
			final float y1 = p[ 1 ] - pcy; // x2
			final float x2 = q[ 0 ] - qcx + dx; // y1
			final float y2 = q[ 1 ] - qcy + dy; // y2
			sin += w * ( x1 * y2 - y1 * x2 ); //   x1 * y2 - x2 * y1 // assuming p1 is x1,x2 and p2 is y1,y2
			cos += w * ( x1 * x2 + y1 * y2 ); //   x1 * y1 + x2 * y2
		}
		final float norm = ( float )Math.sqrt( cos * cos + sin * sin );
		cos /= norm;
		sin /= norm;
		
		tx = qcx - cos * pcx + sin * pcy;
		ty = qcy - sin * pcx - cos * pcy;
		
		invert();
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
	public RigidModel2D clone()
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
		final float a = model.cos * cos - model.sin * sin;
		final float b = model.sin * cos + model.cos * sin;
		final float c = model.cos * tx - model.sin * ty + model.tx;
		final float d = model.sin * tx + model.cos * ty + model.ty;
		
		cos = a;
		sin = b;
		tx = c;
		ty = d;
		
		invert();
	}
	
	@Override
	final public void concatenate( final RigidModel2D model )
	{
		final float a = cos * model.cos - sin * model.sin;
		final float b = sin * model.cos + cos * model.sin;
		final float c = cos * model.tx - sin * model.ty + tx;
		final float d = sin * model.tx + cos * model.ty + ty;
		
		cos = a;
		sin = b;
		tx = c;
		ty = d;
		
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
}
