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
 */
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
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
public class SimilarityModel2D extends AbstractAffineModel2D< SimilarityModel2D >
{
	private static final long serialVersionUID = -2002621576568975203L;

	static final protected int MIN_NUM_MATCHES = 2;

	protected double scos = 1.0, ssin = 0.0, tx = 0.0, ty = 0.0;
	private double iscos = 1.0, issin = 0.0, itx = 0.0, ity = 0.0;


	@Override
	final public int getMinNumMatches(){ return MIN_NUM_MATCHES; }

	@Override
	final public AffineTransform createAffine(){ return new AffineTransform( scos, ssin, -ssin, scos, tx, ty ); }

	@Override
	final public AffineTransform createInverseAffine(){ return new AffineTransform( iscos, issin, -issin, iscos, itx, ity ); }

	@Override
	final public double[] apply( final double[] l )
	{
		assert l.length >= 2 : "2d similarity transformations can be applied to 2d points only.";

		final double[] transformed = l.clone();
		applyInPlace( transformed );
		return transformed;
	}

	@Override
	final public void applyInPlace( final double[] l )
	{
		assert l.length >= 2 : "2d similarity transformations can be applied to 2d points only.";

		final double l0 = l[ 0 ];
		l[ 0 ] = scos * l0 - ssin * l[ 1 ] + tx;
		l[ 1 ] = ssin * l0 + scos * l[ 1 ] + ty;
	}

	@Override
	final public double[] applyInverse( final double[] l )
	{
		assert l.length >= 2 : "2d similarity transformations can be applied to 2d points only.";

		final double[] transformed = l.clone();
		applyInverseInPlace( transformed );
		return transformed;
	}

	@Override
	final public void applyInverseInPlace( final double[] l )
	{
		assert l.length >= 2 : "2d similarity transformations can be applied to 2d points only.";

		final double l0 = l[ 0 ];
		l[ 0 ] = iscos * l0  - issin * l[ 1 ] + itx;
		l[ 1 ] = issin * l0 + iscos * l[ 1 ] + ity;
	}

	/**
	 * Closed form weighted least squares solution as described by
	 * \citet{SchaeferAl06} and implemented by Johannes Schindelin.
	 */
	@Override
	final public void fit(
			final double[][] p,
			final double[][] q,
			final double[] w )
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
			final double[] pX = p[ 0 ];
			final double[] pY = p[ 1 ];
			final double[] qX = q[ 0 ];
			final double[] qY = q[ 1 ];

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
			final double[] pX = p[ 0 ];
			final double[] pY = p[ 1 ];
			final double[] qX = q[ 0 ];
			final double[] qY = q[ 1 ];

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

		scos = scosd;
		ssin = ssind;

		tx = qcx - scosd * pcx + ssind * pcy;
		ty = qcy - ssind * pcx - scosd * pcy;

		invert();
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

		scos = scosd;
		ssin = ssind;

		tx = qcx - scosd * pcx + ssind * pcy;
		ty = qcy - ssind * pcx - scosd * pcy;

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
			final double[] p = m.getP1().getL();
			final double[] q = m.getP2().getW();

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
			final double[] p = m.getP1().getL();
			final double[] q = m.getP2().getW();
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

		scos = scosd;
		ssin = ssind;

		tx = qcx - scosd * pcx + ssind * pcy;
		ty = qcy - ssind * pcx - scosd * pcy;

		invert();
	}

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

		iscos = scos / det;
		issin = -ssin / det;

		itx = ( -ssin * ty - scos * tx ) / det;
		ity = ( ssin * tx - scos * ty ) / det;
	}


	@Override
	final public void preConcatenate( final SimilarityModel2D model )
	{
		final double a = model.scos * scos - model.ssin * ssin;
		final double b = model.ssin * scos + model.scos * ssin;
		final double c = model.scos * tx - model.ssin * ty + model.tx;
		final double d = model.ssin * tx + model.scos * ty + model.ty;

		scos = a;
		ssin = b;
		tx = c;
		ty = d;

		invert();
	}

	@Override
	final public void concatenate( final SimilarityModel2D model )
	{
		final double a = scos * model.scos - ssin * model.ssin;
		final double b = ssin * model.scos + scos * model.ssin;
		final double c = scos * model.tx - ssin * model.ty + tx;
		final double d = ssin * model.tx + scos * model.ty + ty;

		scos = a;
		ssin = b;
		tx = c;
		ty = d;

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
	final public void setScaleRotationTranslation( final double s, final double theta, final double tx, final double ty )
	{
		set( s * Math.cos( theta ), s * Math.sin( theta ), tx, ty );
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
	final public void set( final double scos, final double ssin, final double tx, final double ty )
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
