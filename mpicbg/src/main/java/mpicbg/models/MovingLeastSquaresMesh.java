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

import java.util.HashMap;
import java.util.Set;

/**
 * A transformation mesh that implements a landmark based deformation by means
 * of Moving Least Squares as described by \citet{SchaeferAl06} inspired by the
 * implementation of Johannes Schindelin.
 *
 * BibTeX:
 * <pre>
 * @article{SchaeferAl06,
 *   author    = {Scott Schaefer and Travis McPhail and Joe Warren},
 *   title     = {Image deformation using moving least squares},
 *   journal   = {ACM Transactions on Graphics},
 *   volume    = {25},
 *   number    = {3},
 *   month     = {July},
 *   year      = {2006},
 *   issn      = {0730-0301},
 *   pages     = {533--540},
 *   publisher = {ACM},
 *   address   = {New York, NY, USA},
 * }
 * </pre>
 *
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
public class MovingLeastSquaresMesh< M extends AbstractModel< M > > extends TransformMesh
{
	private static final long serialVersionUID = -5933039703937785040L;

	/**
	 * Tiles are a collection of PointMatches that share a common
	 * transformation model.  In this implementation, each tile has the size of
	 * the whole mesh.  By this means, all tiles initially share the same
	 * reference frame/ coordinate system.
	 *
	 * PointMatches are used for two completely different things:
	 *  1. Being the vertices which span the mesh and are thus required to find
	 *     the affine transform inside each of the triangles.  The PointMatches
	 *     in a, l and pt are meant to be these vertices of the mesh.
	 *  2. Being actual point correspondences that define the local rigid
	 *     transformations of each "tile".
	 */
	final protected HashMap< PointMatch, Tile< M > > pt = new HashMap< PointMatch, Tile< M > >();
	final public HashMap< PointMatch, Tile< M > > getVerticeModelMap(){ return pt; }
	final public Set< PointMatch > getVertices(){ return pt.keySet(); }
	final public int numVertices(){ return pt.size(); }

	protected double error = Double.MAX_VALUE;
	final public double getError(){ return error; }

	final protected Class< M > modelClass;
	final public Class< M > getModelClass(){ return modelClass; }

	public MovingLeastSquaresMesh(
			final Class< M > modelClass,
			final int numX,
			final int numY,
			final double width,
			final double height )
	{
		super( numX, numY, width, height );
		this.modelClass = modelClass;

		final Set< PointMatch > s = va.keySet();
		for ( final PointMatch vertex : s )
		{
			/**
			 * Create a tile for each vertex.
			 */
			try
			{
				final M model = modelClass.newInstance();
				final Tile< M > t = new Tile< M >( model );
				pt.put( vertex, t );
			}
			catch ( final Exception e ){ e.printStackTrace(); }
		}
	}

	public MovingLeastSquaresMesh(
			final Class< M > modelClass,
			final int numX,
			final double width,
			final double height )
	{
		this( modelClass, numX, numY( numX, width, height ), width, height );
	}

	final protected double weigh( final double d, final double alpha )
	{
		return 1.0 / Math.pow( d, alpha );
	}

	/**
	 * What to use this method for:
	 *
	 * If you want to add a PointMatch between two Tiles t and o do the
	 * following.
	 * Tile t is a tile in pt, Tile o is whatever you want it to be.  The
	 * PointMatch has p1 in Tile t and p2 in Tile o.  To find Tile t being
	 * closest to some world coordinate double[] (x,y) search pt with
	 * findClosest(double[]).
	 *
	 * Then say t.addMatch(PointMatch) and o.add(PointMatch.flip())
	 */
	final public Tile< M > findClosest( final double[] there )
	{
		final Set< PointMatch > s = pt.keySet();

		PointMatch closest = null;
		double cd = Double.MAX_VALUE;
		for ( final PointMatch vertex : s )
		{
			final double[] here = vertex.getP2().getW();
			final double dx = here[ 0 ] - there[ 0 ];
			final double dy = here[ 1 ] - there[ 1 ];
			final double d = dx * dx + dy * dy;
			if ( d < cd )
			{
				cd = d;
				closest = vertex;
			}
		}
		return pt.get( closest );
	}

	/**
	 * Add a PointMatch to all Tiles weighted by its distance to the
	 * corresponding vertex.  The distance weight is defined by
	 * 1/(|m.p1.l - vertex.p1.l|^2*alpha)
	 *
	 * @param pm
	 * @param alpha
	 */
	final public void addMatchWeightedByDistance( final PointMatch pm, final double alpha )
	{
		final Set< PointMatch > s = va.keySet();
		final Tile< M > c = findClosest( pm.getP1().getW() );

		final double[] there = pm.getP1().getL();

		final double[] oldWeights = pm.getWeights();
		final double[] weights = new double[ oldWeights.length + 1 ];
		System.arraycopy( oldWeights, 0, weights, 0, oldWeights.length );
		for ( final PointMatch m : s )
		{
			// this is the original location of the vertex
			final double[] here = m.getP1().getW();
			final double dx = here[ 0 ] - there[ 0 ];
			final double dy = here[ 1 ] - there[ 1 ];

			// add a new weight to the existing weights
			//weights[ oldWeights.length ] = 1.0 / Math.pow( dx * dx + dy * dy, alpha );

			/*
			 * TODO The 0.001f constant is a bad trick to be not required to
			 *   handle the special case that the distance is 0.0
			 */
			weights[ oldWeights.length ] = weigh( 0.001 + dx * dx + dy * dy, alpha );

			// add a new PointMatch using the same Points as pm
			final Tile< M > t = pt.get( m );
			if ( t == c )
				t.addMatch( new PointMatch( pm.getP1(), pm.getP2(), weights, 1.0f ) );
			else
				t.addMatch( new PointMatch( pm.getP1(), pm.getP2(), weights, 0.0f ) );
		}
	}

	/**
	 * Updates each vertex' transformation model by means of moving least
	 * squares.
	 *
	 * @throws NotEnoughDataPointsException
	 */
	final public void updateModels()
		throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		final Set< PointMatch > s = va.keySet();

		error = 0.0;
		for ( final PointMatch m : s )
		{
			final Tile< M > t = pt.get( m );
			t.fitModel();
			t.update();

			/**
			 * Update the location of the vertex
			 */
			m.getP2().apply( t.getModel() );

			error += t.getDistance();
			//updateAffine( m );
		}
		error /= s.size();
	}

	/**
	 * Apply an arbitrary coordinate transformation to each Tile's
	 * PointMatches.  This coordinate transformation is not supposed to be
	 * compatible to {@link #modelClass}.
	 *
	 * This method is intended to be used for initializing the mesh in case
	 * that further operations estimate a refined configuration.
	 *
	 * @param t
	 */
	final public void apply( final CoordinateTransform t )
	{
		final Set< PointMatch > s = va.keySet();

		for ( final PointMatch m : s )
		{
			final Set< PointMatch > matches = pt.get( m ).getMatches();
			for ( final PointMatch match : matches )
				match.apply( t );

			/**
			 * Update the location of the vertex
			 */
			m.getP2().apply( t );

			updateAffine( m );
		}
	}
}
