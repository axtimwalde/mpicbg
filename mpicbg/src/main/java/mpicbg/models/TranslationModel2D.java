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
 * 2d-translation {@link AbstractModel} to be applied to points in 2d-space.
 *
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
public class TranslationModel2D extends AbstractAffineModel2D< TranslationModel2D >
{
	private static final long serialVersionUID = -6412303652902075611L;

	static final protected int MIN_NUM_MATCHES = 1;

	protected double tx = 0, ty = 0;

	@Override
	final public int getMinNumMatches(){ return MIN_NUM_MATCHES; }

	@Override
	final public AffineTransform createAffine(){ return new AffineTransform( 1, 0, 0, 1, tx, ty ); }

	@Override
	final public AffineTransform createInverseAffine(){ return new AffineTransform( 1, 0, 0, 1, -tx, -ty ); }

	@Override
	final public double[] apply( final double[] l )
	{
		assert l.length >= 2 : "2d translation transformations can be applied to 2d points only.";

		return new double[]{ l[ 0 ] + tx, l[ 1 ] + ty };
	}

	@Override
	final public void applyInPlace( final double[] l )
	{
		assert l.length >= 2 : "2d translation transformations can be applied to 2d points only.";

		l[ 0 ] += tx;
		l[ 1 ] += ty;
	}

	@Override
	final public double[] applyInverse( final double[] l )
	{
		assert l.length >= 2 : "2d translation transformations can be applied to 2d points only.";

		return new double[]{ l[ 0 ] - tx, l[ 1 ] - ty };
	}

	@Override
	final public void applyInverseInPlace( final double[] l )
	{
		assert l.length >= 2 : "2d translation transformations can be applied to 2d points only.";

		l[ 0 ] -= tx;
		l[ 1 ] -= ty;
	}

	@Override
	final public void fit(
			final double[][] p,
			final double[][] q,
			final double[] w )
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

		tx = qcx - pcx;
		ty = qcy - pcy;
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

		tx = qcx - pcx;
		ty = qcy - pcy;
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

		tx = qcx - pcx;
		ty = qcy - pcy;
	}

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
	final public void set( final double tx, final double ty )
	{
		this.tx = tx;
		this.ty = ty;
	}

	/**
	 * TODO Not yet tested
	 */
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
