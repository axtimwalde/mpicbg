package mpicbg.models;

import java.util.ArrayList;
import java.util.Collection;

public class PointMatch
{
	final private Point p1;
	final public Point getP1() { return p1; }
	
	final private Point p2;
	final public Point getP2() { return p2; }
	
	private float weight;
	final public float getWeight(){ return weight; } 
	final public void setWeight( float weight ){ this.weight = weight; }
	
	private float distance;
	final public float getDistance(){ return distance; }
	
	public PointMatch(
			Point p1,
			Point p2,
			float weight )
	{
		this.p1 = p1;
		this.p2 = p2;
		
		this.weight = weight;
		
		distance = Point.distance( p1, p2 );
	}
	
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
	 * apply a model to p1, update distance
	 * 
	 * @param model
	 */
	final public void apply( Model model )
	{
		p1.apply( model );
		distance = Point.distance( p1, p2 );
	}
	
	/**
	 * flip symmetrically, weight remains unchanged
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
							match.weight ) );
		}
		return list;
	}
	
}
