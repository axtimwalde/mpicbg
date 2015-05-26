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

import java.io.Serializable;

/**
 * An n-dimensional point.
 *
 * {@link #l Local coordinates} are thought to be immutable, application
 * of a model changes the {@link #w world coordinates} of the point.
 *
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
public class Point implements Serializable
{
	private static final long serialVersionUID = -5885773585629275552L;

	/**
	 * World coordinates
	 */
	final protected double[] w;
	public double[] getW() { return w; }

	/**
	 * Local coordinates
	 */
	final protected double[] l;
	public double[] getL() { return l; }

	/**
	 * Constructor
	 *
	 * Sets {@link #l} to the given double[] reference.
	 *
	 * @param l reference to the local coordinates of the {@link Point}
	 * @param w reference to the world coordinates of the {@link Point}
	 */
	public Point( final double[] l, final double[] w )
	{
		assert l.length == w.length : "Local and world coordinates have to have the same dimensionality.";

		this.l = l;
		this.w = w;
	}

	/**
	 * Constructor
	 *
	 * Sets {@link #l} to the given double[] reference.
	 *
	 * @param l reference to the local coordinates of the {@link Point}
	 */
	public Point( final double[] l )
	{
		this( l, l.clone() );
	}

	/**
	 * Apply a {@link CoordinateTransform} to the {@link Point}.
	 *
	 * Transfers the {@link #l local coordinates} to new
	 * {@link #w world coordinates}.
	 *
	 * @param t
	 */
	final public void apply( final CoordinateTransform t )
	{
		System.arraycopy( l, 0, w, 0, l.length );
		t.applyInPlace( w );
	}

	/**
	 * Apply a {@link CoordinateTransform} to the {@link Point} by a given amount.
	 *
	 * Transfers the {@link #l local coordinates} to new
	 * {@link #w world coordinates}.
	 *
	 * @param t
	 * @param amount 0.0 -> no application, 1.0 -> full application
	 */
	final public void apply( final CoordinateTransform t, final double amount )
	{
		final double[] a = t.apply( l );
		for ( int i = 0; i < a.length; ++i )
			w[ i ] += amount * ( a[ i ] - w[ i ] );
	}

	/**
	 * Apply the inverse of a {@link InvertibleModel} to the {@link Point}.
	 *
	 * Transfers the {@link #l local coordinates} to new
	 * {@link #w world coordinates}.
	 *
	 * @param model
	 */
	final public void applyInverse( final InverseCoordinateTransform t ) throws NoninvertibleModelException
	{
		System.arraycopy( l, 0, w, 0, l.length );
		t.applyInverseInPlace( w );
	}

	/**
	 * Estimate the square distance of local and world coordinates.
	 *
	 * @return square distance
	 */
	public double squareDistance()
	{
		double sum = 0.0;
		for ( int i = 0; i < l.length; ++i )
		{
			final double d = w[ i ] - l[ i ];
			sum += d * d;
		}
		return sum;
	}

	/**
	 * Estimate the Euclidean distance of local and world coordinates.
	 *
	 * @return square distance
	 */
	public double distance()
	{
		return Math.sqrt( squareDistance() );
	}


	/**
	 * Estimate the square Euclidean distance of two {@link Point Points} in
	 * world space.
	 *
	 * @param p1
	 * @param p2
	 * @return square distance
	 */
	final static public double squareDistance( final Point p1, final Point p2 )
	{
		assert p1.w.length == p2.w.length : "Both points have to have the same number of dimensions.";

		double sum = 0.0;
		for ( int i = 0; i < p1.w.length; ++i )
		{
			final double d = p1.w[ i ] - p2.w[ i ];
			sum += d * d;
		}
		return sum;
	}


	/**
	 * Estimate the Euclidean distance of two {@link Point Points} in world
	 * space.
	 *
	 * @param p1
	 * @param p2
	 * @return Euclidean distance
	 */
	final static public double distance( final Point p1, final Point p2 )
	{
		assert p1.w.length == p2.w.length:
			"Both points have to have the same number of dimensions.";

		return Math.sqrt( squareDistance( p1, p2 ) );
	}


	/**
	 * Estimate the square Euclidean distance of two {@link Point Points} in
	 * local space.
	 *
	 * @param p1
	 * @param p2
	 * @return square distance
	 */
	final static public double squareLocalDistance( final Point p1, final Point p2 )
	{
		assert p1.l.length == p2.l.length : "Both points have to have the same number of dimensions.";

		double sum = 0.0;
		for ( int i = 0; i < p1.l.length; ++i )
		{
			final double d = p1.l[ i ] - p2.l[ i ];
			sum += d * d;
		}
		return sum;
	}


	/**
	 * Estimate the Euclidean distance of two {@link Point Points} in local
	 * space.
	 *
	 * @param p1
	 * @param p2
	 * @return Euclidean distance
	 */
	final static public double localDistance( final Point p1, final Point p2 )
	{
		assert p1.l.length == p2.l.length:
			"Both points have to have the same number of dimensions.";

		return Math.sqrt( squareLocalDistance( p1, p2 ) );
	}


	/**
	 * Clone this {@link Point} instance.
	 */
	@Override
	public Point clone()
	{
		final Point p = new Point( l.clone() );
		for ( int i = 0; i < w.length; ++i )
			p.w[ i ] = w[ i ];
		return p;
	}

	/**
	 * Apply a {@link CoordinateTransform} to an {@link Iterable} collection of
	 * {@link Point Points}.
	 *
	 * For each {@link Point}, transfers the {@link #l local coordinates} to
	 * new {@link #w world coordinates}.
	 *
	 * @param t
	 */
	static public < P extends Point >void apply( final CoordinateTransform t, final Iterable< P > points )
	{
		for ( final P p : points )
			p.apply( t );
	}

	/**
	 * Apply an {@link InverseCoordinateTransform} to an {@link Iterable} collection of
	 * {@link Point Points}.
	 *
	 * For each {@link Point}, transfers the {@link #l local coordinates} to
	 * new {@link #w world coordinates}.
	 *
	 * @param t
	 */
	static public void applyInverse( final InverseCoordinateTransform t, final Iterable< Point > points ) throws NoninvertibleModelException
	{
		for ( final Point p : points )
			p.applyInverse( t );
	}
}
