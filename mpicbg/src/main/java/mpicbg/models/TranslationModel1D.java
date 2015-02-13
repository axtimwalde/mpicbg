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
 * 1d-translation {@link AbstractModel} to be applied to points in 1d-space.
 *
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
public class TranslationModel1D extends AbstractAffineModel1D< TranslationModel1D >
{
	private static final long serialVersionUID = 846402044582557842L;

	static final protected int MIN_NUM_MATCHES = 1;

	protected double t = 0;

	final public double getTranslation()
	{
		return t;
	}

	@Override
	final public int getMinNumMatches(){ return MIN_NUM_MATCHES; }

	@Override
	final public double[] apply( final double[] l )
	{
		assert l.length >= 1 : "1d translation transformations can be applied to 1d points only.";

		return new double[]{ l[ 0 ] + t };
	}

	@Override
	final public void applyInPlace( final double[] l )
	{
		assert l.length >= 1 : "1d translation transformations can be applied to 1d points only.";

		l[ 0 ] += t;
	}

	@Override
	final public double[] applyInverse( final double[] l )
	{
		assert l.length >= 1 : "1d translation transformations can be applied to 1d points only.";

		return new double[]{ l[ 0 ] - t };
	}

	@Override
	final public void applyInverseInPlace( final double[] l )
	{
		assert l.length >= 1 : "1d translation transformations can be applied to 1d points only.";

		l[ 0 ] -= t;
	}

	@Override
	final public void fit(
			final double[][] p,
			final double[][] q,
			final double[] w )
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
			final double[] pX = p[ 0 ];
			final double[] qX = q[ 0 ];

			final double ww = w[ i ];
			ws += ww;

			pcx += ww * pX[ i ];
			qcx += ww * qX[ i ];
		}
		pcx /= ws;
		qcx /= ws;

		t = qcx - pcx;
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

		t = qcx - pcx;
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
			final double[] p = m.getP1().getL();
			final double[] q = m.getP2().getW();

			final double w = m.getWeight();
			ws += w;

			pcx += w * p[ 0 ];
			qcx += w * q[ 0 ];
		}
		pcx /= ws;
		qcx /= ws;

		t = qcx - pcx;
	}

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
	final public void set( final double t )
	{
		this.t = t;
	}

	/**
	 * TODO Not yet tested
	 */
	@Override
	public TranslationModel1D createInverse()
	{
		final TranslationModel1D ict = new TranslationModel1D();

		ict.t = -t;

		ict.cost = cost;

		return ict;
	}

	@Override
	public void toArray( final double[] data )
	{
		data[ 0 ] = 1;
		data[ 1 ] = t;
	}

	@Override
	public void toMatrix( final double[][] data )
	{
		data[ 0 ][ 0 ] = 1;
		data[ 0 ][ 1 ] = t;
	}

	@Override
	public double[] getMatrix( final double[] m )
	{
		final double[] a;
		if ( m == null || m.length != 2 )
			a = new double[ 2 ];
		else
			a = m;

		a[ 0 ] = 1;
		a[ 1 ] = t;

		return a;
	}
}
