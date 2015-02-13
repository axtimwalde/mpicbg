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
import java.util.Random;

/**
 * A simple spring model.  Instances represent the actual spring only, not the
 * {@link Point Points} it may interconnect.
 *
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
public class Spring implements Serializable
{
	private static final long serialVersionUID = 1901169757781308736L;

	final static protected Random rnd = new Random( 0 );

	protected double length;
	public double getLength() { return length; }
	public void setLength( final double length ) { this.length = length; }

	static protected double squareLength(
			final double[] p1,
			final double[] p2 )
	{
		assert
				p1.length == p2.length :
				"Both locations have to have the same dimensionality.";

		double l = 0;
		for ( int i = 0; i < p1.length; ++i )
		{
			final double dl = p1[ i ] - p2[ i ];
			l += dl * dl;
		}
		return l;
	}

	static protected double length(
			final double[] p1,
			final double[] p2 )
	{
		return Math.sqrt( squareLength( p1, p2 ) );
	}

	final protected double maxStretch;

	protected double[] weights;
	protected double weight;
	protected void calculateWeight()
	{
		weight = 1.0;
		for ( final double wi : weights )
			weight *= wi;
	}

	public double getWeight(){ return weight; }
	public double[] getWeights(){ return weights; }
	public void setWeights( final double[] weights )
	{
		this.weights = weights.clone();
		calculateWeight();
	}
	public void addWeight( final double w )
	{
		final double[] newWeights = new double[ weights.length + 1 ];
		System.arraycopy( weights, 0, newWeights, 0, weights.length );
		newWeights[ weights.length ] = w;
		weights = newWeights;
		calculateWeight();
	}

	public void removeWeight( final int index )
	{
		if ( index < weights.length )
		{
			final double[] newWeights = new double[ weights.length - 1 ];
			int j = 0;
			for ( int i = 0; i < weights.length; ++i )
				if ( i != index )
					newWeights[ j++ ] = weights[ i ];
			weights = newWeights;
			calculateWeight();
		}
	}

	public void setWeight( final int index, final double weight )
	{
		weights[ index ] = weight;
		calculateWeight();
	}

	/**
	 * Estimate the force vector effective to {@link Point p1} if this {@link Spring} would
	 * connect both {@link Point Vertices}.
	 *
	 * @param p1
	 * @param p2
	 * @param force
	 */
	public void calculateForce(
			final Point p1,
			final Point p2,
			final double[] force )
	{
		assert
				force.length == p1.getL().length &&
				force.length == p2.getL().length :
				"Both vertices and force have to have the same dimensionality.";

		final double[] w1 = p1.getW();
		final double[] w2 = p2.getW();

		final double lw = length( w1, w2 );
		final double d = lw - length;

		/**
		 * If stretched more than maxStretch then disrupt.
		 */
		if ( Math.abs( d ) > maxStretch )
			for ( int i = 0; i < force.length; ++i )
				force[ i ] = 0;
		else if ( lw == 0.0f )
		{
			for ( int i = 0; i < force.length; ++i )
				force[ i ] = 0;

			force[ ( int )( force.length * rnd.nextDouble() ) ] = d * weight;
		}
		else
			for ( int i = 0; i < w1.length; ++i )
				force[ i ] = ( w2[ i ] - w1[ i ] ) / lw * d * weight;
	}

	/**
	 * Constructor
	 *
	 * Create a {@link Spring} with an Array of weights.
	 * The Array of weights will be copied.
	 *
	 * @param length
	 * @param weights Array of weights
	 */
	public Spring(
			final double length,
			final double[] weights )
	{
		this.length = length;
		this.weights = weights.clone();
		calculateWeight();
		this.maxStretch = Double.MAX_VALUE;
	}

	/**
	 * Constructor
	 *
	 * Create a {@link Spring} with an Array of weights.
	 * The Array of weights will be copied.
	 *
	 * @param length
	 * @param weights Array of weights
	 * @param maxStretch stretch limit
	 */
	public Spring(
			final double length,
			final double[] weights,
			final double maxStretch )
	{
		this.length = length;
		this.weights = weights.clone();
		calculateWeight();
		this.maxStretch = maxStretch;
	}

	/**
	 * Constructor
	 *
	 * Create a {@link Spring} with one weight.
	 *
	 * @param length
	 * @param weight (spring constant)
	 */
	public Spring(
			final double length,
			final double weight )
	{
		this.length = length;
		weights = new double[]{ weight };
		this.weight = weight;
		this.maxStretch = Double.MAX_VALUE;
	}

	/**
	 * Constructor
	 *
	 * Create a {@link Spring} with one weight.
	 *
	 * @param length
	 * @param weight (spring constant)
	 * @param maxStretch stretch limit
	 */
	public Spring(
			final double length,
			final double weight,
			final double maxStretch )
	{
		this.length = length;
		weights = new double[]{ weight };
		this.weight = weight;
		this.maxStretch = maxStretch;
	}

	/**
	 * Constructor
	 *
	 * Create a {@link Spring} without weight.
	 *
	 * @param length
	 */
	public Spring( final double length )
	{
		this.length = length;
		weight = 1.0;
		this.maxStretch = Double.MAX_VALUE;
	}

	/**
	 * Constructor
	 *
	 * Create a {@link Spring} between two {@link Point Points} with an
	 * Array of weights.  The Array of weights will be copied.
	 *
	 * @param p1 Point 1
	 * @param p2 Point 2
	 * @param weights Array of weights
	 */
	public Spring(
			final Point p1,
			final Point p2,
			final double[] weights )
	{
		this( length( p1.getL(), p2.getL() ), weights );
	}

	/**
	 * Constructor
	 *
	 * Create a {@link Spring} between two {@link Point Points} with an
	 * Array of weights.  The Array of weights will be copied.
	 *
	 * @param v1 Point 1
	 * @param v2 Point 2
	 * @param weights Array of weights
	 * @param maxStretch stretch limit
	 */
	public Spring(
			final Point p1,
			final Point p2,
			final double[] weights,
			final double maxStretch )
	{
		this( length( p1.getL(), p2.getL() ), weights, maxStretch );
	}


	/**
	 * Constructor
	 *
	 * Create a {@link Spring} between two {@link Point Points} with one weight.
	 *
	 * @param p1 Point 1
	 * @param p2 Point 2
	 * @param weight Weight
	 */
	public Spring(
			final Point p1,
			final Point p2,
			final double weight )
	{
		this( length( p1.getL(), p2.getL() ), weight );
	}

	/**
	 * Constructor
	 *
	 * Create a {@link Spring} between two {@link Point Points} with one weight.
	 *
	 * @param p1 Point 1
	 * @param p2 Point 2
	 * @param weight Weight
	 * @param maxStretch stretch limit
	 */
	public Spring(
			final Point p1,
			final Point p2,
			final double weight,
			final double maxStretch )
	{
		this( length( p1.getL(), p2.getL() ), weight, maxStretch );
	}

	/**
	 * Constructor
	 *
	 * Create a {@link Spring} between two {@link Point Points} without weight.
	 *
	 * @param p1 Point 1
	 * @param p2 Point 2
	 */
	public Spring(
			final Point p1,
			final Point p2 )
	{
		this( length( p1.getL(), p2.getL() ) );
	}
}
