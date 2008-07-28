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
import java.util.HashMap;
import java.util.Set;

import mpicbg.models.CoordinateTransform;
import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.PointMatch;
import mpicbg.models.Tile;

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
 */
public class MovingLeastSquaresMesh< M extends Model< M > > extends TransformMesh
{
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
	final public int numVertices(){ return pt.size(); }
	
	protected double error = Double.MAX_VALUE;
	final public double getError(){ return error; }
	
	final protected Class< M > modelClass;
	final public Class< M > getModelClass(){ return modelClass; }
	
	public MovingLeastSquaresMesh(
			final Class< M > modelClass,
			final int numX,
			final int numY,
			final float width,
			final float height )
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
			catch ( Exception e ){ e.printStackTrace(); }
		}
	}
	
	public MovingLeastSquaresMesh(
			final Class< M > modelClass,
			final int numX,
			final float width,
			final float height )
	{
		this( modelClass, numX, numY( numX, width, height ), width, height );
	}
	
	final protected float weigh( final float d, final float alpha )
	{
		return 1.0f / ( float )Math.pow( d, alpha );
	}
	
	/**
	 * What to use this method for:
	 * 
	 * If you want to add a PointMatch between two Tiles t and o do the
	 * following.
	 * Tile t is a tile in pt, Tile o is whatever you want it to be.  The
	 * PointMatch has p1 in Tile t and p2 in Tile o.  To find Tile t being
	 * closest to some world coordinate float[] (x,y) search pt with
	 * findClosest(float[]).
	 * 
	 * Then say t.addMatch(PointMatch) and o.add(PointMatch.flip())
	 */
	final public Tile< M > findClosest( final float[] there )
	{
		final Set< PointMatch > s = pt.keySet();
		
		PointMatch closest = null;
		float cd = Float.MAX_VALUE;
		for ( final PointMatch vertex : s )
		{
			final float[] here = vertex.getP2().getW();
			final float dx = here[ 0 ] - there[ 0 ];
			final float dy = here[ 1 ] - there[ 1 ];
			final float d = dx * dx + dy * dy;
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
	final public void addMatchWeightedByDistance( final PointMatch pm, final float alpha )
	{
		final Set< PointMatch > s = va.keySet();
		final Tile< M > c = findClosest( pm.getP1().getW() );
		
		final float[] there = pm.getP1().getL();
		
		final float[] oldWeights = pm.getWeights();
		final float[] weights = new float[ oldWeights.length + 1 ];
		System.arraycopy( oldWeights, 0, weights, 0, oldWeights.length );
		for ( final PointMatch m : s )
		{
			// this is the original location of the vertex
			final float[] here = m.getP1().getW();
			final float dx = here[ 0 ] - there[ 0 ];
			final float dy = here[ 1 ] - there[ 1 ];
			
			// add a new weight to the existing weights
			//weights[ oldWeights.length ] = 1.0f / ( float )Math.pow( dx * dx + dy * dy, alpha );
			weights[ oldWeights.length ] = weigh( 1f + dx * dx + dy * dy, alpha );
			
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
	final public void updateModels() throws NotEnoughDataPointsException
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
			updateAffine( m );
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
