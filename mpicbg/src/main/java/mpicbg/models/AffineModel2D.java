package mpicbg.models;

import java.awt.geom.AffineTransform;
import java.util.Collection;

/**
 * 2d-affine transformation models to be applied to points in 2d-space.
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
public class AffineModel2D extends AbstractAffineModel2D< AffineModel2D >
{
	private static final long serialVersionUID = -8254250226807170305L;

	static final protected int MIN_NUM_MATCHES = 3;
	
	protected float m00 = 1.0f, m10 = 0.0f, m01 = 0.0f, m11 = 1.0f, m02 = 0.0f, m12 = 0.0f;
	protected float i00 = 1.0f, i10 = 0.0f, i01 = 0.0f, i11 = 1.0f, i02 = 0.0f, i12 = 0.0f;
	
	private boolean isInvertible = true;
	
	@Override
	final public int getMinNumMatches(){ return MIN_NUM_MATCHES; }
	
	@Override
	final public AffineTransform createAffine(){ return new AffineTransform( m00, m10, m01, m11, m02, m12 ); }
	
	@Override
	final public AffineTransform createInverseAffine(){ return new AffineTransform( i00, i10, i01, i11, i02, i12 ); }
	
	//@Override
	@Override
	final public float[] apply( final float[] l )
	{
		assert l.length >= 2 : "2d affine transformations can be applied to 2d points only.";
		
		final float[] transformed = l.clone();
		applyInPlace( transformed );
		return transformed;
	}
	
	//@Override
	@Override
	final public void applyInPlace( final float[] l )
	{
		assert l.length >= 2 : "2d affine transformations can be applied to 2d points only.";
		
		final float l0 = l[ 0 ];
		l[ 0 ] = l0 * m00 + l[ 1 ] * m01 + m02;
		l[ 1 ] = l0 * m10 + l[ 1 ] * m11 + m12;
	}
	
	//@Override
	@Override
	final public float[] applyInverse( final float[] l ) throws NoninvertibleModelException
	{
		assert l.length >= 2 : "2d affine transformations can be applied to 2d points only.";
		
		final float[] transformed = l.clone();
		applyInverseInPlace( transformed );
		return transformed;
	}


	//@Override
	@Override
	final public void applyInverseInPlace( final float[] l ) throws NoninvertibleModelException
	{
		assert l.length >= 2 : "2d affine transformations can be applied to 2d points only.";
		
		if ( isInvertible )
		{
			final float l0 = l[ 0 ];
			l[ 0 ] = l0 * i00 + l[ 1 ] * i01 + i02;
			l[ 1 ] = l0 * i10 + l[ 1 ] * i11 + i12;
		}
		else
			throw new NoninvertibleModelException( "Model not invertible." );
	}
	
	/**
	 * Closed form weighted least squares solution as described by
	 * \citet{SchaeferAl06}.
	 */
	@Override
	final public void fit(
			final float[][] p,
			final float[][] q,
			final float[] w )
		throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		assert
			p.length >= 2 &&
			q.length >= 2 : "2d affine transformations can be applied to 2d points only.";
		
		assert
			p[ 0 ].length == p[ 1 ].length &&
			p[ 0 ].length == q[ 0 ].length &&
			p[ 0 ].length == q[ 1 ].length &&
			p[ 0 ].length == w.length : "Array lengths do not match.";
			
		final int l = p[ 0 ].length;
		
		if ( l < MIN_NUM_MATCHES )
			throw new NotEnoughDataPointsException( l + " data points are not enough to estimate a 2d affine model, at least " + MIN_NUM_MATCHES + " data points required." );
		
		double pcx = 0, pcy = 0;
		double qcx = 0, qcy = 0;
		
		double ws = 0.0;
		
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
		
		double a00, a01, a11;
		double b00, b01, b10, b11;
		a00 = a01 = a11 = b00 = b01 = b10 = b11 = 0;
		for ( int i = 0; i < l; ++i )
		{
			final float[] pX = p[ 0 ];
			final float[] pY = p[ 1 ];
			final float[] qX = q[ 0 ];
			final float[] qY = q[ 1 ];
			
			final double ww = w[ i ];
			
			final double px = pX[ i ] - pcx, py = pY[ i ] - pcy;
			final double qx = qX[ i ] - qcx, qy = qY[ i ] - qcy;
			a00 += ww * px * px;
			a01 += ww * px * py;
			a11 += ww * py * py;
			b00 += ww * px * qx;
			b01 += ww * px * qy;
			b10 += ww * py * qx;
			b11 += ww * py * qy;
		}
		
		final double det = a00 * a11 - a01 * a01;
		
		if ( det == 0 )
			throw new IllDefinedDataPointsException();
		
		m00 = ( float )( ( a11 * b00 - a01 * b10 ) / det );
		m01 = ( float )( ( a00 * b10 - a01 * b00 ) / det );
		m10 = ( float )( ( a11 * b01 - a01 * b11 ) / det );
		m11 = ( float )( ( a00 * b11 - a01 * b01 ) / det );
		
		m02 = ( float )( qcx - m00 * pcx - m01 * pcy );
		m12 = ( float )( qcy - m10 * pcx - m11 * pcy );
		
		invert();
	}
	
	/**
	 * Closed form weighted least squares solution as described by
	 * \citet{SchaeferAl06}.
	 */
	@Override
	final public < P extends PointMatch >void fit( final Collection< P > matches )
		throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		if ( matches.size() < MIN_NUM_MATCHES )
			throw new NotEnoughDataPointsException( matches.size() + " data points are not enough to estimate a 2d affine model, at least " + MIN_NUM_MATCHES + " data points required." );
		
		double pcx = 0, pcy = 0;
		double qcx = 0, qcy = 0;
		
		double ws = 0.0;
		
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
		
		double a00, a01, a11;
		double b00, b01, b10, b11;
		a00 = a01 = a11 = b00 = b01 = b10 = b11 = 0;
		for ( final P m : matches )
		{
			final float[] p = m.getP1().getL();
			final float[] q = m.getP2().getW();
			final double w = m.getWeight();
			
			final double px = p[ 0 ] - pcx, py = p[ 1 ] - pcy;
			final double qx = q[ 0 ] - qcx, qy = q[ 1 ] - qcy;
			a00 += w * px * px;
			a01 += w * px * py;
			a11 += w * py * py;
			b00 += w * px * qx;
			b01 += w * px * qy;
			b10 += w * py * qx;
			b11 += w * py * qy;
		}
		
		final double det = a00 * a11 - a01 * a01;
		
		if ( det == 0 )
			throw new IllDefinedDataPointsException();
		
		m00 = ( float )( ( a11 * b00 - a01 * b10 ) / det );
		m01 = ( float )( ( a00 * b10 - a01 * b00 ) / det );
		m10 = ( float )( ( a11 * b01 - a01 * b11 ) / det );
		m11 = ( float )( ( a00 * b11 - a01 * b01 ) / det );
		
		m02 = ( float )( qcx - m00 * pcx - m01 * pcy );
		m12 = ( float )( qcy - m10 * pcx - m11 * pcy );
		
		invert();
	}

	@Override
	final public void set( final AffineModel2D m )
	{
		m00 = m.m00;
		m01 = m.m01;
		m10 = m.m10;
		m11 = m.m11;

		m02 = m.m02;
		m12 = m.m12;
		
		invert();

		cost = m.getCost();
	}

	@Override
	public AffineModel2D copy()
	{
		final AffineModel2D m = new AffineModel2D();
		m.m00 = m00;
		m.m01 = m01;
		m.m10 = m10;
		m.m11 = m11;

		m.m02 = m02;
		m.m12 = m12;
		
		m.cost = cost;
		
		m.invert();

		return m;
	}
	
	final protected void invert()
	{
		final double det = m00 * m11 - m01 * m10;
		if ( det == 0 )
		{
			isInvertible = false;
			return;
		}
		
		isInvertible = true;
		
		i00 = ( float )( m11 / det );
		i01 = ( float )( -m01 / det );
		i02 = ( float )( ( m01 * m12 - m02 * m11 ) / det );
		
		i10 = ( float )( -m10 / det );
		i11 = ( float )( m00 / det );
		i12 = ( float )( ( m02 * m10 - m00 * m12 ) / det );		
	}
	
	@Override
	final public void preConcatenate( final AffineModel2D model )
	{
		final double a00 = model.m00 * m00 + model.m01 * m10;
		final double a01 = model.m00 * m01 + model.m01 * m11;
		final double a02 = model.m00 * m02 + model.m01 * m12 + model.m02;
		
		final double a10 = model.m10 * m00 + model.m11 * m10;
		final double a11 = model.m10 * m01 + model.m11 * m11;
		final double a12 = model.m10 * m02 + model.m11 * m12 + model.m12;
		
		m00 = ( float )a00;
		m01 = ( float )a01;
		m02 = ( float )a02;
		
		m10 = ( float )a10;
		m11 = ( float )a11;
		m12 = ( float )a12;
		
		invert();
	}
	
	final public void concatenate( final TranslationModel2D model )
	{
		m02 = m00 * model.tx + m01 * model.ty + m02;
		m12 = m10 * model.tx + m11 * model.ty + m12;
		
		invert();
	}
	
	final public void preConcatenate( final TranslationModel2D model )
	{
		m02 += model.tx;
		m12 += model.ty;
		
		invert();
	}
	
	@Override
	final public void concatenate( final AffineModel2D model )
	{
		final double a00 = m00 * model.m00 + m01 * model.m10;
		final double a01 = m00 * model.m01 + m01 * model.m11;
		final double a02 = m00 * model.m02 + m01 * model.m12 + m02;
		
		final double a10 = m10 * model.m00 + m11 * model.m10;
		final double a11 = m10 * model.m01 + m11 * model.m11;
		final double a12 = m10 * model.m02 + m11 * model.m12 + m12;
		
		m00 = ( float )a00;
		m01 = ( float )a01;
		m02 = ( float )a02;
		
		m10 = ( float )a10;
		m11 = ( float )a11;
		m12 = ( float )a12;
		
		invert();
	}
	
	/**
	 * Initialize the model such that the respective affine transform is:
	 * 
	 * m00 m01 m02
	 * m10 m11 m12
	 * 0   0   1
	 * 
	 * @param m00
	 * @param m10
	 * 
	 * @param m01
	 * @param m11
	 * 
	 * @param m02
	 * @param m12
	 */
	final public void set( final float m00, final float m10, final float m01, final float m11, final float m02, final float m12 )
	{
		this.m00 = m00;
		this.m10 = m10;
		
		this.m01 = m01;
		this.m11 = m11;
		
		this.m02 = m02;
		this.m12 = m12;
		
		invert();
	}
	
	/**
	 * Initialize the model with the parameters of an {@link AffineTransform}.
	 * 
	 * @param a
	 */
	final public void set( final AffineTransform a )
	{
		m00 = ( float )a.getScaleX();
		m10 = ( float )a.getShearY();
		
		m01 = ( float )a.getShearX();
		m11 = ( float )a.getScaleY();
		
		m02 = ( float )a.getTranslateX();
		m12 = ( float )a.getTranslateY();
		
		invert();
	}
	
	/**
	 * TODO Not yet tested
	 */
	//@Override
	@Override
	public AffineModel2D createInverse()
	{
		final AffineModel2D ict = new AffineModel2D();
		
		ict.m00 = i00;
		ict.m10 = i10;
		ict.m01 = i01;
		ict.m11 = i11;
		ict.m02 = i02;
		ict.m12 = i12;
		
		ict.i00 = m00;
		ict.i10 = m10;
		ict.i01 = m01;
		ict.i11 = m11;
		ict.i02 = m02;
		ict.i12 = m12;
		
		ict.cost = cost;
		
		return ict;
	}

	@Override
	public void toArray( final float[] data )
	{
		data[ 0 ] = m00;
		data[ 1 ] = m10;
		data[ 2 ] = m01;
		data[ 3 ] = m11;
		data[ 4 ] = m02;
		data[ 5 ] = m12;
	}

	@Override
	public void toArray( final double[] data )
	{
		data[ 0 ] = m00;
		data[ 1 ] = m10;
		data[ 2 ] = m01;
		data[ 3 ] = m11;
		data[ 4 ] = m02;
		data[ 5 ] = m12;
	}

	@Override
	public void toMatrix( final float[][] data )
	{
		data[ 0 ][ 0 ] = m00;
		data[ 0 ][ 1 ] = m01;
		data[ 0 ][ 2 ] = m02;
		data[ 1 ][ 0 ] = m10;
		data[ 1 ][ 1 ] = m11;
		data[ 1 ][ 1 ] = m12;
	}

	@Override
	public void toMatrix( final double[][] data )
	{
		data[ 0 ][ 0 ] = m00;
		data[ 0 ][ 1 ] = m01;
		data[ 0 ][ 2 ] = m02;
		data[ 1 ][ 0 ] = m10;
		data[ 1 ][ 1 ] = m11;
		data[ 1 ][ 1 ] = m12;
	}
}
