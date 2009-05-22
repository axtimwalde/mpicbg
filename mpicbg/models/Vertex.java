package mpicbg.models;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

/**
 * An n-dimensional {@link Vertex} being connected to other
 * {@link Vertex Vertices} by {@link Spring Springs}
 * 
 * TODO Moivng vertices does not move points any more!!!!!!!!!!!!!!!!!!!!!!!!!!! 
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.2b
 */
public class Vertex extends Point
{
	private static final long serialVersionUID = 452481402612427648L;
	
	/**
	 * A set of {@link Spring Springs} with {@link Spring#getV1() v1} being
	 * this {@link Vertex} and {@link Spring#getV2() v2} being the
	 * {@link Vertex} at the other side of the {@link Spring}.
	 */
	final protected HashMap< Vertex, Spring > springs = new HashMap< Vertex, Spring >();
	public Collection< Spring > getSprings(){ return springs.values(); }
	/**
	 * Get all {@link Vertex Vertices} that are connected to this
	 * {@link Vertex} by a spring.
	 * 
	 * @return conntected vertices
	 */
	public Set< Vertex > getConnectedVertices(){ return springs.keySet(); }
	
	/**
	 * Add a {@link Spring} connecting this {@link Vertex} with another
	 * {@link Vertex}.  It puts the spring to the other {@link Vertex} as well.
	 * 
	 * @param v2 the other {@link Vertex}
	 * @param spring the {@link Spring}
	 */
	public void addSpring(
			final Vertex v2,
			final Spring spring )
	{
		springs.put( v2, spring );
		v2.springs.put( this, spring );
	}
	
	/**
	 * Add a {@link Spring} connecting this {@link Vertex} with another
	 * {@link Vertex}.  It puts the spring to the other {@link Vertex} as well.
	 * 
	 * @param v2 the other {@link Vertex}
	 * @param weights weighting factors
	 */
	public void addSpring(
			final Vertex v2,
			final float[] weights )
	{
		addSpring( v2, new Spring( this, v2, weights ) );
	}
	
	/**
	 * Add a {@link Spring} connecting this {@link Vertex} with another
	 * {@link Vertex}.  It puts the spring to the other {@link Vertex} as well.
	 * 
	 * @param v2 the other {@link Vertex}
	 * @param weights weighting factors
	 * @param maxStretch stretch limit
	 */
	public void addSpring(
			final Vertex v2,
			final float[] weights,
			final float maxStretch )
	{
		addSpring( v2, new Spring( this, v2, weights, maxStretch ) );
	}
	
	/**
	 * Add a {@link Spring} connecting this {@link Vertex} with another
	 * {@link Vertex}.  It puts the spring to the other {@link Vertex} as well.
	 * 
	 * @param v2 the other {@link Vertex}
	 * @param weight weighting factor (spring constant)
	 */
	public void addSpring(
			final Vertex v2,
			final float weight )
	{
		addSpring( v2, new Spring( this, v2, weight ) );
	}
	
	/**
	 * Add a {@link Spring} connecting this {@link Vertex} with another
	 * {@link Vertex}.  It puts the spring to the other {@link Vertex} as well.
	 * 
	 * @param v2 the other {@link Vertex}
	 * @param weight weighting factor (spring constant)
	 * @param maxStretch stretch limit
	 */
	public void addSpring(
			final Vertex v2,
			final float weight,
			final float maxStretch )
	{
		addSpring( v2, new Spring( this, v2, weight, maxStretch ) );
	}
	
	/**
	 * The current moving direction of the {@link Vertex}.  The length of this
	 * vector gives the current speed. 
	 */
	final protected float[] direction;
	public float[] getDirection(){ return direction; }
	protected float speed;
	public float getSpeed() { return speed; }
	
	/**
	 * The sum of all forces amplitudes applied to this {@link Vertex}.
	 */
	private float forceSum;
	public float getForceSum() { return forceSum; }
	
	/**
	 * The resulting force amplitude applied to this {@link Vertex}.
	 */
	final private float[] force;
	public float[] getForces(){ return force; }
	private float forceAmplitude;
	public float getForce(){ return forceAmplitude; }
	
	/**
	 * Constructor
	 * 
	 * @param l local coordinates
	 */
	public Vertex( final float[] l )
	{
		super( l );
		direction = new float[ l.length ];
		force = new float[ direction.length ];
	}
	
	/**
	 * Constructor
	 * 
	 * @param l local coordinates
	 * @param w world coordinates
	 */
	public Vertex( final float[] l, final float[] w )
	{
		super( l, w );
		direction = new float[ l.length ];
		force = new float[ direction.length ];
	}
	
	/**
	 * Constructor
	 * 
	 * The {@link Vertex} takes over the coordinates of the {@link Point} by
	 * pointer.  That is, changes applyed to the {@link Vertex} will affect
	 * the {@link Point} and vice versa.
	 * 
	 * TODO This is done for use in {@link SpringMesh} that replaces control
	 * points by the {@link Vertex} class.  It feels that there is a better 
	 * solution for this problem...
	 * 
	 * @param location
	 */
	public Vertex( final Point point )
	{
		this( point.getL(), point.getW() );
	}
	
	/**
	 * Calculate the current force, direction and speed.
	 * 
	 * @param damp damping factor (0.0 fully damped, 1.0 not damped)
	 */
	public void update( final float damp )
	{
		for ( int i = 0; i < force.length; ++i )
			force[ i ] = 0;
		forceSum = 0;
		
		final float[] f = new float[ force.length ];
		float fAmplitude;
		
		final Set< Vertex > vertices = springs.keySet();
		for ( final Vertex vertex : vertices )
		{
			final Spring spring = springs.get( vertex );
			
//			System.out.println(
//					"(" +
//					this.getLocation().getW()[ 0 ] +
//					", " +
//					this.getLocation().getW()[ 1 ] +
//					") -> (" +
//					vertex.getLocation().getW()[ 0 ] +
//					", " +
//					vertex.getLocation().getW()[ 1 ] +
//					")" );
//			
			spring.calculateForce( this, vertex, f );
			fAmplitude = 0;
			for ( int i = 0; i < force.length; ++i )
			{
				force[ i ] += f[ i ];
				fAmplitude += f[ i ] * f[ i ];
			}
			forceSum += ( float )Math.sqrt( fAmplitude );
		}
		
		forceAmplitude = 0;
		speed = 0;
		for ( int i = 0; i < force.length; ++i )
		{
			forceAmplitude += force[ i ] * force[ i ];
			direction[ i ] += force[ i ];
			direction[ i ] *= damp;
			speed += direction[ i ] * direction[ i ];
		}
		forceAmplitude = ( float )Math.sqrt( forceAmplitude );
		speed = ( float )Math.sqrt( speed );
	}
	
	/**
	 * Move the vertex for a given time.
	 * 
	 * @param t time
	 */
	final public void move( float t )
	{
		for ( int i = 0; i < w.length; ++i )
			w[ i ] += t * direction[ i ];
	}
	
	/**
	 * Find all {@link Vertex Vertices} that represent one connectivity graph
	 * by recursively tracing the {@link #springs }.
	 * 
	 * @param graph
	 * @return the number of connected tiles in the graph
	 */
	final public int traceConnectedGraph( Set< Vertex > graph )
	{
		graph.add( this );
		final Set< Vertex > vertices = springs.keySet();
		for ( final Vertex vertex : vertices )
			if ( !vertices.contains( vertex ) )
				vertex.traceConnectedGraph( graph );
		return graph.size();
	}
}
