package mpicbg.models;

import java.util.Random;

/**
 * A simple spring model.  Instances represent the actual spring only, not the
 * {@link Vertex Vertices} it may interconnect.
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.2b
 */
public class Spring
{
	final static protected Random rnd = new Random( 0 );
	final protected float length;
	static protected float length(
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
	protected void calculateWeight()
	{
		weight = 1.0f;
		for ( final float wi : weights )
			weight *= wi;
	}
	
	public float getWeight(){ return weight; }
	public float[] getWeights(){ return weights; }
	public void setWeights( final float[] weights )
	{
		this.weights = weights.clone();
		calculateWeight();
	}
	public void addWeight( final float weight )
	{
		final float[] newWeights = new float[ weights.length + 1 ];
		System.arraycopy( weights, 0, newWeights, 0, weights.length );
		newWeights[ weights.length ] = weight;
		weights = newWeights;
		calculateWeight();
	}
	
	public void removeWeight( final int index )
	{
		if ( index < weights.length )
		{
			final float[] newWeights = new float[ weights.length - 1 ];
			int j = 0;
			for ( int i = 0; i < weights.length; ++i )
				if ( i != index )
					newWeights[ j++ ] = weights[ i ];
			weights = newWeights;
			calculateWeight();
		}
	}
	
	public void setWeight( final int index, final float weight )
	{
		weights[ index ] = weight;
		calculateWeight();
	}
	
	/**
	 * Estimate the force vector effective to {@link Vertex v1} if this {@link Spring} would
	 * connect both {@link Vertex Vertices}.
	 *  
	 * @param v1 
	 * @param v2
	 * @param force
	 */
	public void calculateForce(
			final Vertex v1,
			final Vertex v2,
			final float[] force )
	{
		assert
				force.length == v1.getL().length &&
				force.length == v2.getL().length :
				"Both vertices and force have to have the same dimensionality.";
		
		final float[] w1 = v1.getW();
		final float[] w2 = v2.getW();
		
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
			final float length,
			final float[] weights )
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
			final float length,
			final float[] weights,
			final float maxStretch )
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
			final float length,
			final float weight )
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
			final float length,
			final float weight,
			final float maxStretch )
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
			final Vertex v1,
			final Vertex v2,
			final float[] weights )
	{
		this( length( v1.getL(), v2.getL() ), weights );
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
			final Vertex v1,
			final Vertex v2,
			final float[] weights,
			final float maxStretch )
	{
		this( length( v1.getL(), v2.getL() ), weights, maxStretch );
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
			final Vertex v1,
			final Vertex v2,
			final float weight )
	{
		this( length( v1.getL(), v2.getL() ), weight );
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
			final Vertex v1,
			final Vertex v2,
			final float weight,
			final float maxStretch )
	{
		this( length( v1.getL(), v2.getL() ), weight, maxStretch );
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
			final Vertex v1,
			final Vertex v2 )
	{
		this( length( v1.getL(), v2.getL() ) );
	}	
}
