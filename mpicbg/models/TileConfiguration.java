package mpicbg.models;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;


/**
 * A configuration of tiles.
 * 
 * Add all tiles that build a common interconnectivity graph to one
 * configuration, fix at least one of the tiles and optimize the configuration.
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.2b
 */
public class TileConfiguration
{
	final static private DecimalFormat decimalFormat = new DecimalFormat();
	final static private DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();

	final private Set< Tile< ? > > tiles = new HashSet< Tile< ? > >();
	final public Set< Tile< ? > > getTiles(){ return tiles; }
	
	final private Set< Tile< ? > > fixedTiles = new HashSet< Tile< ? > >();
	final public Set< Tile< ? > > getFixedTiles(){ return fixedTiles; }
	
	private double minError = Double.MAX_VALUE;
	final public double getMinError() {	return minError; }
	
	private double maxError = 0.0;
	final public double getMaxError() { return maxError; }
	
	private double error = Double.MAX_VALUE;
	final public double getError() { return error; }

	public TileConfiguration()
	{
		decimalFormatSymbols.setGroupingSeparator( ',' );
		decimalFormatSymbols.setDecimalSeparator( '.' );
		decimalFormat.setDecimalFormatSymbols( decimalFormatSymbols );
		decimalFormat.setMaximumFractionDigits( 3 );
		decimalFormat.setMinimumFractionDigits( 3 );		
	}
	
	protected void println( String s ){ System.out.println( s ); }
	
	/**
	 * Cleanup.
	 */
	public void clear()
	{
		tiles.clear();
		fixedTiles.clear();
		
		minError = Double.MAX_VALUE;
		maxError = 0.0;
		error = Double.MAX_VALUE;
	}
	
	/**
	 * Add a single {@link Tile}.
	 * 
	 * @param t
	 */
	final public void addTile( final Tile< ? > t ){ tiles.add( t ); }
	
	/**
	 * Add a {@link Collection} of {@link Tile Tiles}.
	 * 
	 * @param t
	 */
	final public void addTiles( final Collection< ? extends Tile< ? > > t ){ tiles.addAll( t ); }
	
	/**
	 * Add all {@link Tile Tiles} of another {@link TileConfiguration}.
	 * 
	 * @param t
	 */
	final public void addTiles( final TileConfiguration t ){ tiles.addAll( t.tiles ); }
	
	/**
	 * Fix a single {@link Tile}.
	 * 
	 * @param t
	 */
	final public void fixTile( final Tile< ? > t ){ fixedTiles.add( t ); }
	
	/**
	 * Update all {@link PointMatch Correspondences} in all {@link Tile Tiles}
	 * and estimate the average displacement. 
	 */
	final private void update()
	{
		double cd = 0.0;
		minError = Double.MAX_VALUE;
		maxError = 0.0;
		for ( Tile< ? > t : tiles )
		{
			t.update();
			double d = t.getDistance();
			if ( d < minError ) minError = d;
			if ( d > maxError ) maxError = d;
			cd += d;
		}
		cd /= tiles.size();
		error = cd;
	}
	
	/**
	 * Minimize the displacement of all {@link PointMatch Correspondence pairs}
	 * of all {@link Tile Tiles}
	 * 
	 * @param maxAllowedError do not accept convergence if error is > max_error
	 * @param maxIterations stop after that many iterations even if there was
	 *   no minimum found
	 * @param maxPlateauwidth convergence is reached if the average absolute
	 *   slope in an interval of this size and half this size is smaller than
	 *   0.0001 (in double accuracy).  This is assumed to prevent the algorithm
	 *   from stopping at plateaus smaller than this value.
	 */
	public void optimize(
			final float maxAllowedError,
			final int maxIterations,
			final int maxPlateauwidth ) throws NotEnoughDataPointsException, IllDefinedDataPointsException 
	{
		final ErrorStatistic observer = new ErrorStatistic( maxPlateauwidth + 1 );
		
		int i = 0;
		
		boolean proceed = i < maxIterations;
		
		while ( proceed )
		{
			for ( final Tile< ? > tile : tiles )
			{
				if ( fixedTiles.contains( tile ) ) continue;
				tile.update();
				tile.fitModel();
				tile.update();
			}
			update();
			observer.add( error );
			
			if ( i > maxPlateauwidth )
			{
				proceed = error > maxAllowedError;
				
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
		
		println( "Successfully optimized configuration of " + tiles.size() + " tiles after " + i + " iterations:" );
		println( "  average displacement: " + decimalFormat.format( error ) + "px" );
		println( "  minimal displacement: " + decimalFormat.format( minError ) + "px" );
		println( "  maximal displacement: " + decimalFormat.format( maxError ) + "px" );
	}
}
