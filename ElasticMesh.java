import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

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
	 * PointMatches are used for two completely different things:
	 *  1. Being the handles which span a mesh and thus are required to solve
	 *     the affine transform inside the mesh.  That is the PointMatches in
	 *     a, l and pt are meant to be the centerpoints of a "tile"
	 *  2. Being actual point correspondences that define the local rigid
	 *     transformations.
	 */
	final HashMap< PointMatch, Tile > pt = new HashMap< PointMatch, Tile >();
	final HashSet< Tile > fixedTiles = new HashSet< Tile >();
	
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
		
		Set< PointMatch > s = l.keySet();
		//HashMap< PointMatch, Point > points = new HashMap< PointMatch, Point >();
		for ( PointMatch handle : s )
		{
			/**
			 * Create a tile for each handle.
			 */
			RigidModel2D model = new RigidModel2D();
			Tile t = new Tile( width, height, model );
			pt.put( handle, t );
			Point p = new Point( new float[]{ 0, 0 } );
			//t.update();
		}
		
		for ( PointMatch handle : s )
		{
			/**
			 * For each handle, collect its connected handles.
			 */
			HashSet< PointMatch > connectedHandles = new HashSet< PointMatch >();
			for ( AffineModel2D ai : l.get( handle ) )
			{
				for ( PointMatch m : a.get( ai ) )
				{
					if ( handle != m ) connectedHandles.add( m );
				}
			}
			
			/**
			 * Add PointMatches for each connectedHandle.
			 */
			Tile t = pt.get( handle );
			//float[] here = handle.getP2().getW();
			for ( PointMatch m : connectedHandles )
			{
				Tile o = pt.get( m );
				Point p2 = m.getP2();
				Point p1 = new Point( p2.getW().clone() );
				
				//System.out.println( "Adding virtual intra-section PointMatch at: (" + p1.getL()[ 0 ] + ", " + p1.getL()[ 1 ] );
				
				//p1.apply( pt.get( m ).getModel() );
				//t.addMatch( new PointMatch( p1, p2 ) );
				t.addMatch( new PointMatch( p1, p2 ) );
				t.addConnectedTile( o );
				//t.update();
				//o.addMatch( new PointMatch( p2, p1 ) );
				//o.addConnectedTile( t );
				//o.update();
				
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
	public Tile findClosest( float[] there )
	{
		Set< PointMatch > s = pt.keySet();
		//System.out.println( s.size() );
		
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
	 * Update all PointMatches in all tiles and estimate the average
	 * displacement. 
	 */
	final private void update()
	{
		Set< PointMatch > s = l.keySet();
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
	final private void updateByWeight()
	{
		Set< PointMatch > s = l.keySet();
		double cd = 0.0;
		double min_d = Double.MAX_VALUE;
		double max_d = Double.MIN_VALUE;
		for ( PointMatch m : s )
		{
			Tile t = pt.get( m );
			t.updateWeighted();

			double d = t.getDistance();
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
	void optimizeIteration( ErrorStatistic observer ) throws NotEnoughDataPointsException
	{
		Set< PointMatch > s = l.keySet();
		
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
	void optimizeIterationByWeight( ErrorStatistic observer ) throws NotEnoughDataPointsException
	{
		Set< PointMatch > s = l.keySet();
		
		error = 0.0;
		for ( PointMatch m : s )
		{
			Tile t = pt.get( m );
			
			if ( fixedTiles.contains( t ) ) continue;
			
			t.fitModel();
			t.updateWeighted();
			
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
			
			if ( i >= maxPlateauwidth && error < maxError && observer.getWideSlope( maxPlateauwidth ) >= 0.0 )
			{
				System.out.println( "Exiting at iteration " + i + " with error " + decimalFormat.format( observer.mean ) );
				break;
			}
			
//			updateMesh();
//			apply( src, trg );
//			imp.updateAndDraw();
			++i;
		}
		
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
	public void optimizeByWeight(
			float maxError,
			int maxIterations,
			int maxPlateauwidth ) throws NotEnoughDataPointsException 
	{
		ErrorStatistic observer = new ErrorStatistic();
		
		int i = 0;
		
		while ( i < maxIterations )  // do not run forever
		{
			optimizeIterationByWeight( observer );			
			
			if ( i >= maxPlateauwidth && error < maxError && observer.getWideSlope( maxPlateauwidth ) >= 0.0 )
			{
				System.out.println( "Exiting at iteration " + i + " with error " + decimalFormat.format( observer.mean ) );
				break;
			}
			
			++i;
		}
		
		System.out.println( "Successfully optimized configuration of " + pt.size() + " vertices:" );
		System.out.println( "  average displacement: " + decimalFormat.format( observer.mean ) + "px" );
		System.out.println( "  minimal displacement: " + decimalFormat.format( observer.min ) + "px" );
		System.out.println( "  maximal displacement: " + decimalFormat.format( observer.max ) + "px" );
	}
}
