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
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 */
public class PointMatch implements Serializable
{
	private static final long serialVersionUID = -4194179098872410057L;

	final protected Point p1;
	public Point getP1() { return p1; }
	
	final protected Point p2;
	public Point getP2() { return p2; }
	
	protected float[] weights;
	public float[] getWeights(){ return weights; }
	public void setWeights( float[] weights )
	{
		this.weights = weights.clone();
		calculateWeight();
	}
	
	protected float weight;
	public float getWeight(){ return weight; }
	public void setWeight( int index, float weight )
	{
		weights[ index ] = weight;
		calculateWeight();
	}
	
	protected void calculateWeight()
	{
		weight = 1.0f;
		for ( float wi : weights )
			weight *= wi;
	}
	
	protected float strength = 1.0f;
	
	public float getDistance(){ return Point.distance( p1, p2 ); }
	
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
	 * @param strength how much should {@link #apply(Model, float)}
	 *   affect {@link #p1}
	 */
	public PointMatch(
			Point p1,
			Point p2,
			float[] weights,
			float strength )
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
			Point p1,
			Point p2,
			float[] weights )
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
			Point p1,
			Point p2,
			float weight )
	{
		this.p1 = p1;
		this.p2 = p2;
		
		weights = new float[]{ weight };
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
	 *  @param strength how much should {@link #apply(Model, float)}
	 *   affect {@link #p1}
	 */
	public PointMatch(
			Point p1,
			Point p2,
			float weight,
			float strength )
	{
		this.p1 = p1;
		this.p2 = p2;
		
		weights = new float[]{ weight };
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
			Point p1,
			Point p2 )
	{
		this.p1 = p1;
		this.p2 = p2;
		
		weights = new float[]{ 1.0f };
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
	public void apply( final CoordinateTransform t, final float amount )
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
	static public void apply( final Collection< PointMatch > matches, final CoordinateTransform t )
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
	
	static public float meanDistance( final Collection< PointMatch > matches )
	{
		double d = 0.0;
		for ( final PointMatch match : matches )
			d += match.getDistance();
		return ( float )( d / matches.size() );
	}
	
	static public float maxDistance( final Collection< PointMatch > matches )
	{
		float max = -Float.MAX_VALUE;
		for ( final PointMatch match : matches )
		{
			final float d = match.getDistance();
			if ( d > max ) max = d;
		}
		return max;
	}
}
