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
import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import mpicbg.models.AffineModel2D;
import mpicbg.models.ErrorStatistic;
import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import mpicbg.models.RigidModel2D;
import mpicbg.models.Tile;

/**
 * 
 * 
 *
 */
public class ElasticMovingLeastSquaresMesh extends MovingLeastSquaresMesh
{
	final protected HashSet< Tile > fixedTiles = new HashSet< Tile >();
	
	final static private DecimalFormat decimalFormat = new DecimalFormat();
	final static private DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
	
	protected float alpha;
	
	public ElasticMovingLeastSquaresMesh(
			final int numX,
			final int numY,
			final float width,
			final float height,
			final Class< ? extends Model > modelClass,
			final float alpha )
	{
		super( numX, numY, width, height, modelClass );
		
		decimalFormatSymbols.setGroupingSeparator( ',' );
		decimalFormatSymbols.setDecimalSeparator( '.' );
		decimalFormat.setDecimalFormatSymbols( decimalFormatSymbols );
		decimalFormat.setMaximumFractionDigits( 3 );
		decimalFormat.setMinimumFractionDigits( 3 );
		
		this.alpha = alpha;
		
		Set< PointMatch > s = va.keySet();
		
		//float w = 1.0f / s.size();
		// temporary weights for inter-vertex PointMatches
		float[] w = new float[ 2 ];
		w[ 0 ] = 100.0f / s.size();
		
		for ( PointMatch vertex : s )
		{
			/**
			 * For each vertex, collect its connected vertices.
			 */
			HashSet< PointMatch > connectedVertices = new HashSet< PointMatch >();
			for ( AffineModel2D ai : va.get( vertex ) )
			{
				for ( PointMatch m : av.get( ai ) )
				{
					if ( vertex != m ) connectedVertices.add( m );
				}
			}
			
			/**
			 * Add PointMatches for each connectedVertex.
			 * These PointMatches work as "regularizers" for the mesh, that is
			 * the mesh tries to stay rigid.  The influence of these intra-stability
			 * points is given by weighting factors.
			 * 
			 * TODO
			 *   Currently, we assign two weights to each match:
			 *   1. a constant weight that defines the "stiffness" of the mesh
			 *   2. a weigh that depends on the distance to the vertex similar
			 *      to the moving least squares. 
			 * 
			 *   Outdated:
			 * 	 Currently we assign a weight of 1 / number of vertices to
			 *   each vertex.  That represents a "density" related weight
			 *   assuming the image has an area of 1.  This will give different
			 *   results for square and non-square images.
			 *   
			 *   Should we use min( number of vertical, number of horizontal)^2
			 *   instead?
			 */
			Tile t = pt.get( vertex );
			for ( PointMatch m : connectedVertices )
			{
				Tile o = pt.get( m );
				Point p2 = m.getP2();
				Point p1 = new Point( p2.getW().clone() );
				
				w[ 1 ] = weigh( Point.squareDistance( vertex.getP1(), p2 ), alpha );
				
				/*
				 * non weighted match
				 */
//				t.addMatch( new PointMatch( p1, p2 ) );
				
				/*
				 * weighted match
				 */
				t.addMatch( new PointMatch( p1, p2, w, 1.0f ) );				
				t.addConnectedTile( o );
			}
		}
	}
	
	public ElasticMovingLeastSquaresMesh( int numX, float width, float height, Class< ? extends Model > modelClass, float alpha )
	{
		this( numX, numY( numX, width, height ), width, height, modelClass, alpha );
	}
	
	
	/**
	 * Update all PointMatches in all tiles and estimate the average
	 * displacement. 
	 */
	final private void update( float amount )
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
	final public void updateByStrength( float amount )
	{
		Set< PointMatch > s = va.keySet();
		
		for ( PointMatch m : s )
		{
			Tile t = pt.get( m );
			
			/**
			 * Update the location of the vertex
			 */
			m.getP2().apply( t.getModel(), amount );
			
			/**
			 * Update the tile
			 */
			t.updateByStrength( amount );
		}
		
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
	public final void optimizeIteration( ErrorStatistic observer ) throws NotEnoughDataPointsException
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
			 * Update the location of the vertex
			 */
			m.getP2().apply( t.getModel() );
			
			error += t.getDistance();
			updateAffine( m );
		}
		error /= s.size();
		
		observer.add( error );
	}
	
	/**
	 * Performs one optimization iteration.
	 * 
	 * @throws NotEnoughDataPointsException
	 */
	final public void optimizeIteration() throws NotEnoughDataPointsException
	{
		Set< PointMatch > s = va.keySet();
		
		for ( PointMatch m : s )
		{
			Tile t = pt.get( m );
			if ( fixedTiles.contains( t ) ) continue;
			
			t.fitModel();
		}
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
			optimizeIteration();
			updateByStrength( 0.75f );
			observer.add( error );
			
			ipPlot.set(
					( int )( i * ( float )ipPlot.getWidth() / ( float )maxIterations ),
					//( int )( ( double )observer.values.get( observer.values.size() - 1 ) ),
					Math.min( ipPlot.getHeight() - 1, Math.max( 0, ipPlot.getHeight() / 2 - ( int )( Math.log( ( double )observer.values.get( observer.values.size() - 1 ) ) * 10 ) ) ),
					//( int )( 10 * error ),
					0 );
			impPlot.updateAndDraw();
			
			if ( i >= maxPlateauwidth && error < maxError &&
					Math.abs( observer.getWideSlope( maxPlateauwidth ) ) <= 0.0001 &&
					Math.abs( observer.getWideSlope( maxPlateauwidth / 2 ) ) <= 0.0001 )
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
