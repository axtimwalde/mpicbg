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

import java.util.Collection;

/**
 *
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
public class AffineModel1D extends AbstractAffineModel1D< AffineModel1D > implements InvertibleBoundable
{
	private static final long serialVersionUID = -6691788501310913119L;

	static final protected int MIN_NUM_MATCHES = 2;

	protected double
		m00 = 1.0, m01 = 0.0;

	protected double
		i00 = 1.0, i01 = 0.0;

	@Override
	public double[] getMatrix( final double[] m )
	{
		final double[] a;
		if ( m == null || m.length != 2 )
			a = new double[ 2 ];
		else
			a = m;

		a[ 0 ] = m00;
		a[ 1 ] = m01;

		return a;
	}

	protected boolean isInvertible = true;

	@Override
	final public int getMinNumMatches(){ return MIN_NUM_MATCHES; }

	@Override
	final public double[] apply( final double[] l )
	{
		final double[] transformed = l.clone();
		applyInPlace( transformed );
		return transformed;
	}

	@Override
	final public void applyInPlace( final double[] l )
	{
		assert l.length >= 1 : "1d affine transformations can be applied to 1d points only.";
		l[ 0 ] = l[ 0 ] * m00 + m01;
	}

	@Override
	final public double[] applyInverse( final double[] l ) throws NoninvertibleModelException
	{
		final double[] transformed = l.clone();
		applyInverseInPlace( transformed );
		return transformed;
	}


	@Override
	final public void applyInverseInPlace( final double[] l ) throws NoninvertibleModelException
	{
		assert l.length >= 1 : "1d affine transformations can be applied to 1d points only.";

		if ( isInvertible )
			l[ 0 ] = l[ 0 ] * i00 + i01;
		else
			throw new NoninvertibleModelException( "Model not invertible." );
	}

	/**
	 * Closed form weighted least squares solution as described by
	 * \citet{SchaeferAl06}.
	 */
	@Override
	final public void fit(
			final double[][] p,
			final double[][] q,
			final double[] w )
		throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		assert
		p.length >= 1 &&
		q.length >= 1 : "1d affine transformations can be applied to 1d points only.";

		assert
			p[ 0 ].length == p[ 1 ].length &&
			p[ 0 ].length == q[ 0 ].length &&
			p[ 0 ].length == q[ 1 ].length &&
			p[ 0 ].length == w.length : "Array lengths do not match.";

		final int l = p[ 0 ].length;

		if ( l < MIN_NUM_MATCHES )
			throw new NotEnoughDataPointsException( l + " data points are not enough to estimate a 2d affine model, at least " + MIN_NUM_MATCHES + " data points required." );

		double pcx = 0;
		double qcx = 0;

		double ws = 0.0;

		final double[] pX = p[ 0 ];
		final double[] qX = q[ 0 ];
		for ( int i = 0; i < l; ++i )
		{
			final double ww = w[ i ];
			ws += ww;
			pcx += ww * pX[ i ];
			qcx += ww * qX[ i ];
		}
		pcx /= ws;
		qcx /= ws;

		double a = 0;
		double b = 0;
		for ( int i = 0; i < l; ++i )
		{
			final double px = pX[ i ] - pcx;
			final double qx = qX[ i ] - qcx;
			final double wwpx = w[ i ] * px;
			a += wwpx * px;
			b += wwpx * qx;
		}

		if ( a == 0 )
			throw new IllDefinedDataPointsException();

		m00 = b / a;
		m01 = qcx - m00 * pcx;

		invert();
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
		p.length >= 1 &&
		q.length >= 1 : "1d affine transformations can be applied to 1d points only.";

		assert
			p[ 0 ].length == p[ 1 ].length &&
			p[ 0 ].length == q[ 0 ].length &&
			p[ 0 ].length == q[ 1 ].length &&
			p[ 0 ].length == w.length : "Array lengths do not match.";

		final int l = p[ 0 ].length;

		if ( l < MIN_NUM_MATCHES )
			throw new NotEnoughDataPointsException( l + " data points are not enough to estimate a 2d affine model, at least " + MIN_NUM_MATCHES + " data points required." );

		double pcx = 0;
		double qcx = 0;

		double ws = 0.0;

		final float[] pX = p[ 0 ];
		final float[] qX = q[ 0 ];
		for ( int i = 0; i < l; ++i )
		{
			final double ww = w[ i ];
			ws += ww;
			pcx += ww * pX[ i ];
			qcx += ww * qX[ i ];
		}
		pcx /= ws;
		qcx /= ws;

		double a = 0;
		double b = 0;
		for ( int i = 0; i < l; ++i )
		{
			final double px = pX[ i ] - pcx;
			final double qx = qX[ i ] - qcx;
			final double wwpx = w[ i ] * px;
			a += wwpx * px;
			b += wwpx * qx;
		}

		if ( a == 0 )
			throw new IllDefinedDataPointsException();

		m00 = b / a;
		m01 = qcx - m00 * pcx;

		invert();
	}

	/**
	 * Closed form weighted least squares solution as described by
	 * \citet{SchaeferAl06}.
	 *
	 * TODO
	 */
	@Override
	final public < P extends PointMatch >void fit( final Collection< P > matches )
		throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		if ( matches.size() < MIN_NUM_MATCHES )
			throw new NotEnoughDataPointsException( matches.size() + " data points are not enough to estimate a 2d affine model, at least " + MIN_NUM_MATCHES + " data points required." );

		double pcx = 0;
		double qcx = 0;

		double ws = 0.0;

		for ( final P m : matches )
		{
			final double[] p = m.getP1().getL();
			final double[] q = m.getP2().getW();

			final double w = m.getWeight();
			ws += w;

			pcx += w * p[ 0 ];
			qcx += w * q[ 0 ];
		}
		pcx /= ws;
		qcx /= ws;

		double a = 0;
		double b = 0;
		for ( final P m : matches )
		{
			final double[] p = m.getP1().getL();
			final double[] q = m.getP2().getW();

			final double px = p[ 0 ] - pcx;
			final double qx = q[ 0 ] - qcx;
			final double wwpx = m.getWeight() * px;
			a += wwpx * px;
			b += wwpx * qx;
		}

		if ( a == 0 )
			throw new IllDefinedDataPointsException();

		m00 = b / a;
		m01 = qcx - m00 * pcx;

		invert();
	}

	@Override
	final public void set( final AffineModel1D m )
	{
		m00 = m.m00;
		m01 = m.m01;

		cost = m.cost;

		invert();
	}

	@Override
	public AffineModel1D copy()
	{
		final AffineModel1D m = new AffineModel1D();
		m.set( this );
		return m;
	}

	protected void invert()
	{
		if ( m00 == 0 )
		{
			isInvertible = false;
			return;
		}

		isInvertible = true;

		i00 = 1f / m00;
		i01 = -m01 / m00;
	}

	@Override
	final public void preConcatenate( final AffineModel1D model )
	{
		final double a00 = model.m00 * m00;
		final double a01 = model.m00 * m01 + model.m01;

		m00 = a00;
		m01 = a01;

		invert();
	}

	@Override
	final public void concatenate( final AffineModel1D model )
	{
		final double a00 = m00 * model.m00;
		final double a01 = m00 * model.m01 + m01;

		m00 = a00;
		m01 = a01;

		invert();
	}

	/**
	 * Initialize the model such that the respective affine transform is:
	 *
	 * <pre>
	 * m00 m01
	 *   0   1
	 * </pre>
	 *
	 * @param m00
	 * @param m01
	 */
	final public void set(
			final double m00, final double m01 )
	{
		this.m00 = m00;
		this.m01 = m01;

		invert();
	}

	@Override
	final public String toString()
	{
		return
			"1d-affine: (" + m00 + ", " + m01 + ")";
	}

	/**
	 * TODO Not yet tested
	 */
	@Override
	public AffineModel1D createInverse()
	{
		final AffineModel1D ict = new AffineModel1D();

		ict.m00 = i00;
		ict.m01 = i01;

		ict.i00 = m00;
		ict.i01 = m01;

		ict.cost = cost;

		ict.isInvertible = isInvertible;

		return ict;
	}

	@Override
	public void toArray( final double[] data )
	{
		data[ 0 ] = m00;
		data[ 1 ] = m01;
	}

	@Override
	public void toMatrix( final double[][] data )
	{
		data[ 0 ][ 0 ] = m00;
		data[ 0 ][ 1 ] = m01;
	}
}
