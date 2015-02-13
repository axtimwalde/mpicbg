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
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @param <M> the {@linkplain AbstractModel transformation model} of the tile.
 *
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
public class Tile< M extends Model< M > > implements Serializable
{
	private static final long serialVersionUID = 3715791741771321832L;

	/**
	 * The transformation {@link AbstractModel} of the {@link Tile}.  All local
	 * {@link Point Points} in the {@link Tile} share (and thus determine)
	 * this common {@link AbstractModel}.
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
	final protected Set< Tile< ? > > connectedTiles = new HashSet< Tile< ? > >();
	final public Set< Tile< ? > > getConnectedTiles() { return connectedTiles; }


	/**
	 * Add a {@link Tile} to the set of connected tiles.  Checks if this
	 * {@link Tile} is present already.
	 *
	 * @param t the new {@link Tile}.
	 * @return Success of the operation.
	 */
	final public boolean addConnectedTile( final Tile< ? > t )
	{
		return connectedTiles.add( t );
	}


	/**
	 * Remove a {@link Tile} from the set of connected {@link Tile}s.
	 *
	 * @param t the {@link Tile} to be removed.
	 * @return Success of the operation.
	 */
	final public boolean removeConnectedTile( final Tile< ? > t )
	{
		if ( connectedTiles.remove( t ) )
		{
			/* remove the PointMatches connecting to t */
			final ArrayList< PointMatch > toBeRemovedHere = new ArrayList< PointMatch >();
			final ArrayList< PointMatch > toBeRemovedThere = new ArrayList< PointMatch >();
			for ( final PointMatch p : matches )
			{
				for ( final PointMatch m : t.matches )
				{
					if ( p.getP2() == m.getP1() )
					{
						toBeRemovedHere.add( p );
						toBeRemovedThere.add( m );
						break;
					}
				}
			}
			matches.removeAll( toBeRemovedHere );
			t.matches.removeAll( toBeRemovedThere );
			return true;
		}
		else
			return false;
	}


	/**
	 * Try to find the tile which is connected by a particular
	 * {@link PointMatch}.
	 *
	 * Note that this method searches only the known connected tiles to limit
	 * the cost of that anyway expensive search.
	 *
	 * @param match
	 *
	 * @return connectedTile or null
	 */
	public Tile< ? > findConnectedTile( final PointMatch match )
	{
		final Point p = match.getP2();
		for ( final mpicbg.models.Tile< ? > t : connectedTiles )
		{
			for ( final PointMatch m : t.getMatches() )
			{
				if ( p == m.getP1() ) return t;
			}
		}
		return null;
	}




	/**
	 * The transfer error of this {@link Tile Tile's} {@link AbstractModel} as
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
	 * @param model the transformation {@link AbstractModel} of the {@link Tile}.
	 */
	public Tile( final M model )
	{
		this.model = model;
	}

	/**
	 * Apply the current {@link AbstractModel} to all local point coordinates.
	 *
	 * <em>This method does not recalculate the cost of the tile.</em>
	 */
	final public void apply()
	{
		for ( final PointMatch match : matches )
			match.apply( model );
	}

	/**
	 * Apply the current {@link AbstractModel} to all local point coordinates.
	 *
	 * <em>This method does not recalculate the cost of the tile.</em>
	 */
	final public void apply( final double amount )
	{
		for ( final PointMatch match : matches )
			match.apply( model, amount );
	}

	/**
	 * Update {@link #cost} and {@link #distance}.
	 */
	final public void updateCost()
	{
		double d = 0.0;
		double c = 0.0;

		final int numMatches = matches.size();
		if ( numMatches > 0 )
		{
			double sumWeight = 0.0;
			for ( final PointMatch match : matches )
			{
				final double dl = match.getDistance();
				d += dl;
				c += dl * dl * match.getWeight();
				sumWeight += match.getWeight();
			}
			d /= numMatches;
			c /= sumWeight;
		}
		distance = d;
		cost = c;
		model.setCost( c );
	}

	/**
	 * Apply the current {@link AbstractModel} to all local point coordinates.
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
		distance = d;
		cost = c;
		model.setCost( c );
	}

	/**
	 * Apply the current {@link AbstractModel} to all local point coordinates by weight.
	 * Update {@link #cost} and {@link #distance}.
	 *
	 */
	final public void update( final double amount )
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
		distance = d;
		cost = c;
		model.setCost( c );
	}

	/**
	 * Update the transformation {@link AbstractModel}.  That is, fit it to the
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
	final protected void traceConnectedGraph( final Set< Tile< ? > > graph )
	{
		graph.add( this );
		for ( final Tile< ? > t : connectedTiles )
		{
			if ( !graph.contains( t ) )
				t.traceConnectedGraph( graph );
		}
	}

	/**
	 * Connect two tiles by a set of point correspondences
	 *
	 * @param o
	 * @param m
	 */
	final public void connect(
			final Tile< ? > o,
			final Collection< PointMatch > m )
	{
		this.addMatches( m );
		o.addMatches( PointMatch.flip( m ) );

		this.addConnectedTile( o );
		o.addConnectedTile( this );
	}

	/**
	 * Identify the set of connected graphs that contains all given tiles.
	 *
	 * @param tiles
	 * @return
	 */
	final static public ArrayList< Set< Tile< ?  > > > identifyConnectedGraphs(
			final Collection< ? extends Tile< ? > > tiles )
	{
		final ArrayList< Set< Tile< ? > > > graphs = new ArrayList< Set< Tile< ? > > >();
		int numInspectedTiles = 0;
A:		for ( final Tile< ? > tile : tiles )
		{
			for ( final Set< Tile< ? > > knownGraph : graphs )
				if ( knownGraph.contains( tile ) ) continue A;
			final Set< Tile< ? > > current_graph = new HashSet< Tile< ? > >();
			tile.traceConnectedGraph( current_graph );
			numInspectedTiles += current_graph.size();
			graphs.add( current_graph );
			if ( numInspectedTiles == tiles.size() ) break;
		}
		return graphs;
	}
}
