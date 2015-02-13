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
import java.util.ArrayList;
import java.util.Collection;

/**
 * A link between two {@link Point Points} that are expected to be ideally at
 * the same location in the world coordinate space.
 *
 * The link is directed, such that each link touches only {@link #p1}.
 *
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
public class PointMatch implements Serializable
{
	private static final long serialVersionUID = -3943684962223800732L;

	protected double strength = 1.0f;

	final protected Point p1;
	public Point getP1() { return p1; }

	final protected Point p2;
	public Point getP2() { return p2; }

	protected double[] weights;

	protected void calculateWeight()
	{
		weight = 1.0;
		for ( final double wi : weights )
			weight *= wi;
	}

	public double[] getWeights(){ return weights; }
	public void setWeights( final double[] weights )
	{
		this.weights = weights.clone();
		calculateWeight();
	}

	protected double weight;
	public double getWeight(){ return weight; }
	public void setWeight( final int index, final double weight )
	{
		weights[ index ] = weight;
		calculateWeight();
	}


	/**
	 * Get the last weights element and remove it from the list.  In case that
	 * only one element is in the list, the element is not removed but set to
	 * 1.0.
	 *
	 * @return
	 */
	public double popWeight()
	{
		final int l = weights.length - 1;
		final double w = weights[ l ];
		if ( l > 0 )
		{
			final double[] newWeights = new double[ l ];
			System.arraycopy( weights, 0, newWeights, 0, l );
			weights = newWeights;
			calculateWeight();
		}
		else
			weights[ 0 ] = weight = 1.0;

		return w;
	}


	/**
	 * Append a new element to the right side of the weights list.
	 *
	 * @param w
	 */
	public void pushWeight( final double w )
	{
		final double[] newWeights = new double[ weights.length + 1 ];
		System.arraycopy( weights, 0, newWeights, 0, weights.length );
		newWeights[ weights.length ] = w;
		weights = newWeights;
		weight *= w;
	}


	/**
	 * Get the first weights element and remove it from the list.  In case that
	 * only one element is in the list, the element is not removed but set to
	 * 1.0.
	 *
	 * @return
	 */
	public double shiftWeight()
	{
		final int l = weights.length - 1;
		final double w = weights[ 0 ];
		if ( l > 0 )
		{
			final double[] newWeights = new double[ l ];
			System.arraycopy( weights, 1, newWeights, 0, l );
			weights = newWeights;
			calculateWeight();
		}
		else
			weights[ 0 ] = weight = 1.0;

		return w;
	}


	/**
	 * Append a new element to the left side of the weights list.
	 *
	 * @param w
	 */
	public void unshiftWeight( final double w )
	{
		final double[] newWeights = new double[ weights.length + 1 ];
		System.arraycopy( weights, 0, newWeights, 1, weights.length );
		newWeights[ 0 ] = w;
		weights = newWeights;
		weight *= w;
	}


	public double getDistance(){ return Point.distance( p1, p2 ); }

	/**
	 * Constructor
	 *
	 * Create a {@link PointMatch} with an Array of weights and a strength.
	 * The Array of weights will be copied.
	 *
	 * Strength gives the amount of application:
	 *   strength = 0 means {@link #p1} will not be transferred,
	 *   strength = 1 means {@link #p1} will be fully transferred
	 *
	 * @param p1 Point 1
	 * @param p2 Point 2
	 * @param weights Array of weights
	 * @param strength how much should {@link #apply(Model, double)}
	 *   affect {@link #p1}
	 */
	public PointMatch(
			final Point p1,
			final Point p2,
			final double[] weights,
			final double strength )
	{
		this.p1 = p1;
		this.p2 = p2;

		this.weights = weights.clone();
		calculateWeight();

		this.strength = strength;
	}

	/**
	 * Constructor
	 *
	 * Create a {@link PointMatch} with an Array of weights.
	 * The Array of weights will be copied.
	 *
	 * @param p1 Point 1
	 * @param p2 Point 2
	 * @param weights Array of weights
	 */
	public PointMatch(
			final Point p1,
			final Point p2,
			final double[] weights )
	{
		this.p1 = p1;
		this.p2 = p2;

		this.weights = weights.clone();
		calculateWeight();
	}

	/**
	 * Constructor
	 *
	 * Create a {@link PointMatch} with one weight.
	 *
	 * @param p1 Point 1
	 * @param p2 Point 2
	 * @param weight Weight
	 */
	public PointMatch(
			final Point p1,
			final Point p2,
			final double weight )
	{
		this.p1 = p1;
		this.p2 = p2;

		weights = new double[]{ weight };
		this.weight = weight;
	}

	/**
	 * Constructor
	 *
	 * Create a {@link PointMatch} with one weight and strength.
	 *
	 * Strength gives the amount of application:
	 *   strength = 0 means {@link #p1} will not be transferred,
	 *   strength = 1 means {@link #p1} will be fully transferred
	 *
	 * @param p1 Point 1
	 * @param p2 Point 2
	 * @param weight Weight
	 *  @param strength how much should {@link #apply(Model, double)}
	 *   affect {@link #p1}
	 */
	public PointMatch(
			final Point p1,
			final Point p2,
			final double weight,
			final double strength )
	{
		this.p1 = p1;
		this.p2 = p2;

		weights = new double[]{ weight };
		this.weight = weight;

		this.strength = strength;
	}

	/**
	 * Constructor
	 *
	 * Create a {@link PointMatch} without weight.
	 *
	 * @param p1 Point 1
	 * @param p2 Point 2
	 * @param weights Weight
	 */
	public PointMatch(
			final Point p1,
			final Point p2 )
	{
		this.p1 = p1;
		this.p2 = p2;

		weights = new double[]{ 1.0f };
		weight = 1.0f;
	}

	/**
	 * Apply a {@link CoordinateTransform} to {@link #p1}, update distance.
	 *
	 * @param t
	 */
	public void apply( final CoordinateTransform t )
	{
		p1.apply( t );
	}

	/**
	 * Apply a {@link CoordinateTransform} to {@link #p1} with a given amount,
	 * update distance.
	 *
	 * @param t
	 * @param amount
	 */
	public void apply( final CoordinateTransform t, final double amount )
	{
		p1.apply( t, strength * amount );
	}

	/**
	 * Apply a {@link CoordinateTransform} to {@link #p1} a {@link Collection}
	 * of {@link PointMatch PointMatches}, update their distances.
	 *
	 * @param matches
	 * @param t
	 */
	static public void apply( final Collection< ? extends PointMatch > matches, final CoordinateTransform t )
	{
		for ( final PointMatch match : matches )
			match.apply( t );
	}

	/**
	 * Flip all {@link PointMatch PointMatches} from
	 * {@linkplain Collection matches} symmetrically and fill
	 * {@linkplain Collection flippedMatches} with them, weights remain
	 * unchanged.
	 *
	 * @param matches original set
	 * @param flippedMatches result set
	 */
	final public static void flip(
			final Collection< PointMatch > matches,
			final Collection< PointMatch > flippedMatches )
	{
		for ( final PointMatch match : matches )
			flippedMatches.add(
					new PointMatch(
							match.p2,
							match.p1,
							match.weights ) );
	}

	/**
	 * Flip symmetrically, weights remains unchanged.
	 *
	 * @param matches
	 * @return
	 */
	final public static Collection< PointMatch > flip( final Collection< PointMatch > matches )
	{
		final ArrayList< PointMatch > list = new ArrayList< PointMatch >();
		flip( matches, list );
		return list;
	}

	final public static void sourcePoints( final Collection< PointMatch > matches, final Collection< Point > sourcePoints )
	{
		for ( final PointMatch m : matches )
			sourcePoints.add( m.getP1() );
	}

	final public static void cloneSourcePoints( final Collection< PointMatch > matches, final Collection< Point > sourcePoints )
	{
		for ( final PointMatch m : matches )
			sourcePoints.add( m.getP1().clone() );
	}

	final public static void targetPoints( final Collection< PointMatch > matches, final Collection< Point > targetPoints )
	{
		for ( final PointMatch m : matches )
			targetPoints.add( m.getP2() );
	}

	final public static void cloneTargetPoints( final Collection< PointMatch > matches, final Collection< Point > targetPoints )
	{
		for ( final PointMatch m : matches )
			targetPoints.add( m.getP2().clone() );
	}

	static public double meanDistance( final Collection< PointMatch > matches )
	{
		double d = 0.0;
		for ( final PointMatch match : matches )
			d += match.getDistance();
		return d / matches.size();
	}

	static public double maxDistance( final Collection< PointMatch > matches )
	{
		double max = -Double.MAX_VALUE;
		for ( final PointMatch match : matches )
		{
			final double d = match.getDistance();
			if ( d > max ) max = d;
		}
		return max;
	}
}
