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

import java.util.Random;

/**
 * A simple spring model.  Instances represent the acyual spring only, not the
 * {@link Vertex Vertices} it may interconnect.
 * 
 */
public class Spring
{
	final static protected Random rnd = new Random( 0 );
	final protected float length;
	final static protected float length(
			float[] p1,
			float[] p2 )
	{
		assert
				p1.length == p2.length :
				"Both locations have to have the same dimensionality.";
		
		float l = 0;
		for ( int i = 0; i < p1.length; ++i )
		{
			final float dl = p1[ i ] - p2[ i ];
			l += dl * dl;
		}
		return ( float )Math.sqrt( l );			
	}
	
	final protected float maxStretch;
	
	protected float[] weights;
	protected float weight;
	final protected void calculateWeight()
	{
		weight = 1.0f;
		for ( float wi : weights )
			weight *= wi;
	}
	
	final public float getWeight(){ return weight; }
	final public float[] getWeights(){ return weights; }
	final public void setWeights( float[] weights )
	{
		this.weights = weights.clone();
		calculateWeight();
	}
	final public void addWeight( float weight )
	{
		float[] newWeights = new float[ weights.length + 1 ];
		System.arraycopy( weights, 0, newWeights, 0, weights.length );
		newWeights[ weights.length ] = weight;
		calculateWeight();
	}
	final public void removeWeight( int index )
	{
		if ( index < weights.length )
		{
			float[] newWeights = new float[ weights.length - 1 ];
			int j = 0;
			for ( int i = 0; i < weights.length; ++i )
				if ( i != index )
					newWeights[ j++ ] = weights[ i ];
			calculateWeight();
		}
	}
	final public void setWeight( int index, float weight )
	{
		weights[ index ] = weight;
		calculateWeight();
	}
	
	/**
	 * Estimate the force vector effective to {@link Vertex v1} if this {@link Spring} would
	 * connect both {@Vertex Vertices}.
	 *  
	 * @param v1 
	 * @param v2
	 * @param force
	 */
	final public void calculateForce(
			Vertex v1,
			Vertex v2,
			float[] force )
	{
		assert
				force.length == v1.getLocation().getL().length &&
				force.length == v2.getLocation().getL().length :
				"Both vertices and force have to have the same dimensionality.";
		
		final float[] w1 = v1.getLocation().getW();
		final float[] w2 = v2.getLocation().getW();
		
		float lw = length( w1, w2 );
		final float d = lw - length;
		
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
			float length,
			float[] weights )
	{
		this.length = length;
		this.weights = weights.clone();
		calculateWeight();
		this.maxStretch = Float.MAX_VALUE;
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
			float length,
			float[] weights,
			float maxStretch )
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
			float length,
			float weight )
	{
		this.length = length;
		weights = new float[]{ weight };
		this.weight = weight;
		this.maxStretch = Float.MAX_VALUE;
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
			float length,
			float weight,
			float maxStretch )
	{
		this.length = length;
		weights = new float[]{ weight };
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
	public Spring( float length )
	{
		this.length = length;
		weight = 1.0f;
		this.maxStretch = Float.MAX_VALUE;
	}
	
	/**
	 * Constructor
	 * 
	 * Create a {@link Spring} between two {@link Vertex Vertices} with an
	 * Array of weights.  The Array of weights will be copied.
	 * 
	 * @param v1 Vertex 1
	 * @param v2 Vertex 2
	 * @param weights Array of weights
	 */
	public Spring(
			Vertex v1,
			Vertex v2,
			float[] weights )
	{
		this( length( v1.getLocation().getL(), v2.getLocation().getL() ), weights );
	}
	
	/**
	 * Constructor
	 * 
	 * Create a {@link Spring} between two {@link Vertex Vertices} with an
	 * Array of weights.  The Array of weights will be copied.
	 * 
	 * @param v1 Vertex 1
	 * @param v2 Vertex 2
	 * @param weights Array of weights
	 * @param maxStretch stretch limit
	 */
	public Spring(
			Vertex v1,
			Vertex v2,
			float[] weights,
			float maxStretch )
	{
		this( length( v1.getLocation().getL(), v2.getLocation().getL() ), weights, maxStretch );
	}
	
	
	/**
	 * Constructor
	 * 
	 * Create a {@link Spring} between two {@link Vertex Vertices} with one weight.
	 * 
	 * @param v1 Vertex 1
	 * @param v2 Vertex 2
	 * @param weight Weight
	 */
	public Spring(
			Vertex v1,
			Vertex v2,
			float weight )
	{
		this( length( v1.getLocation().getL(), v2.getLocation().getL() ), weight );
	}
	
	/**
	 * Constructor
	 * 
	 * Create a {@link Spring} between two {@link Vertex Vertices} with one weight.
	 * 
	 * @param v1 Vertex 1
	 * @param v2 Vertex 2
	 * @param weight Weight
	 * @param maxStretch stretch limit
	 */
	public Spring(
			Vertex v1,
			Vertex v2,
			float weight,
			float maxStretch )
	{
		this( length( v1.getLocation().getL(), v2.getLocation().getL() ), weight, maxStretch );
	}
	
	/**
	 * Constructor
	 * 
	 * Create a {@link Spring} between two {@link Vertex Vertices} without weight.
	 * 
	 * @param v1 Vertex 1
	 * @param v2 Vertex 2
	 */
	public Spring(
			Vertex v1,
			Vertex v2 )
	{
		this( length( v1.getLocation().getL(), v2.getLocation().getL() ) );
	}	
}
