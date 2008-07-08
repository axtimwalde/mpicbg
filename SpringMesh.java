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
import ij.ImagePlus;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import mpicbg.models.AffineModel2D;
import mpicbg.models.ErrorStatistic;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.PointMatch;
import mpicbg.models.Spring;
import mpicbg.models.Tile;
import mpicbg.models.Vertex;

/**
 * A {@link TransformMesh} with all Vertices being interconnected by springs.
 * It implements the optimization straightforward as a dynamic process.
 *
 */
public class SpringMesh extends TransformMesh
{
	final protected HashSet< Vertex > fixedVertices = new HashSet< Vertex >();
	final protected HashSet< Vertex > vertices = new HashSet< Vertex >(); 
	final protected HashMap< Vertex, PointMatch > vp = new HashMap< Vertex, PointMatch >();
	final protected HashMap< PointMatch, Vertex > pv = new HashMap< PointMatch, Vertex >();
	final public int numVertices(){ return pv.size(); }
	
	final static private DecimalFormat decimalFormat = new DecimalFormat();
	final static private DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
	
	public SpringMesh(
			final int numX,
			final int numY,
			final float width,
			final float height )
	{
		super( numX, numY, width, height );
		
		decimalFormatSymbols.setGroupingSeparator( ',' );
		decimalFormatSymbols.setDecimalSeparator( '.' );
		decimalFormat.setDecimalFormatSymbols( decimalFormatSymbols );
		decimalFormat.setMaximumFractionDigits( 3 );
		decimalFormat.setMinimumFractionDigits( 3 );
		
		Set< PointMatch > s = va.keySet();
		
		float[] w = new float[ 2 ];
		w[ 0 ] = 100.0f / s.size();
		//w[ 0 ] = 0.1f;
		w[ 1 ] = 1.0f;
		
		for ( PointMatch p : s )
		{
			Vertex vertex = new Vertex( p.getP2() );
			vp.put( vertex, p );
			pv.put( p, vertex );
			vertices.add( vertex );
		}
		
		/**
		 * For each vertex, find its connected vertices and add a Spring.
		 * 
		 * Note that that
		 * {@link Vertex#addSpring(Vertex, float)} links the same
		 * {@link Spring} from both sides and thus interconnects both
		 * {@link mpicbg.models.Vertex Vertices}.
		 */
		for ( Vertex vertex : vertices )
		{
			PointMatch p = vp.get( vertex );
			for ( AffineModel2D ai : va.get( p ) )
			{
				Set< Vertex > connectedVertices = vertex.getConnectedVertices();
				for ( PointMatch m : av.get( ai ) )
				{
					Vertex connectedVertex = pv.get( m );
					if ( p != m && !connectedVertices.contains( connectedVertex ) )
						vertex.addSpring( connectedVertex, w );
				}
			}
		}
		
		/**
		 * For each vertex, find its connected vertices and add a Spring.
		 * 
		 * Note that that
		 * {@link Vertex#addSpring(Vertex, float)} links the same
		 * {@link Spring} from both sides and thus interconnects both
		 * {@link mpicbg.models.Vertex Vertices}.
		 */
		for ( Vertex vertex : vertices )
		{
			Set< Vertex > connectedVertices = vertex.getConnectedVertices();
			for ( Vertex v : connectedVertices )
			{
				Set< Vertex > currentlyConnectedVertices = vertex.getConnectedVertices();
				for ( AffineModel2D ai : va.get( vp.get( vertex ) ) )
				{
					for ( PointMatch m : av.get( ai ) )
					{
						// TODO cross-connect things
					}
				}
			}
		}
	}
	
	public SpringMesh( int numX, float width, float height )
	{
		this( numX, numY( numX, width, height ), width, height );
	}
	
	final protected float weigh( final float d, final float alpha )
	{
		return 1.0f / ( float )Math.pow( d, alpha );
	}
	
	/**
	 * Add a {@link Vertex} to the mesh.  Connect it to its next three vertices
	 * of the actual mesh by springs with the given weights.
	 *  
	 * @param vertex
	 * @param weights
	 */
	public void addVertex( final Vertex vertex, final float weight )
	{
		Set< Vertex > vertices = vp.keySet();
		final float[] there = vertex.getLocation().getW();
		
		/**
		 * Find the closest vertex.
		 */
		Vertex closest = null;
		float cd = Float.MAX_VALUE;
		for ( Vertex v : vertices )
		{
			float[] here = v.getLocation().getW();
			float dx = here[ 0 ] - there[ 0 ];
			float dy = here[ 1 ] - there[ 1 ];
			float d = dx * dx + dy * dy;
			if ( d < cd )
			{
				cd = d;
				closest = v;
			}
		}
		vertex.addSpring( closest, weight );
		
		/**
		 * The three closest vertices are connected to the closest.
		 * 
		 * TODO This has to be changed, if we interconnect vertices
		 *   differently.
		 */
		for ( Vertex v : closest.getConnectedVertices() )
		{
			if ( vertices.contains( v ) )
			{
				//System.out.println( "Adding hook vertex." );
				vertex.addSpring( v, weight );
			}
		}
	}
	
	/**
	 * Add a {@link Vertex} to the mesh.  Connect it to all other
	 * {@link Vertex Vertices} by springs that are weighted by their length.
	 * The weight is defined by 1/(l^2*alpha)
	 * 
	 * @param vertex
	 * @param weight
	 * @param alpha
	 */
	final public void addVertexWeightedByDistance(
			final Vertex vertex,
			final float weight,
			final float alpha )
	{
		Set< Vertex > vertices = vp.keySet();
		final float[] there = vertex.getLocation().getL();
		
		float[] weights = new float[]{ weight, 1.0f };
		
		for ( Vertex v : vertices )
		{
			float[] here = v.getLocation().getL();
			float dx = here[ 0 ] - there[ 0 ];
			float dy = here[ 1 ] - there[ 1 ];
			
			weights[ 1 ] = weigh( 1f + dx * dx + dy * dy, alpha );
			
			// add a Spring if the Vertex is one of the observed ones
			if ( vertices.contains( v ) )
				vertex.addSpring( v, weights );
		}
	}
	
	/**
	 * Performs one optimization step.
	 * 
	 * @param observer collecting the error after update
	 * @throws NotEnoughDataPointsException
	 */
	void optimizeStep( ErrorStatistic observer ) throws NotEnoughDataPointsException
	{
		double force = 0;
		float maxSpeed = Float.MIN_VALUE;
		synchronized ( this )
		{
			for ( Vertex vertex : vertices )
			{
				vertex.update( 0.5f );
				force += vertex.getForce();
				final float speed = vertex.getSpeed();
				if ( speed > maxSpeed ) maxSpeed = speed;
				//System.out.println( "force: " + force );
			}
			
			for ( Vertex vertex : vertices )
				vertex.move( 1.0f / maxSpeed );
		}
		observer.add( force );
	}
	
	/**
	 * Optimize the mesh.
	 * 
	 * @param maxError do not accept convergence if error is > max_error
	 * @param maxIterations stop after that many iterations even if there was
	 *   no minimum found
	 * @param maxPlateauwidth convergence is reached if the average slope in
	 *   an interval of this size is 0.0 (in double accuracy).  This prevents
	 *   the algorithm from stopping at plateaus smaller than this value.
	 * 
	 */
	public void optimize(
			float maxError,
			int maxIterations,
			int maxPlateauwidth,
			ImagePlus imp ) throws NotEnoughDataPointsException 
	{
		ErrorStatistic observer = new ErrorStatistic();
		
		int i = 0;
		
		while ( i < maxIterations )  // do not run forever
		{
			optimizeStep( observer );
			
			//imp.getCanvas().setDisplayList( illustrateMesh(), Color.white, null );
			//imp.getCanvas().setDisplayList( illustrateSprings(), Color.white, null );
			
			if (
					i >= maxPlateauwidth &&
					observer.values.get( observer.values.size() - 1 ) < maxError &&
					Math.abs( observer.getWideSlope( maxPlateauwidth ) ) <= 0.0001 &&
					Math.abs( observer.getWideSlope( maxPlateauwidth / 2 ) ) <= 0.0001 )
				break;
			
			++i;
		}
		
		updateAffines();
		
		System.out.println( "Exiting at iteration " + i + " with error " + decimalFormat.format( observer.mean ) + " and slope " + observer.getWideSlope( maxPlateauwidth ) );
		System.out.println( "Successfully optimized configuration of " + vertices.size() + " vertices:" );
		System.out.println( "  average force: " + decimalFormat.format( observer.mean ) + "N" );
		System.out.println( "  minimal force: " + decimalFormat.format( observer.min ) + "N" );
		System.out.println( "  maximal force: " + decimalFormat.format( observer.max ) + "N" );
	}
	
	/**
	 * Create a Shape that illustrates the {@Spring Springs}.
	 * 
	 * @return illustration
	 */
	public Shape illustrateSprings()
	{
		GeneralPath path = new GeneralPath();
		
		for ( Vertex vertex : vertices )
		{
			float[] l = vertex.getLocation().getW();
			for ( Vertex v : vertex.getConnectedVertices() )
			{
				float[] d = v.getForces();
				path.moveTo( l[ 0 ], l[ 1 ] );
				path.lineTo( l[ 0 ] + d[ 0 ], l[ 1 ] + d[ 1 ] );
			}
		}
		return path;
	}
}
