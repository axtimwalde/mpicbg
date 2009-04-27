package mpicbg.models;


import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Set;

public class ElasticMeshStack< M extends AbstractAffineModel2D< M > >
{
	private double error = Double.MAX_VALUE;
	public double getError(){ return error; }
	
	final public ArrayList< ElasticMovingLeastSquaresMesh< M > > meshes = new ArrayList< ElasticMovingLeastSquaresMesh< M > >();
	
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
	
	
	final public void addMesh( final ElasticMovingLeastSquaresMesh< M > mesh )
	{
		meshes.add( mesh );
	}
	
	final public void update( final float amount )
	{
		double cd = 0.0;
		for ( final ElasticMovingLeastSquaresMesh m : meshes )
		{
			m.update( amount );
			cd += m.getError();
		}
		cd /= meshes.size();
		error = cd;
	}
	
	final public void updateAffines()
	{
		for ( final ElasticMovingLeastSquaresMesh< M > m : meshes )
			m.updateAffines();
	}
	
	/**
	 * Performs one optimization iteration and writes its error into the ErrorStatistics
	 * 
	 * @throws NotEnoughDataPointsException
	 */
	final public void optimizeIteration()
		throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		for ( final ElasticMovingLeastSquaresMesh< M > m : meshes )
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
	final public void optimize(
			final float maxAllowedError,
			final int maxIterations,
			final int maxPlateauwidth,
			final ImagePlus imp,
			final ImageStack src,
			final ImageStack trg )
		throws NotEnoughDataPointsException, IllDefinedDataPointsException 
	{
		final ErrorStatistic observer = new ErrorStatistic();
		int i = 0;
		
		while ( i < maxIterations )  // do not run forever
		{
			optimizeIteration();
			update( 0.5f );
			//update( 1f );
			observer.add( error );
			
			//paint( src, trg );
			//imp.updateAndDraw();
			
			double wideSlope = 0;			
			
			if ( i >= maxPlateauwidth )
			{
				wideSlope = observer.getWideSlope( maxPlateauwidth );
				if (
						error < maxAllowedError &&
						Math.abs( wideSlope ) <= 0.0001 &&
						Math.abs( observer.getWideSlope( maxPlateauwidth / 2 ) ) <= 0.0001 )
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
	
	/**
	 * Minimize the displacement of all PointMatches of all tiles.
	 * 
	 * @param maxError do not accept convergence if error is > max_error
	 * @param maxIterations stop after that many iterations even if there was
	 *   no minimum found
	 * @param maxPlateauwidth convergence is reached if the average slope in
	 *   an interval of this size is 0.0 (in double accuracy).  This prevents
	 *   the algorithm from stopping at plateaus smaller than this value.
	 * @param maxTrust reject PointMatches with an error > median * maxTrust
	 * 
	 * TODO  Johannes Schindelin suggested to start from a good guess, which is
	 *   e.g. the propagated unoptimized pose of a tile relative to its
	 *   connected tile that was already identified during RANSAC
	 *   correspondence check.  Thank you, Johannes, great hint!
	 */
	final public void optimizeAndFilter(
			final float maxAllowedError,
			final int maxIterations,
			final int maxPlateauwidth,
			final float maxTrust,
			final ImagePlus imp,
			final ImageStack src,
			final ImageStack trg )
		throws NotEnoughDataPointsException, IllDefinedDataPointsException 
	{
		int numMatches = Integer.MAX_VALUE;
		int lastNumMatches = Integer.MAX_VALUE;
		final ArrayList< PointMatch > badMatches = new ArrayList< PointMatch >();
		do
		{
			lastNumMatches = numMatches;
			optimize(
					maxAllowedError,
					maxIterations,
					maxPlateauwidth,
					imp,
					src,
					trg );
			numMatches = 0;
			final ErrorStatistic observer = new ErrorStatistic();
			for ( final ElasticMovingLeastSquaresMesh< ? > m : meshes )
			{
				Set< PointMatch > tiles = m.getVertices();
				for ( final PointMatch pm : tiles )
				{
					final Tile< ? > t = m.getVerticeModelMap().get( pm );
					for ( final PointMatch p : t.getMatches() )
					{
						observer.add( p.getDistance() );
					}
				}
			}
			
			final double max = observer.getMedian() * maxTrust;
			
			for ( final ElasticMovingLeastSquaresMesh< ? > m : meshes )
			{
				Set< PointMatch > tiles = m.getVertices();
				for ( final PointMatch pm : tiles )
				{
					final Tile< ? > t = m.getVerticeModelMap().get( pm );
					badMatches.clear();
					for ( final PointMatch p : t.getMatches() )
					{
						if ( p.getDistance() > max )
							badMatches.add( p );
						else
							++numMatches;
					}
					t.getMatches().removeAll( badMatches );
				}
			}
			IJ.log( ( lastNumMatches - numMatches ) + " matches removed." );
			IJ.log( "Re-optimizing ..." );
		}
		while ( numMatches < lastNumMatches );
	}
	
	public void clear()
	{
		meshes.clear();
	}
}
