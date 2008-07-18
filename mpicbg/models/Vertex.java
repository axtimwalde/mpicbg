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

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

/**
 * An n-dimensional {@link Vertex} being connected to other
 * {@link Vertex Vertices} by {@link Spring Springs}
 */
public class Vertex
{
	/**
	 * The location of the {@link Vertex}.
	 */
	final protected Point location;
	final public Point getLocation() { return location; }
	
	/**
	 * A set of {@link Spring Springs} with {@link Spring#getV1() v1} being
	 * this {@link Vertex} and {@link Spring#getV2() v2} being the
	 * {@link Vertex} at the other side of the {@link Spring}.
	 */
	final protected HashMap< Vertex, Spring > springs = new HashMap< Vertex, Spring >();
	final public Collection< Spring > getSprings(){ return springs.values(); }
	/**
	 * Get all {@link Vertex Vertices} that are connected to this
	 * {@link Vertex} by a spring.
	 * 
	 * @return conntected vertices
	 */
	final public Set< Vertex > getConnectedVertices(){ return springs.keySet(); }
	
	/**
	 * Add a {@link Spring} connecting this {@link Vertex} with another
	 * {@link Vertex}.  It puts the spring to the other {@link Vertex} as well.
	 * 
	 * @param v2 the other {@link Vertex}
	 * @param weights weighting factors
	 */
	final public void addSpring(
			Vertex v2,
			float[] weights )
	{
		Spring spring = new Spring( this, v2, weights );
		springs.put( v2, spring );
		v2.springs.put( this, spring );
	}
	
	/**
	 * Add a {@link Spring} connecting this {@link Vertex} with another
	 * {@link Vertex}.  It puts the spring to the other {@link Vertex} as well.
	 * 
	 * @param v2 the other {@link Vertex}
	 * @param weights weighting factors
	 * @param maxStretch stretch limit
	 */
	final public void addSpring(
			Vertex v2,
			float[] weights,
			float maxStretch )
	{
		Spring spring = new Spring( this, v2, weights, maxStretch );
		springs.put( v2, spring );
		v2.springs.put( this, spring );
	}
	
	/**
	 * Add a {@link Spring} connecting this {@link Vertex} with another
	 * {@link Vertex}.  It puts the spring to the other {@link Vertex} as well.
	 * 
	 * @param v2 the other {@link Vertex}
	 * @param weight weighting factor (spring constant)
	 */
	final public void addSpring(
			Vertex v2,
			float weight )
	{
		Spring spring = new Spring( this, v2, weight );
		springs.put( v2, spring );
		v2.springs.put( this, spring );
	}
	
	/**
	 * Add a {@link Spring} connecting this {@link Vertex} with another
	 * {@link Vertex}.  It puts the spring to the other {@link Vertex} as well.
	 * 
	 * @param v2 the other {@link Vertex}
	 * @param weight weighting factor (spring constant)
	 * @param maxStretch stretch limit
	 */
	final public void addSpring(
			Vertex v2,
			float weight,
			float maxStretch )
	{
		Spring spring = new Spring( this, v2, weight, maxStretch );
		springs.put( v2, spring );
		v2.springs.put( this, spring );
	}
	
	/**
	 * The current moving direction of the {@link Vertex}.  The length of this
	 * vector gives the current speed. 
	 */
	final protected float[] direction;
	final public float[] getDirection(){ return direction; }
	protected float speed;
	final public float getSpeed() { return speed; }
	
	/**
	 * The sum of all forces amplitudes applied to this {@link Vertex}.
	 */
	private float forceSum;
	final public float getForceSum() { return forceSum; }
	
	/**
	 * The resulting force amplitude applied to this {@link Vertex}.
	 */
	final private float[] force;
	final public float[] getForces(){ return force; }
	private float forceAmplitude;
	final public float getForce(){ return forceAmplitude; }
	
	/**
	 * Constructor
	 * 
	 * @param location
	 */
	public Vertex( Point location )
	{
		this.location = location;
		direction = new float[ location.getL().length ];
		force = new float[ direction.length ];
	}
	
	/**
	 * Calculate the current force, direction and speed.
	 * 
	 * @param damp damping factor (0.0 fully damped, 1.0 not damped)
	 */
	final public void update( float damp )
	{
		for ( int i = 0; i < force.length; ++i )
			force[ i ] = 0;
		forceSum = 0;
		
		final float[] f = new float[ force.length ];
		float fAmplitude;
		
		Set< Vertex > vertices = springs.keySet();
		for ( Vertex vertex : vertices )
		{
			Spring spring = springs.get( vertex );
			
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
		final float[] w = location.getW();
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
		Set< Vertex > vertices = springs.keySet();
		for ( Vertex vertex : vertices )
			if ( !vertices.contains( vertex ) )
				vertex.traceConnectedGraph( graph );
		return graph.size();
	}
}
