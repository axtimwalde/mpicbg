package mpicbg.models;

import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map.Entry;

/**
 * A {@link TransformMesh} with all Vertices being interconnected by springs.
 * It implements the optimization straightforward as a dynamic process.
 * 
 * A {@link SpringMesh} may or may not contain passive
 * {@linkplain Vertex vertices} that are not connected to other
 * {@linkplain Vertex vertices} of the mesh itself.  Depending on their
 * location, such passive {@linkplain Vertex vertices} are moved by the
 * respective {@link AffineModel2D}.  Passive {@linkplain Vertex vertices}
 * are used to uni-directionally connect two
 * {@linkplain SpringMesh SpringMeshes}.
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.2b
 */
public class SpringMesh extends TransformMesh
{
	final protected HashSet< Vertex > fixedVertices = new HashSet< Vertex >();
	final protected HashSet< Vertex > vertices = new HashSet< Vertex >();
	public Set< Vertex > getVertices(){ return vertices; }
	final protected HashMap< Vertex, PointMatch > vp = new HashMap< Vertex, PointMatch >();
	final protected HashMap< PointMatch, Vertex > pv = new HashMap< PointMatch, Vertex >();
	public int numVertices(){ return pv.size(); }
	
	final protected HashMap< AffineModel2D, Vertex > apv = new HashMap< AffineModel2D, Vertex >();
	final protected HashMap< Vertex, AffineModel2D > pva = new HashMap< Vertex, AffineModel2D >();
	
	final static private DecimalFormat decimalFormat = new DecimalFormat();
	final static private DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
	
	protected double force = 0.0;
	protected double minForce = Double.MAX_VALUE;
	protected double maxForce = 0.0;
	public double getForce(){ return force; }
	
	protected float damp;
	
	public SpringMesh(
			final int numX,
			final int numY,
			final float width,
			final float height,
			final float springWeight,
			final float maxStretch,
			final float damp )
	{
		super( numX, numY, width, height );
		
		this.damp = damp;
		
		decimalFormatSymbols.setGroupingSeparator( ',' );
		decimalFormatSymbols.setDecimalSeparator( '.' );
		decimalFormat.setDecimalFormatSymbols( decimalFormatSymbols );
		decimalFormat.setMaximumFractionDigits( 3 );
		decimalFormat.setMinimumFractionDigits( 3 );
		
		final Set< PointMatch > s = va.keySet();
		
		for ( final PointMatch p : s )
		{
			final Vertex vertex = new Vertex( p.getP2() );
			vp.put( vertex, p );
			pv.put( p, vertex );
			vertices.add( vertex );
		}
		
		/**
		 * For each vertex, find its connected vertices and add a Spring.
		 * 
		 * Note that
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
						vertex.addSpring( connectedVertex, springWeight, maxStretch );
				}
			}
		}
		
//		/**
//		 * For each vertex, find the illlustrated connections and add a Spring.
//		 * 
//		 * <pre>
//		 *   *        *
//		 *  / \      /|\
//		 * *---* -> *-+-*
//		 *  \ /      \|/
//		 *   *        *
//		 * </pre>
//		 * 
//		 * TODO This is not really necessary---isn't it?!
//		 * 
//		 * Note that that
//		 * {@link Vertex#addSpring(Vertex, float)} links the same
//		 * {@link Spring} from both sides and thus interconnects both
//		 * {@link mpicbg.models.Vertex Vertices}.
//		 */
//		w[ 0 ] *= 2;
//		for ( Vertex vertex : vertices )
//		{
//			// Find direct neighbours
//			final HashSet< PointMatch > neighbours = new HashSet< PointMatch >();
//			for ( AffineModel2D ai : va.get( vp.get( vertex ) ) )
//			{
//				for ( PointMatch m : av.get( ai ) )
//					neighbours.add( m );
//			}
//			
//			for ( PointMatch m : neighbours )
//			{
//				for ( AffineModel2D ai : va.get( m ) )
//				{
//					Set< Vertex > connectedVertices = vertex.getConnectedVertices();
//					Vertex toBeConnected = null;
//					
//					// Find out if the triangle shares exactly two vertices with connectedVertices
//					int numSharedVertices = 0;
//					for ( PointMatch p : av.get( ai ) )
//					{
//						Vertex c = pv.get( p );
//						if ( connectedVertices.contains( c ) )
//							++numSharedVertices;
//						else
//							toBeConnected = c;
//					}
//					
//					if ( numSharedVertices == 2 && toBeConnected != null )
//						vertex.addSpring( toBeConnected, w, maxStretch );
//				}
//			}
//		}
	}
	
	public SpringMesh(
			final int numX,
			final float width,
			final float height,
			final float springWeight,
			final float maxStretch,
			final float damp )
	{
		this( numX, numY( numX, width, height ), width, height, springWeight, maxStretch, damp );
	}
	
	protected float weigh( final float d, final float alpha )
	{
		return 1.0f / ( float )Math.pow( d, alpha );
	}
	
	/**
	 * Find the closest {@link Vertex} to a given coordinate in terms of its
	 * {@linkplain Vertex#getW() target coordinates}.
	 *  
	 * @param there
	 * @return closest {@link Vertex}
	 */
	final public Vertex findClosestTargetVertex( final float[] there )
	{
		Set< Vertex > vertices = vp.keySet();
		
		Vertex closest = null;
		float cd = Float.MAX_VALUE;
		for ( final Vertex v : vertices )
		{
			final float[] here = v.getW();
			final float dx = here[ 0 ] - there[ 0 ];
			final float dy = here[ 1 ] - there[ 1 ];
			final float d = dx * dx + dy * dy;
			if ( d < cd )
			{
				cd = d;
				closest = v;
			}
		}
		return closest;
	}
	
	/**
	 * Find the closest {@link Vertex} to a given coordinate in terms of its
	 * {@linkplain Vertex#getL() source coordinates}.
	 *  
	 * @param there
	 * @return closest {@link Vertex}
	 */
	final public Vertex findClosestSourceVertex( final float[] there )
	{
		Set< Vertex > vertices = vp.keySet();
		
		Vertex closest = null;
		float cd = Float.MAX_VALUE;
		for ( final Vertex v : vertices )
		{
			final float[] here = v.getL();
			final float dx = here[ 0 ] - there[ 0 ];
			final float dy = here[ 1 ] - there[ 1 ];
			final float d = dx * dx + dy * dy;
			if ( d < cd )
			{
				cd = d;
				closest = v;
			}
		}
		return closest;
	}
	
	/**
	 * Add a {@linkplain Vertex passive vertex}.  Associate it with the
	 * {@linkplain AffineModel2D triangle} by whom it is contained.
	 * 
	 * @param vertex
	 */
	public void addPassiveVertex( final Vertex vertex )
	{
		final float[] l = vertex.getL();
		final PointMatch closest = findClosestSourcePoint( vertex.getL() );
		final Collection< AffineModel2D > s = va.get( closest );
		for ( final AffineModel2D ai : s )
		{
			final ArrayList< PointMatch > pm = av.get( ai );
			if ( isInSourcePolygon( pm, l ) )
			{
				apv.put( ai, vertex );
				pva.put( vertex, ai );
				return;
			}
		}
	}
	
	/**
	 * Remove a {@linkplain Vertex passive vertex}.
	 * 
	 * @param vertex
	 */
	public void removePassiveVertex( final Vertex vertex )
	{
		apv.remove( pva.remove( vertex ) );
	}
	
	/**
	 * Add a {@link Vertex} to the mesh.  Connect it to the closest vertex
	 * of the actual mesh by a spring with the given weight.
	 *  
	 * @param vertex
	 * @param weights
	 */
	public void addVertex( final Vertex vertex, final float weight )
	{
		final Vertex closest = findClosestTargetVertex( vertex.getW() ); 
		vertex.addSpring( closest, weight );
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
		final Set< Vertex > vertices = vp.keySet();
		final float[] there = vertex.getW();
		
		float[] weights = new float[]{ weight, 1.0f };
		
		for ( final Vertex v : vertices )
		{
			final float[] here = v.getL();
			final float dx = here[ 0 ] - there[ 0 ];
			final float dy = here[ 1 ] - there[ 1 ];
			
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
	protected void optimizeStep( final ErrorStatistic observer ) throws NotEnoughDataPointsException
	{
		float maxSpeed = Float.MIN_VALUE;
		minForce = Double.MAX_VALUE;
		maxForce = 0.0;
		force = 0;
		synchronized ( this )
		{
			/* active vertices */
			for ( final Vertex vertex : vertices )
			{
				vertex.update( damp );
				force += vertex.getForce();
				final float speed = vertex.getSpeed();
				if ( speed > maxSpeed ) maxSpeed = speed;
				if ( force < minForce ) minForce = force;
				if ( force > maxForce ) maxForce = force;
			}
			
			for ( final Vertex vertex : vertices )
				vertex.move( Math.min( 1000.0f, 2.0f / maxSpeed ) );
			
			/* passive vertices */
			
			updateAffines();
			updatePassiveVertices();			
		}
		observer.add( force );
	}
	
	public void updatePassiveVertices()
	{
		for ( final Entry< Vertex, AffineModel2D > entry : pva.entrySet() )
			entry.getKey().apply( entry.getValue() );
	}
	
	@Override
	public void updateAffines()
	{
		super.updateAffines();
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
			final float maxError,
			final int maxIterations,
			final int maxPlateauwidth ) throws NotEnoughDataPointsException 
	{
		final ErrorStatistic observer = new ErrorStatistic( maxPlateauwidth + 1 );
		
		int i = 0;
		
		boolean proceed = i < maxIterations;
		
		while ( proceed )
		{
			optimizeStep( observer );
			
			if ( i > maxPlateauwidth )
			{
				proceed = observer.values.get( observer.values.lastIndex() ) > maxError;
				
				int d = maxPlateauwidth;
				while ( !proceed && d >= 1 )
				{
					try
					{
						proceed |= Math.abs( observer.getWideSlope( d ) ) > 0.0001;
					}
					catch ( Exception e ) { e.printStackTrace(); }
					d /= 2;
				}
			}
			
			proceed &= ++i < maxIterations;
		}
		
		updateAffines();
		
		System.out.println( "Successfully optimized configuration of " + vertices.size() + " vertices after " + i + " iterations:" );
		System.out.println( "  average force: " + decimalFormat.format( force ) + "N" );
		System.out.println( "  minimal force: " + decimalFormat.format( minForce ) + "N" );
		System.out.println( "  maximal force: " + decimalFormat.format( maxForce ) + "N" );
	}
	
	/**
	 * Optimize a {@link Collection} of connected {@link SpringMesh SpringMeshes}.
	 * 
	 * @param maxError do not accept convergence if error is > max_error
	 * @param maxIterations stop after that many iterations even if there was
	 *   no minimum found
	 * @param maxPlateauwidth convergence is reached if the average slope in
	 *   an interval of this size is 0.0 (in double accuracy).  This prevents
	 *   the algorithm from stopping at plateaus smaller than this value.
	 * 
	 */
	public static void optimizeMeshes(
			final Collection< SpringMesh > meshes,
			final float maxError,
			final int maxIterations,
			final int maxPlateauwidth ) throws NotEnoughDataPointsException 
	{
		final ErrorStatistic observer = new ErrorStatistic( maxPlateauwidth + 1 );
		final ErrorStatistic singleMeshObserver = new ErrorStatistic( maxPlateauwidth + 1 );
		
		int i = 0;
		
		double force = 0;
		double maxForce = 0;
		double minForce = 0;
		
		boolean proceed = i < maxIterations;
		
		while ( proceed )
		{
			force = 0;
			maxForce = -Double.MAX_VALUE;
			minForce = Double.MAX_VALUE;
			
			for ( final SpringMesh mesh : meshes )
			{
				mesh.optimizeStep( singleMeshObserver );
				force += mesh.getForce();
				final double meshMaxForce = mesh.maxForce;
				final double meshMinForce = mesh.minForce;
				if ( meshMaxForce > maxForce ) maxForce = meshMaxForce;
				if ( meshMinForce < minForce ) minForce = meshMinForce;
			}
			observer.add( force / meshes.size() );
			
			if ( i > maxPlateauwidth )
			{
				proceed = force > maxError;
				
				int d = maxPlateauwidth;
				while ( !proceed && d >= 1 )
				{
					try
					{
						proceed |= Math.abs( observer.getWideSlope( d ) ) > 0.0001;
					}
					catch ( Exception e ) { e.printStackTrace(); }
					d /= 2;
				}
			}
			
			proceed &= ++i < maxIterations;
		}
		
		for ( final SpringMesh mesh : meshes )
		{
			mesh.updateAffines();
			mesh.updatePassiveVertices();
		}
		
		System.out.println( "Successfully optimized " + meshes.size() + " meshes after " + i + " iterations:" );
		System.out.println( "  average force: " + decimalFormat.format( force ) + "N" );
		System.out.println( "  minimal force: " + decimalFormat.format( minForce ) + "N" );
		System.out.println( "  maximal force: " + decimalFormat.format( maxForce ) + "N" );
	}
	
	/**
	 * Create a Shape that illustrates the {@Spring Springs}.
	 * 
	 * @return illustration
	 */
	public Shape illustrateSprings()
	{
		final GeneralPath path = new GeneralPath();
		
		for ( final Vertex vertex : vertices )
		{
			final float[] v1 = vertex.getW();
			for ( final Vertex v : vertex.getConnectedVertices() )
			{
				final float[] v2 = v.getW();
				path.moveTo( v1[ 0 ], v1[ 1 ] );
				path.lineTo( v2[ 0 ], v2[ 1 ] );
			}
		}
		return path;
	}
	
	/**
	 * Create a Shape that illustrates the mesh.
	 * 
	 * @return the illustration
	 */
	@Override
	public Shape illustrateMesh()
	{
		final GeneralPath path = ( GeneralPath )super.illustrateMesh();
		for ( final Vertex pv : pva.keySet() )
		{
			final float[] w = pv.getW();
			path.moveTo( w[ 0 ], w[ 1 ] -1 );
			path.lineTo( w[ 0 ] + 1, w[ 1 ] );
			path.lineTo( w[ 0 ], w[ 1 ] + 1 );
			path.lineTo( w[ 0 ] - 1, w[ 1 ] );
			path.closePath();
		}
		
		return path;
	}
	
	/**
	 * TODO Not yet tested
	 */
	@Override
	public void init( final CoordinateTransform t )
	{
		super.init( t );
		updatePassiveVertices();
	}
	
	/**
	 * TODO Not yet tested
	 */
	@Override
	public void scale( final float scale )
	{
		super.scale( scale );
		
		/* active vertices */
		for ( final Vertex v : vertices )
		{
			final float[] d = v.getDirection();
			final float[] f = v.getForces();
			for ( int i = 0; i < d.length; ++i )
			{
				d[ i ] *= scale;
				f[ i ] *= scale;
			}
			for ( final Spring s : v.getSprings() )
				s.setLength( s.getLength() * scale );
		}
		
		/* passive vertices */
		for ( final Vertex v : pva.keySet() )
		{
			final float[] d = v.getDirection();
			final float[] f = v.getForces();
			for ( int i = 0; i < d.length; ++i )
			{
				d[ i ] *= scale;
				f[ i ] *= scale;
			}
			for ( final Spring s : v.getSprings() )
				s.setLength( s.getLength() * scale );
		}
		
		updatePassiveVertices();
	}
}
