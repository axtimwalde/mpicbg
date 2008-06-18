package mpicbg.models;

import java.util.ArrayList;
import java.util.Collection;

public class PointMatch
{
	final private Point p1;
	final public Point getP1() { return p1; }
	
	final private Point p2;
	final public Point getP2() { return p2; }
	
	protected float[] weights;
	final public float[] getWeights(){ return weights; }
	final public void setWeights( float[] weights )
	{
		this.weights = weights.clone();
		calculateWeight();
	}
	
	protected float weight;
	final public float getWeight(){ return weight; }
	final public void setWeight( int index, float weight )
	{
		weights[ index ] = weight;
		calculateWeight();
	}
	
	final protected void calculateWeight()
	{
		weight = 1.0f;
		for ( float wi : weights )
			weight *= wi;
	}
	
	private float distance;
	final public float getDistance(){ return distance; }
	
	/**
	 * Constructor
	 * 
	 * Create a PointMatch with an Array of weights.
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
		
		distance = Point.distance( p1, p2 );
	}
	
	/**
	 * Constructor
	 * 
	 * Create a PointMatch with one weight.
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
		
		distance = Point.distance( p1, p2 );
	}
	
	/**
	 * Constructor
	 * 
	 * Create a PointMatch without weight.
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
		
		weight = 1.0f;
		
		distance = Point.distance( p1, p2 );
	}
	
	/**
	 * Apply a model to Point 1, update distance.
	 * 
	 * @param model
	 */
	final public void apply( Model model )
	{
		p1.apply( model );
		distance = Point.distance( p1, p2 );
	}
	
	/**
	 * Weigthed application a model to Point 1, update distance.
	 * Point 1 assures weight to be in the range [0,1].
	 * That is, the resulting location ends up somewhere between its actual
	 * location and the transferred one.
	 * 
	 * @param model
	 */
	final public void applyWeighted( Model model )
	{
		p1.applyWeighted( model, weight );
		distance = Point.distance( p1, p2 );
	}
	
	/**
	 * Flip symmetrically, weights remains unchanged.
	 * 
	 * @param matches
	 * @return
	 */
	final public static ArrayList< PointMatch > flip( Collection< PointMatch > matches )
	{
		ArrayList< PointMatch > list = new ArrayList< PointMatch >();
		for ( PointMatch match : matches )
		{
			list.add(
					new PointMatch(
							match.p2,
							match.p1,
							match.weights ) );
		}
		return list;
	}
	
}
