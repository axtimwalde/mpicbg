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

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import mpicbg.ij.util.Util;

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
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
public class SpringMesh extends TransformMesh
{
	private static final long serialVersionUID = -7744869220563880920L;

	final static private int VIS_SIZE = 512;

	final protected HashSet< Vertex > fixedVertices = new HashSet< Vertex >();
	final protected ArrayList< Vertex > vertices = new ArrayList< Vertex >();
	public ArrayList< Vertex > getVertices(){ return vertices; }
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

	protected double maxSpeed = 0.0;

	protected double damp;

	public SpringMesh(
			final int numX,
			final int numY,
			final double width,
			final double height,
			final double springWeight,
			final double maxStretch,
			final double damp )
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
		 * {@link Vertex#addSpring(Vertex, double)} links the same
		 * {@link Spring} from both sides and thus interconnects both
		 * {@link mpicbg.models.Vertex Vertices}.
		 */
		for ( final Vertex vertex : vertices )
		{
			final PointMatch p = vp.get( vertex );
			for ( final AffineModel2D ai : va.get( p ) )
			{
				final Set< Vertex > connectedVertices = vertex.getConnectedVertices();
				for ( final PointMatch m : av.get( ai ) )
				{
					final Vertex connectedVertex = pv.get( m );
					if ( p != m && !connectedVertices.contains( connectedVertex ) )
						vertex.addSpring( connectedVertex, springWeight, maxStretch );
				}
			}
		}

//		/**
//		 * For each vertex, find the illustrated connections and add a Spring.
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
//		 * {@link Vertex#addSpring(Vertex, double)} links the same
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
			final double width,
			final double height,
			final double springWeight,
			final double maxStretch,
			final double damp )
	{
		this( numX, numY( numX, width, height ), width, height, springWeight, maxStretch, damp );
	}

	protected double weigh( final double d, final double alpha )
	{
		return 1.0 / Math.pow( d, alpha );
	}

	static protected void println( final String s ){ IJ.log( s ); }

	/**
	 * Find the closest {@link Vertex} to a given coordinate in terms of its
	 * {@linkplain Vertex#getW() target coordinates}.
	 *
	 * @param there
	 * @return closest {@link Vertex}
	 */
	final public Vertex findClosestTargetVertex( final double[] there )
	{
		final Set< Vertex > vs = vp.keySet();

		Vertex closest = null;
		double cd = Double.MAX_VALUE;
		for ( final Vertex v : vs )
		{
			final double[] here = v.getW();
			final double dx = here[ 0 ] - there[ 0 ];
			final double dy = here[ 1 ] - there[ 1 ];
			final double d = dx * dx + dy * dy;
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
	final public Vertex findClosestSourceVertex( final double[] there )
	{
		final Set< Vertex > vs = vp.keySet();

		Vertex closest = null;
		double cd = Double.MAX_VALUE;
		for ( final Vertex v : vs )
		{
			final double[] here = v.getL();
			final double dx = here[ 0 ] - there[ 0 ];
			final double dy = here[ 1 ] - there[ 1 ];
			final double d = dx * dx + dy * dy;
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
		final double[] l = vertex.getL();
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
	public void addVertex( final Vertex vertex, final double weight )
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
			final double weight,
			final double alpha )
	{
		final Set< Vertex > vs = vp.keySet();
		final double[] there = vertex.getW();

		final double[] weights = new double[]{ weight, 1.0 };

		for ( final Vertex v : vs )
		{
			final double[] here = v.getL();
			final double dx = here[ 0 ] - there[ 0 ];
			final double dy = here[ 1 ] - there[ 1 ];

			weights[ 1 ] = weigh( 1.0 + dx * dx + dy * dy, alpha );

			// add a Spring if the Vertex is one of the observed ones
			if ( vs.contains( v ) )
				vertex.addSpring( v, weights );
		}
	}


	/**
	 * Update force vectors for all vertices.
	 *
	 * @param observer
	 */
	protected void updateForce( final ErrorStatistic observer )
	{
		minForce = Double.MAX_VALUE;
		maxForce = 0.0;
		force = 0;
		synchronized ( this )
		{
			/* active vertices */
			for ( final Vertex vertex : vertices )
			{
				vertex.updateForce();
				final double vertexForce = vertex.getForce();
				force += vertexForce;
				if ( vertexForce < minForce ) minForce = vertexForce;
				if ( vertexForce > maxForce ) maxForce = vertexForce;
			}

			force /= vertices.size();
		}
		observer.add( force );
	}


	/**
	 * Calculate motion vectors for all vertices.
	 *
	 * @param observer
	 */
	protected void updateDirection( final double dt )
	{
		final double dampDt = Math.pow( damp, dt );
		synchronized ( this )
		{
			/* active vertices */
			for ( final Vertex vertex : vertices )
			{
				vertex.updateDirection( dampDt, dt );
				final double speed = vertex.getSpeed();
				if ( speed > maxSpeed ) maxSpeed = speed;
			}
		}
	}

	/**
	 * Move all vertices for a given &Delta;t
	 * @param t
	 */
	protected void update( final double dt )
	{
		synchronized ( this )
		{
			for ( final Vertex vertex : vertices )
				vertex.move( dt );

			/* passive vertices */
			updateAffines();
			updatePassiveVertices();
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
		maxSpeed = Double.MIN_VALUE;
		minForce = Double.MAX_VALUE;
		maxForce = 0.0;
		force = 0;

		synchronized ( this )
		{
			/* active vertices */
			for ( final Vertex vertex : vertices )
			{
				vertex.updateForce();
				final double vertexForce = vertex.getForce();
				if ( vertexForce < minForce ) minForce = vertexForce;
				if ( vertexForce > maxForce ) maxForce = vertexForce;
			}

			double dt = Math.min( 1000.0, 1.0 / maxForce );

			/* active vertices */
			for ( final Vertex vertex : vertices )
			{
				vertex.update( damp, dt );
				final double vertexForce = vertex.getForce();
				force += vertexForce;
				final double speed = vertex.getSpeed();
				if ( speed > maxSpeed ) maxSpeed = speed;
				if ( vertexForce < minForce ) minForce = vertexForce;
				if ( vertexForce > maxForce ) maxForce = vertexForce;
			}

			force /= vertices.size();

			dt = Math.min( dt, 1.0 / maxSpeed );

			for ( final Vertex vertex : vertices )
				vertex.move( dt );

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
			final double maxError,
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
						proceed |= Math.abs( observer.getWideSlope( d ) ) > 0.0;
					}
					catch ( final Exception e ) { e.printStackTrace(); }
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


	/* <visualization> */
	final static public ColorProcessor paintMeshes( final Collection< SpringMesh > meshes, final double scale )
	{
		final int width = ( int )( meshes.iterator().next().getWidth() * scale );
		final int height = ( int )( meshes.iterator().next().getHeight() * scale );
		final ColorProcessor ip = new ColorProcessor( width, height );
		final BufferedImage bi = ip.getBufferedImage();
		final Graphics2D g = bi.createGraphics();
		g.setBackground( Color.WHITE );
		g.clearRect( 0, 0, width, height );
		g.setTransform( new AffineTransform( scale, 0, 0, scale, 0, 0 ) );
		int i = 0;
		for ( final SpringMesh m : meshes )
		{
			final Shape shape = m.illustrateMesh();
			g.setColor( Util.createSaturatedColor( i++, meshes.size() ) );
			g.draw( shape );
		}
		return new ColorProcessor( bi );
	}
	/* </visualization> */


	/* <visualization> */
	final static public ColorProcessor paintSprings( final Collection< SpringMesh > meshes, final double scale, final double maxStretch )
	{
		final int width = mpicbg.util.Util.round( meshes.iterator().next().getWidth() * scale );
		final int height = mpicbg.util.Util.round( meshes.iterator().next().getHeight() * scale );
		final ColorProcessor ip = new ColorProcessor( width, height );

		/* estimate maximal spring stretch */
		double maxSpringStretch = 0;
		for ( final SpringMesh m : meshes )
			for ( final Vertex vertex : m.getVertices() )
			{
				for ( final Vertex v : vertex.getConnectedVertices() )
				{
					final Spring spring = vertex.getSpring( v );
					final double stretch = Math.abs( Point.distance( vertex, v ) - spring.getLength() );
					if ( stretch > maxSpringStretch ) maxSpringStretch = stretch;
				}
			}

		for ( final SpringMesh m : meshes )
			m.illustrateSprings( ip, scale, maxSpringStretch );

		return ip;
	}
	/* </visualization> */


	/* <visualization> */
	final static public ColorProcessor paintSprings( final Collection< SpringMesh > meshes, final int width, final int height, final double maxStretch )
	{
		final ColorProcessor ip = new ColorProcessor( width, height );

		/* calculate bounding box */
		final double[] min = new double[ 2 ];
		final double[] max = new double[ 2 ];

		/* estimate maximal spring stretch */
		double maxSpringStretch = 0;
		for ( final SpringMesh mesh : meshes )
		{
			final double[] meshMin = new double[ 2 ];
			final double[] meshMax = new double[ 2 ];

			mesh.bounds( meshMin, meshMax );

			mpicbg.util.Util.min( min, meshMin );
			mpicbg.util.Util.max( max, meshMax );

			for ( final Vertex vertex : mesh.getVertices() )
			{
				for ( final Vertex v : vertex.getConnectedVertices() )
				{
					final Spring spring = vertex.getSpring( v );
					final double stretch = Math.abs( Point.distance( vertex, v ) - spring.getLength() );
					if ( stretch > maxSpringStretch ) maxSpringStretch = stretch;
				}
			}
		}

		/* calculate scale */
		final double w = max[ 0 ] - min[ 0 ];
		final double h = max[ 1 ] - min[ 1 ];

		final double scale = Math.min( width / w, height / h );
		final double offsetX = ( width - scale * w ) / 2 - min[ 0 ] * scale;
		final double offsetY = ( height - scale * h ) / 2 - min[ 1 ] * scale;

		for ( final SpringMesh m : meshes )
			m.illustrateSprings( ip, scale, maxSpringStretch, offsetX, offsetY );

		return ip;
	}
	/* </visualization> */


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
			final double maxError,
			final int maxIterations,
			final int maxPlateauwidth ) throws NotEnoughDataPointsException
	{
		optimizeMeshes( meshes, maxError, maxIterations, maxPlateauwidth, false );
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
			final double maxError,
			final int maxIterations,
			final int maxPlateauwidth,
			final boolean visualize ) throws NotEnoughDataPointsException
	{
		optimizeMeshes( meshes, maxError, maxIterations, maxPlateauwidth, 0.5, visualize );
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
			final double maxError,
			final int maxIterations,
			final int maxPlateauwidth,
			final double maxStepSize,
			final boolean visualize ) throws NotEnoughDataPointsException
	{
		final ErrorStatistic observer = new ErrorStatistic( maxPlateauwidth + 1 );
		final ErrorStatistic singleMeshObserver = new ErrorStatistic( maxPlateauwidth + 1 );

		int i = 0;

		double force = 0;
		double maxForce = 0;
		double minForce = 0;

		boolean proceed = i < maxIterations;

		/* <visualization> */
		final ImageStack stackAnimation;
		final ImagePlus impAnimation;
		if ( visualize )
		{
			stackAnimation = new ImageStack( VIS_SIZE, VIS_SIZE );
			impAnimation = new ImagePlus();
		}
		else
		{
			stackAnimation = null;
			impAnimation = null;
		}
		/* </visualization> */

		println( "i mean min max" );

		/* initialize dt */
		maxForce = 0;
		for ( final SpringMesh mesh : meshes )
		{
			mesh.updateForce( singleMeshObserver );
			force += mesh.getForce();

			final double meshMaxForce = mesh.maxForce;
			final double meshMinForce = mesh.minForce;
			if ( meshMaxForce > maxForce ) maxForce = meshMaxForce;
			if ( meshMinForce < minForce ) minForce = meshMinForce;
		}
		double dt = Math.min( 1000, maxStepSize / maxForce );
		double dt1 = dt;

		while ( proceed )
		{
			force = 0;
			maxForce = 0;
			minForce = Double.MAX_VALUE;

			double maxSpeed = 0;

			/* <visualization> */
//			stackAnimation.addSlice( "" + i, paintMeshes( meshes, scale ) );
			if ( visualize )
			{
				stackAnimation.addSlice( "" + i, paintSprings( meshes, VIS_SIZE, VIS_SIZE, maxError ) );
				impAnimation.setStack( stackAnimation );
				impAnimation.updateAndDraw();
				if ( i == 1 )
				{
					impAnimation.show();
				}
			}
			/* </visualization> */

			/* update force vectors */
			for ( final SpringMesh mesh : meshes )
			{
				mesh.updateForce( singleMeshObserver );
				force += mesh.getForce();

				final double meshMaxForce = mesh.maxForce;
				final double meshMinForce = mesh.minForce;
				if ( meshMaxForce > maxForce ) maxForce = meshMaxForce;
				if ( meshMinForce < minForce ) minForce = meshMinForce;
			}
			observer.add( force / meshes.size() );

			/* adjust step size to maximum force vector */
			double dt0 = Math.min( 1000, maxStepSize / maxForce );
			dt = Math.min( dt0, ( dt1 + dt0 ) / 2.0 );

			/* update motion vectors */
			for ( final SpringMesh mesh : meshes )
			{
				mesh.updateDirection( dt );
				if ( mesh.maxSpeed > maxSpeed )
					maxSpeed = mesh.maxSpeed;
			}

			/* adjust step size to maximum motion vector */
			dt0 = Math.min( dt0, maxStepSize / maxSpeed );
			dt = Math.min( dt0, ( dt1 + dt0 ) / 2.0 );

			dt1 = dt;


			for ( final SpringMesh mesh : meshes )
			{
				mesh.update( dt );
			}

			println( new StringBuffer( i + " " ).append( force / meshes.size() ).append( " " ).append( minForce ).append( " " ).append( maxForce ).append( " " ).append( dt ).toString() );

			if ( i > maxPlateauwidth )
			{
				proceed = force > maxError;

				int d = maxPlateauwidth;
				while ( !proceed && d >= 1 )
				{
					try
					{
						proceed |= Math.abs( observer.getWideSlope( d ) ) > 0.0;
					}
					catch ( final Exception e ) { e.printStackTrace(); }
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
		System.out.println( "  average force: " + decimalFormat.format( force / meshes.size() ) + "N" );
		System.out.println( "  minimal force: " + decimalFormat.format( minForce ) + "N" );
		System.out.println( "  maximal force: " + decimalFormat.format( maxForce ) + "N" );
	}


	/* LEGACY OPTIMIZER */

	/**
	 * Calculate force and speed vectors for all vertices.
	 *
	 * @deprecated Remains for legacy compatibility
	 *
	 * @param observer
	 */
	@Deprecated
	protected void calculateForceAndSpeed( final ErrorStatistic observer )
	{
		maxSpeed = 0.0;
		minForce = Double.MAX_VALUE;
		maxForce = 0.0;
		force = 0;
		synchronized ( this )
		{
			/* active vertices */
			for ( final Vertex vertex : vertices )
			{
				vertex.update( damp );
				final double vertexForce = vertex.getForce();
				force += vertexForce;
				final double speed = vertex.getSpeed();
				if ( speed > maxSpeed ) maxSpeed = speed;
				if ( vertexForce < minForce ) minForce = vertexForce;
				if ( vertexForce > maxForce ) maxForce = vertexForce;
			}

			force /= vertices.size();
		}
		observer.add( force );
	}

	/**
	 * Optimize a {@link Collection} of connected {@link SpringMesh SpringMeshes}.
	 *
	 * @deprecated Remains for reproducing legacy results
	 *
	 * @param maxError do not accept convergence if error is > max_error
	 * @param maxIterations stop after that many iterations even if there was
	 *   no minimum found
	 * @param maxPlateauwidth convergence is reached if the average slope in
	 *   an interval of this size is 0.0 (in double accuracy).  This prevents
	 *   the algorithm from stopping at plateaus smaller than this value.
	 *
	 */
	@Deprecated
	public static void optimizeMeshes2(
			final Collection< SpringMesh > meshes,
			final double maxError,
			final int maxIterations,
			final int maxPlateauwidth ) throws NotEnoughDataPointsException
	{
		optimizeMeshes2( meshes, maxError, maxIterations, maxPlateauwidth, false );
	}

	/**
	 * Optimize a {@link Collection} of connected {@link SpringMesh SpringMeshes}.
	 *
	 * @deprecated Remains for reproducing legacy results

	 * @param maxError do not accept convergence if error is > max_error
	 * @param maxIterations stop after that many iterations even if there was
	 *   no minimum found
	 * @param maxPlateauwidth convergence is reached if the average slope in
	 *   an interval of this size is 0.0 (in double accuracy).  This prevents
	 *   the algorithm from stopping at plateaus smaller than this value.
	 *
	 */
	@Deprecated
	public static void optimizeMeshes2(
			final Collection< SpringMesh > meshes,
			final double maxError,
			final int maxIterations,
			final int maxPlateauwidth,
			final boolean visualize ) throws NotEnoughDataPointsException
	{
		final ErrorStatistic observer = new ErrorStatistic( maxPlateauwidth + 1 );
		final ErrorStatistic singleMeshObserver = new ErrorStatistic( maxPlateauwidth + 1 );

		int i = 0;

		double force = 0;
		double maxForce = 0;
		double minForce = 0;

		boolean proceed = i < maxIterations;

		/* <visualization> */
		final ImageStack stackAnimation = new ImageStack( VIS_SIZE, VIS_SIZE );
		final ImagePlus impAnimation = new ImagePlus();
		/* </visualization> */

		println( "i mean min max" );

		while ( proceed )
		{
			force = 0;
			maxForce = 0;
			minForce = Double.MAX_VALUE;

			double maxSpeed = 0;

			/* <visualization> */
//			stackAnimation.addSlice( "" + i, paintMeshes( meshes, scale ) );
			if ( visualize )
			{
				stackAnimation.addSlice( "" + i, paintSprings( meshes, VIS_SIZE, VIS_SIZE, maxError ) );
				impAnimation.setStack( stackAnimation );
				impAnimation.updateAndDraw();
				if ( i == 1 )
				{
					impAnimation.show();
				}
			}
			/* </visualization> */

			for ( final SpringMesh mesh : meshes )
			{
				mesh.calculateForceAndSpeed( singleMeshObserver );
				force += mesh.getForce();
				if ( mesh.maxSpeed > maxSpeed )
					maxSpeed = mesh.maxSpeed;

				final double meshMaxForce = mesh.maxForce;
				final double meshMinForce = mesh.minForce;
				if ( meshMaxForce > maxForce ) maxForce = meshMaxForce;
				if ( meshMinForce < minForce ) minForce = meshMinForce;
			}
			observer.add( force / meshes.size() );

			final double dt = Math.min( 1000, 1.0 / maxSpeed );

			for ( final SpringMesh mesh : meshes )
			{
				mesh.update( dt );
			}

			println( new StringBuffer( i + " " ).append( force / meshes.size() ).append( " " ).append( minForce ).append( " " ).append( maxForce ).toString() );

			if ( i > maxPlateauwidth )
			{
				proceed = force > maxError;

				int d = maxPlateauwidth;
				while ( !proceed && d >= 1 )
				{
					try
					{
						proceed |= Math.abs( observer.getWideSlope( d ) ) > 0.0;
					}
					catch ( final Exception e ) { e.printStackTrace(); }
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
		System.out.println( "  average force: " + decimalFormat.format( force / meshes.size() ) + "N" );
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
			final double[] v1 = vertex.getW();
			for ( final Vertex v : vertex.getConnectedVertices() )
			{
				final double[] v2 = v.getW();
				path.moveTo( v1[ 0 ], v1[ 1 ] );
				path.lineTo( v2[ 0 ], v2[ 1 ] );
			}
		}
		return path;
	}


	/**
	 * Paint all {@Spring Springs} into a {@link ColorProcessor}.
	 *
	 * @return illustration
	 */
	public void illustrateSprings( final ColorProcessor ip, final double scale, final double maxStretch )
	{
		for ( final Vertex vertex : vertices )
		{
			final double[] v1 = vertex.getW();
			for ( final Vertex v : vertex.getConnectedVertices() )
			{
				final Spring spring = vertex.getSpring( v );
				//final double stretch = mpicbg.util.Util.pow( Math.min( 1.0f, Math.abs( Point.distance( vertex, v ) - spring.getLength() ) / maxStretch ), 2 );
				final double stretch = Math.min( 1.0, Math.abs( Point.distance( vertex, v ) - spring.getLength() ) / maxStretch );

				final double r = Math.min( 1, stretch * 2 );
				final double g = Math.min( 1, 2 - stretch * 2 );

				ip.setColor( new Color( ( float )r, ( float )g, 0 ) );

				final double[] v2 = v.getW();

				ip.drawLine(
						mpicbg.util.Util.round( scale * v1[ 0 ] ),
						mpicbg.util.Util.round( scale * v1[ 1 ] ),
						mpicbg.util.Util.round( scale * v2[ 0 ] ),
						mpicbg.util.Util.round( scale * v2[ 1 ] ) );
			}
		}
	}


	/**
	 * Paint all {@Spring Springs} into a {@link ColorProcessor}.
	 *
	 * @return illustration
	 */
	public void illustrateSprings( final ColorProcessor ip, final double scale, final double maxStretch, final double offsetX, final double offsetY )
	{
		for ( final Vertex vertex : vertices )
		{
			final double[] v1 = vertex.getW();
			for ( final Vertex v : vertex.getConnectedVertices() )
			{
				final Spring spring = vertex.getSpring( v );
				//final double stretch = mpicbg.util.Util.pow( Math.min( 1.0f, Math.abs( Point.distance( vertex, v ) - spring.getLength() ) / maxStretch ), 2 );
				final double stretch = Math.min( 1.0, Math.abs( Point.distance( vertex, v ) - spring.getLength() ) / maxStretch );

				final double r = Math.min( 1, stretch * 2 );
				final double g = Math.min( 1, 2 - stretch * 2 );

				ip.setColor( new Color( ( float )r, ( float )g, 0 ) );

				final double[] v2 = v.getW();

				ip.drawLine(
						mpicbg.util.Util.round( scale * v1[ 0 ] + offsetX ),
						mpicbg.util.Util.round( scale * v1[ 1 ] + offsetY ),
						mpicbg.util.Util.round( scale * v2[ 0 ] + offsetX ),
						mpicbg.util.Util.round( scale * v2[ 1 ] + offsetY ) );
			}
		}
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
		for ( final Vertex vertex : pva.keySet() )
		{
			final double[] w = vertex.getW();
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
	public void scale( final double scale )
	{
		super.scale( scale );

		/* active vertices */
		for ( final Vertex v : vertices )
		{
			final double[] d = v.getDirection();
			final double[] f = v.getForces();
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
			final double[] d = v.getDirection();
			final double[] f = v.getForces();
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
