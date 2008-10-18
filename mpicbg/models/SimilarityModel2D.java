/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 *
 */
package mpicbg.models;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
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
 * @version 0.4b
 * 
 */
public class SimilarityModel2D extends AbstractAffineModel2D< SimilarityModel2D >
{
	static final protected int MIN_NUM_MATCHES = 2;
	
	private float scos = 1.0f, ssin = 0.0f, tx = 0.0f, ty = 0.0f;
	private float iscos = 1.0f, issin = 0.0f, itx = 0.0f, ity = 0.0f;
	
	
	@Override
	final public int getMinNumMatches(){ return MIN_NUM_MATCHES; }
	
	@Override
	final public AffineTransform createAffine(){ return new AffineTransform( scos, -ssin, ssin, scos, tx, ty ); }
	
	@Override
	final public AffineTransform createInverseAffine(){ return new AffineTransform( iscos, ssin, -ssin, iscos, itx, ity ); }
	
	//@Override
	final public float[] apply( final float[] l )
	{
		assert l.length == 2 : "2d similarity transformations can be applied to 2d points only.";
		
		final float[] transformed = new float[ 2 ];
		applyInPlace( transformed );
		return transformed;
	}
	
	//@Override
	final public void applyInPlace( final float[] l )
	{
		assert l.length == 2 : "2d similarity transformations can be applied to 2d points only.";
		
		final float l0 = l[ 0 ];
		l[ 0 ] = scos * l0 - ssin * l[ 1 ] + tx;
		l[ 1 ] = ssin * l0 + scos * l[ 1 ] + ty;
	}
	
	//@Override
	final public float[] applyInverse( final float[] l )
	{
		assert l.length == 2 : "2d similarity transformations can be applied to 2d points only.";
		
		final float[] transformed = new float[ 2 ];
		applyInverseInPlace( transformed );
		return transformed;
	}

	//@Override
	final public void applyInverseInPlace( final float[] l )
	{
		assert l.length == 2 : "2d similarity transformations can be applied to 2d points only.";
		
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
		
		scos = 0;
		ssin = 0;
		
		ws = 0.0f;
		
		for ( final PointMatch m : matches )
		{
			final float[] p = m.getP1().getL(); 
			final float[] q = m.getP2().getW();
			final float w = m.getWeight();

			final float x1 = p[ 0 ] - pcx; // x1
			final float y1 = p[ 1 ] - pcy; // x2
			final float x2 = q[ 0 ] - qcx + dx; // y1
			final float y2 = q[ 1 ] - qcy + dy; // y2
			ssin += w * ( x1 * y2 - y1 * x2 ); //   x1 * y2 - x2 * y1 // assuming p1 is x1,x2 and p2 is y1,y2
			scos += w * ( x1 * x2 + y1 * y2 ); //   x1 * y1 + x2 * y2
			
			ws += w * ( x1 * x1 + y1 * y1 );
		}
		scos /= ws;
		ssin /= ws;
		
		tx = qcx - scos * pcx + ssin * pcy;
		ty = qcy - ssin * pcx - scos * pcy;
		
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
	final public SimilarityModel2D clone()
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
	
	final private void invert()
	{
		final float det = scos * scos + ssin * ssin;
		
		iscos = scos / det;
		issin = -ssin / det;
		
		itx = ( -ssin * ty - scos * tx ) / det;
		ity = ( ssin * tx - scos * ty ) / det;
	}


	@Override
	final public void preConcatenate( final SimilarityModel2D model )
	{
		final float a = model.scos * scos - model.ssin * ssin;
		final float b = model.ssin * scos + model.scos * ssin;
		final float c = model.scos * tx - model.ssin * ty + model.tx;
		final float d = model.ssin * tx + model.scos * ty + model.ty;
		
		scos = a;
		ssin = b;
		tx = c;
		ty = d;
		
		invert();
	}
	
	@Override
	final public void concatenate( final SimilarityModel2D model )
	{
		final float a = scos * model.scos - ssin * model.ssin;
		final float b = ssin * model.scos + scos * model.ssin;
		final float c = scos * model.tx - ssin * model.ty + tx;
		final float d = ssin * model.tx + scos * model.ty + ty;
		
		scos = a;
		ssin = b;
		tx = c;
		ty = d;
		
		invert();
	}
	
//	/**
//	 * Initialize the model such that the respective affine transform is:
//	 * 
//	 * s * cos(&theta;) -sin(&theta;) tx
//	 * sin(&theta;)      s * cos(&theta;) ty
//	 * 0           0          1
//	 * 
//	 * @param theta &theta;
//	 * @param tx
//	 * @param ty
//	 */
//	final public void set( final float s, final float theta, final float tx, final float ty )
//	{
//		set( s * ( float )Math.cos( theta ), ( float )Math.sin( theta ), tx, ty );
//	}
	
	/**
	 * Initialize the model such that the respective affine transform is:
	 * 
	 * scos -sin  tx
	 * sin   scos ty
	 * 0     0    1
	 * 
	 * @param scos
	 * @param sin
	 * @param tx
	 * @param ty
	 */
	final public void set( final float scos, final float sin, final float tx, final float ty )
	{
		this.scos = scos;
		this.ssin = sin;
		this.tx = tx;
		this.ty = ty;
		invert();
	}
}
