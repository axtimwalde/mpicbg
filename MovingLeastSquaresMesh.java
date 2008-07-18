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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
public class MovingLeastSquaresMesh extends TransformMesh
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
	final protected HashMap< PointMatch, Tile > pt = new HashMap< PointMatch, Tile >();
	final public int numVertices(){ return pt.size(); }
	
	protected double error = Double.MAX_VALUE;
	final public double getError(){ return error; }
	
	final protected Class< ? extends Model > modelClass;
	final public Class< ? extends Model > getModelClass(){ return modelClass; }
	
	public MovingLeastSquaresMesh( final int numX, final int numY, final float width, final float height, final Class< ? extends Model > modelClass )
	{
		super( numX, numY, width, height );
		this.modelClass = modelClass; 
		
		Set< PointMatch > s = va.keySet();
		for ( PointMatch vertex : s )
		{
			/**
			 * Create a tile for each vertex.
			 */
			try
			{
				Model model = modelClass.newInstance();
				Tile t = new Tile( width, height, model );
				pt.put( vertex, t );
			}
			catch ( Exception e ){ e.printStackTrace(); }
		}
	}
	
	public MovingLeastSquaresMesh( final int numX, final float width, final float height, final Class< ? extends Model > modelClass )
	{
		this( numX, numY( numX, width, height ), width, height, modelClass );
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
	final public Tile findClosest( final float[] there )
	{
		Set< PointMatch > s = pt.keySet();
		
		PointMatch closest = null;
		float cd = Float.MAX_VALUE;
		for ( PointMatch vertex : s )
		{
			float[] here = vertex.getP2().getW();
			float dx = here[ 0 ] - there[ 0 ];
			float dy = here[ 1 ] - there[ 1 ];
			float d = dx * dx + dy * dy;
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
		Set< PointMatch > s = va.keySet();
		float[] there = pm.getP1().getW();
		Tile c = findClosest( there );
		
		there = pm.getP1().getL();
		
		float[] oldWeights = pm.getWeights();
		float[] weights = new float[ oldWeights.length + 1 ];
		System.arraycopy( oldWeights, 0, weights, 0, oldWeights.length );
		for ( PointMatch m : s )
		{
			// this is the original location of the vertex
			float[] here = m.getP1().getW();
			float dx = here[ 0 ] - there[ 0 ];
			float dy = here[ 1 ] - there[ 1 ];
			
			// add a new weight to the existing weights
			//weights[ oldWeights.length ] = 1.0f / ( float )Math.pow( dx * dx + dy * dy, alpha );
			weights[ oldWeights.length ] = weigh( 1f + dx * dx + dy * dy, alpha );
			
			// add a new PointMatch using the same Points as pm
			Tile t = pt.get( m );
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
		Set< PointMatch > s = va.keySet();
		
		error = 0.0;
		for ( PointMatch m : s )
		{
			Tile t = pt.get( m );
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
	final public void apply( CoordinateTransform t )
	{
		Set< PointMatch > s = va.keySet();
		
		for ( PointMatch m : s )
		{
			ArrayList< PointMatch > matches = pt.get( m ).getMatches();
			for ( PointMatch match : matches )
				match.apply( t );
			
			/**
			 * Update the location of the vertex
			 */
			m.getP2().apply( t );
			
			updateAffine( m );
		}
	}
}
