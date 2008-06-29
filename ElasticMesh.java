import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import mpicbg.models.AffineModel2D;
import mpicbg.models.ErrorStatistic;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import mpicbg.models.RigidModel2D;
import mpicbg.models.Tile;

/**
 * @author saalfeld
 *
 */
public class ElasticMesh extends TransformMesh
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
	final HashMap< PointMatch, Tile > pt = new HashMap< PointMatch, Tile >();
	final HashSet< Tile > fixedTiles = new HashSet< Tile >();
	final public int numVertices(){ return pt.size(); }
	
	private double error = Double.MAX_VALUE;
	public double getError(){ return error; }
	
	final static private DecimalFormat decimalFormat = new DecimalFormat();
	final static private DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
	
	public ElasticMesh( int numX, int numY, float width, float height )
	{
		super( numX, numY, width, height );
		
		decimalFormatSymbols.setGroupingSeparator( ',' );
		decimalFormatSymbols.setDecimalSeparator( '.' );
		decimalFormat.setDecimalFormatSymbols( decimalFormatSymbols );
		decimalFormat.setMaximumFractionDigits( 3 );
		decimalFormat.setMinimumFractionDigits( 3 );
		
		float w = 1.0f / numX / numY;
		
		Set< PointMatch > s = va.keySet();
		for ( PointMatch handle : s )
		{
			/**
			 * Create a tile for each handle.
			 */
			RigidModel2D model = new RigidModel2D();
			//AffineModel2D model = new AffineModel2D();
			Tile t = new Tile( width, height, model );
			pt.put( handle, t );
			//t.update();
		}
		
		for ( PointMatch handle : s )
		{
			/**
			 * For each handle, collect its connected handles.
			 */
			HashSet< PointMatch > connectedHandles = new HashSet< PointMatch >();
			for ( AffineModel2D ai : va.get( handle ) )
			{
				for ( PointMatch m : av.get( ai ) )
				{
					if ( handle != m ) connectedHandles.add( m );
				}
			}
			
			/**
			 * Add PointMatches for each connectedHandle.
			 * These PointMatches work as "regularizers" for the mesh, that is
			 * the mesh tries to stay rigid.  The influence of these intra-stability
			 * points is given by weighting factors.
			 * 
			 * TODO Currently we assign a weight of 1 / number of vertices to
			 *   each vertex.  That represents a "density" related weight
			 *   assuming the image has an area of 1.  This will give different
			 *   results for square and non-square images.
			 *   
			 *   Should we use min( number of vertical, number of horizontal)^2
			 *   instead?
			 */
			Tile t = pt.get( handle );
			for ( PointMatch m : connectedHandles )
			{
				Tile o = pt.get( m );
				Point p2 = m.getP2();
				Point p1 = new Point( p2.getW().clone() );
				
				/*
				 * non weighted match
				 */
//				t.addMatch( new PointMatch( p1, p2 ) );
				
				/*
				 * weighted match
				 */
				t.addMatch( new PointMatch( p1, p2, w ) );
				
				t.addConnectedTile( o );
			}
		}
		
		//System.out.println( pt.size() );
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
	synchronized public Tile findClosest( float[] there )
	{
		Set< PointMatch > s = pt.keySet();
		
		PointMatch closest = null;
		float cd = Float.MAX_VALUE;
		for ( PointMatch handle : s )
		{
			float[] here = handle.getP2().getW();
			float dx = here[ 0 ] - there[ 0 ];
			float dy = here[ 1 ] - there[ 1 ];
			float d = dx * dx + dy * dy;
			if ( d < cd )
			{
				cd = d;
				closest = handle;
			}
		}
		return pt.get( closest );
	}
	
	/**
	 * Add a PointMatch to all Tiles weighted by its distance to the
	 * corresponding handle.  The distance weight is defined by
	 * 1/(|m.p2.w - handle.p2.w|^2*alpha)
	 * 
	 * @param pm
	 * @param alpha
	 */
	public void addMatchWeightedByDistance( PointMatch pm, float alpha )
	{
		Set< PointMatch > s = va.keySet();
		float[] there = pm.getP2().getW();
		
		Tile c = findClosest( there );
		
		float[] oldWeights = pm.getWeights();
		float[] weights = new float[ oldWeights.length + 1 ];
		System.arraycopy( oldWeights, 0, weights, 0, oldWeights.length );
		for ( PointMatch m : s )
		{
			float[] here = m.getP2().getW();
			float dx = here[ 0 ] - there[ 0 ];
			float dy = here[ 1 ] - there[ 1 ];
			
			// add a new weight to the existing weights
			weights[ oldWeights.length ] = 1.0f / ( float )Math.pow( dx * dx + dy * dy, alpha );
			
			// add a new PointMatch using the same Points as pm
			Tile t = pt.get( m );
			if ( t == c )
				t.addMatch( new PointMatch( pm.getP1(), pm.getP2(), weights, 1.0f ) );
			else
				t.addMatch( new PointMatch( pm.getP1(), pm.getP2(), weights, 0.0f ) );
		}
	}
	
	/**
	 * Update all PointMatches in all tiles and estimate the average
	 * displacement. 
	 */
	synchronized final private void update()
	{
		Set< PointMatch > s = va.keySet();
		double cd = 0.0;
		double min_d = Double.MAX_VALUE;
		double max_d = Double.MIN_VALUE;
		for ( PointMatch m : s )
		{
			Tile t = pt.get( m );
			t.update();

			double d = t.getDistance();
			if ( d < min_d ) min_d = d;
			if ( d > max_d ) max_d = d;
			cd += d;
		}
		cd /= pt.size();
		error = cd;
	}
	
	/**
	 * Update all PointMatches in all tiles and estimate the average
	 * displacement by weight of the PointMatch. 
	 */
	synchronized final private void updateByStrength()
	{
		Set< PointMatch > s = va.keySet();
		
		for ( PointMatch m : s )
			pt.get( m ).updateByStrength();
		
		double cd = 0.0;
		double min_d = Double.MAX_VALUE;
		double max_d = Double.MIN_VALUE;
		for ( PointMatch m : s )
		{
			double d = pt.get( m ).getDistance();
			if ( d < min_d ) min_d = d;
			if ( d > max_d ) max_d = d;
			cd += d;
		}
		cd /= pt.size();
		error = cd;
	}
	
	/**
	 * Performs one optimization iteration and writes its error into the ErrorStatistics
	 * 
	 * @param observer collecting the error after update
	 * @throws NotEnoughDataPointsException
	 */
	synchronized void optimizeIteration( ErrorStatistic observer ) throws NotEnoughDataPointsException
	{
		Set< PointMatch > s = va.keySet();
		
		error = 0.0;
		for ( PointMatch m : s )
		{
			Tile t = pt.get( m );
			
			//System.out.println( t.getMatches().size() );
			if ( fixedTiles.contains( t ) ) continue;
			t.fitModel();
			t.update();
			
			/**
			 * Update the location of the handle
			 */
			m.getP2().apply( t.getModel() );
			
			error += t.getDistance();
			updateAffine( m );
		}
		error /= s.size();
		
		observer.add( error );
	}
	
	/**
	 * Performs one optimization iteration and writes its error into the ErrorStatistics
	 * 
	 * @param observer collecting the error after update
	 * @throws NotEnoughDataPointsException
	 */
	synchronized void optimizeIterationByStrength( ErrorStatistic observer ) throws NotEnoughDataPointsException
	{
		Set< PointMatch > s = va.keySet();
		
		for ( PointMatch m : s )
		{
			Tile t = pt.get( m );
			if ( fixedTiles.contains( t ) ) continue;
			
			t.fitModel();
			
			/**
			 * Update the location of the handle
			 */
			m.getP2().apply( t.getModel() );
		}
		
		updateByStrength();
		
		observer.add( error );
	}
	
	public void fixTile( Tile t )
	{
		fixedTiles.add( t );
	}
	
	/**
	 * Minimize the displacement of all PointMatches of all tiles.
	 * 
	 * @param maxError do not accept convergence if error is > max_error
	 * @param maxIterations stop after that many iterations even if there was
	 *   no minimum found
	 * @param maxPlateauwidth convergence is reached if the average slope in
	 *   an interval of this size is 0.0 (in double accuracy).  This prevents
	 *   the algorithm from stopping at plateaus smaller than this value.
	 * 
	 * TODO  Johannes Schindelin suggested to start from a good guess, which is
	 *   e.g. the propagated unoptimized pose of a tile relative to its
	 *   connected tile that was already identified during RANSAC
	 *   correspondence check.  Thank you, Johannes, great hint!
	 */
	public void optimize(
			float maxError,
			int maxIterations,
			int maxPlateauwidth ) throws NotEnoughDataPointsException 
	{
		ErrorStatistic observer = new ErrorStatistic();
		
		int i = 0;
		
		while ( i < maxIterations )  // do not run forever
		{
			optimizeIteration( observer );			
			
			if ( i >= maxPlateauwidth && error < maxError &&  Math.abs( observer.getWideSlope( maxPlateauwidth ) ) <= 0.0001 )
				break;
			
			++i;
		}
		
		System.out.println( "Exiting at iteration " + i + " with error " + decimalFormat.format( observer.mean ) + " and slope " + observer.getWideSlope( maxPlateauwidth ) );
		System.out.println( "Successfully optimized configuration of " + pt.size() + " tiles:" );
		System.out.println( "  average displacement: " + decimalFormat.format( observer.mean ) + "px" );
		System.out.println( "  minimal displacement: " + decimalFormat.format( observer.min ) + "px" );
		System.out.println( "  maximal displacement: " + decimalFormat.format( observer.max ) + "px" );
	}
	
	/**
	 * Minimize the displacement of all PointMatches of all tiles.
	 * 
	 * @param maxError do not accept convergence if error is > max_error
	 * @param maxIterations stop after that many iterations even if there was
	 *   no minimum found
	 * @param maxPlateauwidth convergence is reached if the average slope in
	 *   an interval of this size is 0.0 (in double accuracy).  This prevents
	 *   the algorithm from stopping at plateaus smaller than this value.
	 * 
	 * TODO  Johannes Schindelin suggested to start from a good guess, which is
	 *   e.g. the propagated unoptimized pose of a tile relative to its
	 *   connected tile that was already identified during RANSAC
	 *   correspondence check.  Thank you, Johannes, great hint!
	 */
	public void optimizeByStrength(
			float maxError,
			int maxIterations,
			int maxPlateauwidth,
			ByteProcessor ipPlot,
			ImagePlus impPlot ) throws NotEnoughDataPointsException 
	{
		ErrorStatistic observer = new ErrorStatistic();
		
		int i = 0;
		
		ipPlot.setColor( Color.white );
		ipPlot.fill();
		impPlot.updateAndDraw();
		
		while ( i < maxIterations )  // do not run forever
		{
			optimizeIterationByStrength( observer );	
			
			ipPlot.set(
					( int )( i * ( float )ipPlot.getWidth() / ( float )maxIterations ),
					//( int )( ( double )observer.values.get( observer.values.size() - 1 ) ),
					( int )( 10 * ( double )observer.values.get( observer.values.size() - 1 ) ),
					//( int )( 10 * error ),
					0 );
			impPlot.updateAndDraw();
			
			if ( i >= maxPlateauwidth && error < maxError &&  Math.abs( observer.getWideSlope( maxPlateauwidth ) ) <= 0.0001 )
				break;
			
			++i;
		}
		
		updateAffines();
		
		System.out.println( "Exiting at iteration " + i + " with error " + decimalFormat.format( observer.mean ) + " and slope " + decimalFormat.format( observer.getWideSlope( maxPlateauwidth ) ) );
		System.out.println( "Successfully optimized configuration of " + pt.size() + " vertices:" );
		System.out.println( "  average displacement: " + decimalFormat.format( observer.mean ) + "px" );
		System.out.println( "  minimal displacement: " + decimalFormat.format( observer.min ) + "px" );
		System.out.println( "  maximal displacement: " + decimalFormat.format( observer.max ) + "px" );
	}
	
	/**
	 * Create a Shape that illustrates the PointMatches.
	 * 
	 * @return the illustration
	 */
	public Shape illustratePointMatches()
	{
		Set< PointMatch > s = va.keySet();
		
		GeneralPath path = new GeneralPath();
		
		for ( PointMatch m : s )
		{
			Tile t = pt.get( m );
			for ( PointMatch ma : t.getMatches() )
			{
				float[] l = ma.getP1().getW();
				
				path.moveTo( l[ 0 ] - 1, l[ 1 ] - 1 );
				path.lineTo( l[ 0 ] + 1, l[ 1 ] - 1 );
				path.lineTo( l[ 0 ] + 1, l[ 1 ] + 1 );
				path.lineTo( l[ 0 ] - 1, l[ 1 ] + 1 );
				path.closePath();
			}
		}
		return path;
	}
	
	/**
	 * Create a Shape that illustrates the displacements of PointMatches.
	 * 
	 * @return the illustration
	 */
	public Shape illustratePointMatchDisplacements()
	{
		Set< PointMatch > s = va.keySet();
		
		GeneralPath path = new GeneralPath();
		
		for ( PointMatch m : s )
		{
			Tile t = pt.get( m );
			for ( PointMatch ma : t.getMatches() )
			{
				float[] l = ma.getP1().getW();
				float[] k = ma.getP2().getW();
				
				path.moveTo( l[ 0 ], l[ 1 ] );
				path.lineTo( k[ 0 ], k[ 1 ] );
			}
		}
		return path;
	}
	
}
