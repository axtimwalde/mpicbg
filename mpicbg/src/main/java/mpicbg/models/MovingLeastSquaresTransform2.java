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
 * <p>Alternative implementation of the smooth coordinate transformation
 * interpolating between a set of control points that are maped exactly on top
 * of each other using landmark based deformation by means of Moving Least
 * Squares as described by \citet{SchaeferAl06}.</p>
 *
 * <p>This implementation stores the control points in double arrays thus
 * being significantly more memory efficient than the object based
 * {@link MovingLeastSquaresTransform}.  The object count is constant and does
 * not depend on the number of control points.</p>
 *
 * <p>Note, the {@link #apply(double[])} and {@link #applyInPlace(double[])}
 * methods are not concurrency safe because they use the same {@link Model}
 * instance to execute the local least squares fit.</p>
 *
 * BibTeX:
 * <pre>
 * &#64;article{SchaeferAl06,
 *   author    = {Scott Schaefer and Travis McPhail and Joe Warren},
 *   title     = {Image deformation using moving least squares},
 *   journal   = {ACM Transactions on Graphics},
 *   volume    = {25},
 *   number    = {3},
 *   month     = {July},
 *   year      = {2006},
 *   issn      = {0730-0301},
 *   pages     = {533--540},
 *   publisher = {ACM},
 *   address   = {New York, NY, USA},
 * }
 * </pre>
 *
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
public class MovingLeastSquaresTransform2 extends AbstractMovingLeastSquaresTransform
{
	private static final long serialVersionUID = -8809482252029606075L;

	protected float[][] p;
	protected float[][] q;
	protected float[] w;

	/**
	 * Set the control points.  {@link PointMatch PointMatches} are not stored
	 * by reference but their data is copied into internal data buffers.
	 *
	 * @param matches
	 */
	@Override
	final public void setMatches( final Collection< PointMatch > matches )
		throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		/*
		 * fragile test for number of dimensions, we expect data to be
		 * consistent
		 */
		final int n = ( matches.size() > 0 ) ? matches.iterator().next().getP1().getL().length : 0;

		p = new float[ n ][ matches.size() ];
		q = new float[ n ][ matches.size() ];
		w = new float[ matches.size() ];

		int i = 0;
		for ( final PointMatch match : matches )
		{
			final double[] pp = match.getP1().getL();
			final double[] qq = match.getP2().getW();

			for ( int d = 0; d < n; ++d )
			{
				p[ d ][ i ] = ( float )pp[ d ];
				q[ d ][ i ] = ( float )qq[ d ];
			}
			w[ i ] = ( float )match.getWeight();
			++i;
		}
		if ( n > 0 )
			model.fit( p, q, w );
		else
			throw new NotEnoughDataPointsException( "No matches passed." );
	}

	/**
	 * <p>Set the control points passing them as arrays that are used by
	 * reference.  The leading index is dimension which usually results in a
	 * reduced object count.   E.g. four 2d points are:</p>
	 * <pre>
	 * double[][]{
	 *   {x<sub>1</sub>, x<sub>2</sub>, x<sub>3</sub>, x<sub>4</sub>},
	 *   {y<sub>1</sub>, y<sub>2</sub>, y<sub>3</sub>, y<sub>4</sub>} }
	 * </pre>
	 *
	 * @param p source points
	 * @param q target points
	 * @param w weights
	 */
	final public void setMatches(
			final float[][] p,
			final float[][] q,
			final float[] w )
		throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		this.p = p;
		this.q = q;
		this.w = w;

		model.fit( p, q, w );
	}

	@Override
	public void applyInPlace( final double[] location )
	{
		final float[] ww = new float[ w.length ];
		for ( int i = 0; i < w.length; ++i )
		{
			double s = 0;
			for ( int d = 0; d < location.length; ++d )
			{
				final double dx = p[ d ][ i ] - location[ d ];
				s += dx * dx;
			}
			if ( s <= 0 )
			{
				for ( int d = 0; d < location.length; ++d )
					location[ d ] = q[ d ][ i ];
				return;
			}
			ww[ i ] = ( float )( w[ i ] * weigh( s ) );
		}

		try
		{
			model.fit( p, q, ww );
			model.applyInPlace( location );
		}
		catch ( final IllDefinedDataPointsException e ){}
		catch ( final NotEnoughDataPointsException e ){}
	}
}
