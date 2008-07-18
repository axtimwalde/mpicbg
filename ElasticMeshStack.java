import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;

import mpicbg.models.ErrorStatistic;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.PointMatch;

/**
 * @author saalfeld
 *
 */
public class ElasticMeshStack
{
	private double error = Double.MAX_VALUE;
	public double getError(){ return error; }
	
	final public ArrayList< ElasticMovingLeastSquaresMesh > meshes = new ArrayList< ElasticMovingLeastSquaresMesh >();
	
	final static public DecimalFormat decimalFormat = new DecimalFormat();
	final static public DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
	
	public ElasticMeshStack()
	{
		decimalFormatSymbols.setGroupingSeparator( ',' );
		decimalFormatSymbols.setDecimalSeparator( '.' );
		decimalFormat.setDecimalFormatSymbols( decimalFormatSymbols );
		decimalFormat.setMaximumFractionDigits( 3 );
		decimalFormat.setMinimumFractionDigits( 3 );		
	}
	
	
	public void addMesh( ElasticMovingLeastSquaresMesh mesh )
	{
		meshes.add( mesh );
	}
	
	public void update( float amount )
	{
		double cd = 0.0;
		for ( ElasticMovingLeastSquaresMesh m : meshes )
		{
			m.updateByStrength( amount );
			cd += m.getError();
		}
		cd /= meshes.size();
		error = cd;
	}
	
	public void updateAffines()
	{
		for ( ElasticMovingLeastSquaresMesh m : meshes )
			m.updateAffines();
	}
	
	/**
	 * Performs one optimization iteration and writes its error into the ErrorStatistics
	 * 
	 * @throws NotEnoughDataPointsException
	 */
	public void optimizeIteration() throws NotEnoughDataPointsException
	{
		for ( ElasticMovingLeastSquaresMesh m : meshes )
			m.optimizeIteration();
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
			int maxPlateauwidth,
			ImagePlus imp,
			ImageStack src,
			ImageStack trg ) throws NotEnoughDataPointsException 
	{
		ErrorStatistic observer = new ErrorStatistic();
		int i = 0;
		
		while ( i < maxIterations )  // do not run forever
		{
			optimizeIteration();
			update( 1f );
			observer.add( error );
			
			//paint( src, trg );
			//imp.updateAndDraw();
			
			double wideSlope = 0;
			
			if ( i >= maxPlateauwidth )
			{
				wideSlope = observer.getWideSlope( maxPlateauwidth );
				if ( i >= maxPlateauwidth && error < maxError && Math.abs( wideSlope ) <= 0.001 )
					break;
			}
			
			IJ.showStatus( "Optimizing... " + i + ": " + decimalFormat.format( error ) + " ' " + decimalFormat.format( wideSlope ) );
			
			++i;
		}
		
		updateAffines();
		
		IJ.log( "Exiting at iteration " + i + " with error " + decimalFormat.format( error ) );
		System.out.println( "Successfully optimized " + meshes.size() + " slices:" );
		System.out.println( "  average displacement: " + decimalFormat.format( observer.mean ) + "px" );
		System.out.println( "  minimal displacement: " + decimalFormat.format( observer.min ) + "px" );
		System.out.println( "  maximal displacement: " + decimalFormat.format( observer.max ) + "px" );
	}
	
	public void paint( ImageStack src, ImageStack trg )
	{
		for ( int i = 0; i < meshes.size(); ++i )
		{
			ElasticMovingLeastSquaresMesh mesh = meshes.get( i );
			ImageProcessor ipSrc = src.getProcessor( i + 1 );
			ImageProcessor ipTrg = trg.getProcessor( i + 1 );
			
			mesh.updateAffines();
			
			mesh.paint( ipSrc, ipTrg );
		}
	}
	
	public void clear()
	{
		meshes.clear();
	}
}
