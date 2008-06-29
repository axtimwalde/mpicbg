import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;

import mpicbg.models.ErrorStatistic;
import mpicbg.models.NotEnoughDataPointsException;

/**
 * @author saalfeld
 *
 */
public class ElasticMeshStack
{
	private double error = Double.MAX_VALUE;
	public double getError(){ return error; }
	
	final public ArrayList< ElasticMesh > meshes = new ArrayList< ElasticMesh >();
	
	final static private DecimalFormat decimalFormat = new DecimalFormat();
	final static private DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
	
	public ElasticMeshStack()
	{
		decimalFormatSymbols.setGroupingSeparator( ',' );
		decimalFormatSymbols.setDecimalSeparator( '.' );
		decimalFormat.setDecimalFormatSymbols( decimalFormatSymbols );
		decimalFormat.setMaximumFractionDigits( 3 );
		decimalFormat.setMinimumFractionDigits( 3 );		
	}
	
	
	public void addMesh( ElasticMesh mesh )
	{
		meshes.add( mesh );
	}
	
	/**
	 * Performs one optimization iteration and writes its error into the ErrorStatistics
	 * 
	 * @param observer collecting the error after update
	 * @throws NotEnoughDataPointsException
	 */
	public void optimizeIteration( ErrorStatistic observer ) throws NotEnoughDataPointsException
	{
		error = 0.0;
		for ( ElasticMesh m : meshes )
		{
			m.optimizeIteration( observer );
			error += m.getError();
		}
		error /= meshes.size();
		observer.add( error );
	}
	
	/**
	 * Performs one optimization iteration and writes its error into the ErrorStatistics
	 * 
	 * @param observer collecting the error after update
	 * @throws NotEnoughDataPointsException
	 */
	public void optimizeIterationByStrength( ErrorStatistic observer ) throws NotEnoughDataPointsException
	{
		error = 0.0;
		for ( ElasticMesh m : meshes )
		{
			m.optimizeIterationByStrength( observer );
			error += m.getError();
		}
		error /= meshes.size();
		observer.add( error );
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
			
			if ( i >= maxPlateauwidth && error < maxError && observer.getWideSlope( maxPlateauwidth ) >= -0.0001 )
			{
				IJ.log( "Exiting at iteration " + i + " with error " + decimalFormat.format( observer.mean ) );
				break;
			}
			
			++i;
		}
		
		System.out.println( "Successfully optimized " + meshes.size() + " slices:" );
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
			int maxPlateauwidth ) throws NotEnoughDataPointsException 
	{
		ErrorStatistic observer = new ErrorStatistic();
		int i = 0;
		
		while ( i < maxIterations )  // do not run forever
		{
			optimizeIterationByStrength( observer );
			
			IJ.showStatus( "Optimizing... " + i + ": " + decimalFormat.format( error ) );
			
			if ( i >= maxPlateauwidth && error < maxError && Math.abs( observer.getWideSlope( maxPlateauwidth ) ) <= 0.0001 )
				break;
			
			++i;
		}
		
		IJ.log( "Exiting at iteration " + i + " with error " + decimalFormat.format( error ) );
		System.out.println( "Successfully optimized " + meshes.size() + " slices:" );
		System.out.println( "  average displacement: " + decimalFormat.format( observer.mean ) + "px" );
		System.out.println( "  minimal displacement: " + decimalFormat.format( observer.min ) + "px" );
		System.out.println( "  maximal displacement: " + decimalFormat.format( observer.max ) + "px" );
	}
	
	public void clear()
	{
		meshes.clear();
	}
}
