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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Tile< M extends Model< M > >
{
	/**
	 * The transformation {@link Model} of the {@link Tile}.  All local
	 * {@link Point Points} in the {@link Tile} share (and thus determine)
	 * this common {@link Model}.
	 */
	final protected M model;
	final public M getModel() { return model; }
	
	/**
	 * A set of point correspondences with {@link PointMatch#getP1() p1} being
	 * a local point in this {@link Tile} and {@link PointMatch#getP2() p2}
	 * being the corresponding  local point in another {@link Tile}.
	 * {@link Tile Tiles} are perfectly registered if both
	 * {@link PointMatch#getP1() p1} and {@link PointMatch#getP1() p1} have the
	 * same world coordinates for all {@link PointMatch PointMatches}.
	 */
	final protected Set< PointMatch > matches = new HashSet< PointMatch >();
	final public Set< PointMatch > getMatches(){ return matches; }
	
	/**
	 * Add more {@link PointMatch PointMatches}.
	 *  
	 * @param more the {@link PointMatch PointMatches} to be added
	 * 
	 * @return true if this set changed as a result of the call
	 */
	final public boolean addMatches( final Collection< PointMatch > more )
	{
		return matches.addAll( more );
	}
	
	/**
	 * Add one {@link PointMatch}.
	 *  
	 * @param match the {@link PointMatch} to be added
	 * 
	 * @return true if this set did not already contain the specified element
	 */
	final public boolean addMatch( final PointMatch match )
	{
		return matches.add( match );
	}
	
	/**
	 * Remove a {@link PointMatch}.
	 * 
	 * @param match the {@link PointMatch} to be removed
	 * 
	 * @return true if this set contained the specified element
	 */
	final public boolean removeMatch( final PointMatch match )
	{
		return matches.remove( match );
	}
	
	/**
	 * A set of {@link Tile Tiles} that share point correpondences with this
	 * {@link Tile}.
	 * 
	 * Note that point correspondences do not know about the tiles they belong
	 * to.
	 */
	final private Set< Tile< ? extends Model > > connectedTiles = new HashSet< Tile< ? extends Model > >();
	final public Set< Tile< ? extends Model > > getConnectedTiles() { return connectedTiles; }
	
	
	/**
	 * Add a {@link Tile} to the set of connected tiles.  Checks if this
	 * {@link Tile} is present already.
	 * 
	 * @param t the new {@link Tile}.
	 * @return Success of the operation.
	 */
	final public boolean addConnectedTile( final Tile< ? extends Model > t )
	{
		return connectedTiles.add( t );
	}
	
	
	/**
	 * Remove a {@link Tile} from the set of connected {@link Tile}s.
	 * 
	 * @param t the {@link Tile} to be removed.
	 * @return Success of the operation.
	 */
	final public boolean removeConnectedTile( final Tile< ? extends Model > t )
	{
		return connectedTiles.remove( t );
	}
	
	
	/**
	 * The transfer error of this {@link Tile Tile's} {@link Model} as
	 * estimated from weighted square point correspondence displacement.
	 */
	protected double cost;
	final public double getCost() { return cost; }
	
	
	/**
	 * The average {@link PointMatch} displacement.
	 */
	private double distance;
	final public double getDistance() { return distance; }
	
	/**
	 * Constructor
	 * 
	 * @param model the transformation {@link Model} of the {@link Tile}.
	 */
	public Tile( final M model )
	{
		this.model = model;
	}
	
	/**
	 * Apply the current {@link Model} to all local point coordinates.
	 * Update {@link #cost} and {@link #distance}.
	 *
	 */
	final public void update()
	{
		double d = 0.0;
		double c = 0.0;
		
		final int numMatches = matches.size();
		if ( numMatches > 0 )
		{
			double sumWeight = 0.0;
			for ( final PointMatch match : matches )
			{
				match.apply( model );
				final double dl = match.getDistance();
				d += dl;
				c += dl * dl * match.getWeight();
				sumWeight += match.getWeight();
			}
			d /= numMatches;
			c /= sumWeight;
		}
		distance = ( float )d;
		cost = ( float )c;
		model.setCost( c );
	}
	
	/**
	 * Apply the current {@link Model} to all local point coordinates by weight.
	 * Update {@link #cost} and {@link #distance}.
	 *
	 */
	final public void update( final float amount )
	{
		double d = 0.0;
		double c = 0.0;
		
		final int numMatches = matches.size();
		if ( numMatches > 0 )
		{
			double sumWeight = 0.0;
			for ( final PointMatch match : matches )
			{
				match.apply( model, amount );
				final double dl = match.getDistance();
				d += dl;
				c += dl * dl * match.getWeight();
				sumWeight += match.getWeight();
			}
			d /= numMatches;
			c /= sumWeight;
		}
		distance = ( float )d;
		cost = ( float )c;
		model.setCost( c );
	}
	
	/**
	 * Randomly dice new model until the cost is smaller than the old one
	 * 
	 * @param maxNumTries maximal number of tries before returning false (which means "no better model found")
	 * @param amount strength of shaking
	 * @return true if a better model was found
	 */
	final public boolean diceBetterModel( final int maxNumTries, final float amount )
	{
		// store old model
		final M oldModel = model.clone();
		
		for ( int t = 0; t < maxNumTries; ++t )
		{
			model.shake( amount );
			update();
			if ( model.betterThan( oldModel ) )
			{
				return true;
			}
			else model.set( oldModel );
		}
		// no better model found, so roll back
		update();
		return false;
	}
	
	/**
	 * Update the transformation {@link Model}.  That is, fit it to the
	 * current set of {@link PointMatch PointMatches}.
	 */
	final public void fitModel() throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		model.fit( matches );
	}
	
	/**
	 * Find all {@link Tile}s that represent one connectivity graph by
	 * recursively tracing the {@link #connectedTiles }.
	 * 
	 * @param graph
	 * @return the number of connected tiles in the graph
	 */
	final private int traceConnectedGraph( final Set< Tile< ? extends Model > > graph )
	{
		graph.add( this );
		for ( final Tile< ? extends Model > t : connectedTiles )
		{
			if ( !graph.contains( t ) )
				t.traceConnectedGraph( graph );
		}
		return graph.size();
	}
	
	/**
	 * connect two tiles by a set of point correspondences
	 * 
	 * re-weighs the point correpondences
	 * 
	 * We set a weigh of 1.0 / num_matches to each correspondence to equalize
	 * the connections between tiles during minimization.
	 * TODO Check if this is a good idea...
	 * TODO What about the size of a detection, shouldn't it be used as a
	 * weight factor as	well?
	 * 
	 * Change 2007-10-27
	 * Do not normalize by changing the weight, correpondences are weighted by
	 * feature scale. 
	 * 
	 * @param o
	 * @param matches
	 */
	final public void connect(
			final Tile< ? extends Model > o,
			final Collection< PointMatch > matches )
	{
		this.addMatches( matches );
		o.addMatches( PointMatch.flip( matches ) );
		
		this.addConnectedTile( o );
		o.addConnectedTile( this );
	}
	
	/**
	 * Identify the set of connected graphs that contains all given tiles.
	 * 
	 * @param tiles
	 * @return
	 */
	final static public ArrayList< Set< Tile< ? extends Model > > > identifyConnectedGraphs(
			Collection< Tile< ? extends Model > > tiles )
	{
		ArrayList< Set< Tile< ? extends Model > > > graphs = new ArrayList< Set< Tile< ? extends Model > > >();
		int numInspectedTiles = 0;
A:		for ( final Tile< ? extends Model > tile : tiles )
		{
			for ( final Set< Tile< ? extends Model > > knownGraph : graphs )
				if ( knownGraph.contains( tile ) ) continue A; 
			Set< Tile< ? extends Model > > current_graph = new HashSet< Tile< ? extends Model > >();
			numInspectedTiles += tile.traceConnectedGraph( current_graph );
			graphs.add( current_graph );
			if ( numInspectedTiles == tiles.size() ) break;
		}
		return graphs;
	}
}
