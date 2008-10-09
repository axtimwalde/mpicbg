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
import ij.plugin.PlugIn;
import ij.gui.*;
import ij.io.SaveDialog;

import mpicbg.models.*;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Random;

public class SVG_Figure implements PlugIn
{
	public static final String NL = System.getProperty( "line.separator" );
	public final static String man =
		"Add some control points with your mouse" + NL +
		"and drag them to deform the image." + NL + " " + NL +
		"ENTER - Apply the deformation." + NL +
		"ESC - Return to the original image." + NL +
		"Y - Toggle mesh display.";
	
	private final static float TILE_SIZE = 204.8f;
	// number of tiles in horizontal direction
	private static int numCols = 5;
	// number of tiles in vertical direction
	private static int numRows = 5;
	// number of vertices in horizontal direction
	private static int numX = 16;
	// alpha [0 smooth, 1 less smooth ;)]
	private static float alpha = 1.0f;
	// pseudo random seed
	private static int rndSeed = 69997;
	// local transformation model
	final static String[] methods = new String[]{ "Translation", "Rigid", "Affine" };
	private static int method = 1;
	
	private static String rawFileName = "figure";
	
	//final ArrayList< PointMatch > pq = new ArrayList< PointMatch >();
	final ArrayList< Point > hooks = new ArrayList< Point >();
	PointRoi handles;
	
	int targetIndex = -1;
	
	boolean showMesh = false;
	
	public void run( String arg )
    {
		hooks.clear();
		
		final GenericDialog gd = new GenericDialog( "Create SVG Figure" );
		gd.addNumericField( "Tile_columns :", numCols, 0 );
		gd.addNumericField( "Tile_rows :", numRows, 0 );
		gd.addNumericField( "Vertices_per_row :", numX, 0 );
		//gd.addNumericField( "vertical_handles :", numY, 0 );
		gd.addNumericField( "Alpha :", alpha, 2 );
		gd.addNumericField( "Pseudo_random_seed :", rndSeed, 0 );
		gd.addChoice( "Local_transformation :", methods, methods[ method ] );
		gd.addMessage( man );
		gd.showDialog();
		
		if (gd.wasCanceled()) return;
		
		SaveDialog sd = new SaveDialog( "Save as ...", rawFileName, ".svg" );
		String directory = sd.getDirectory();
		String name = sd.getFileName();
		rawFileName = name.replaceAll( "\\.svg$", "" );

		if ( name == null || name == "" ) 
		{
			IJ.error( "No filename selected." );
			return;
		}
				
		String fileName = directory + name;
		
		numCols = ( int )gd.getNextNumber();
		numRows = ( int )gd.getNextNumber();
		numX = ( int )gd.getNextNumber();
		alpha = ( float )gd.getNextNumber();
		rndSeed = ( int )gd.getNextNumber();
		method = gd.getNextChoiceIndex();
		
		final Random rndShift = new Random( rndSeed );
		final Random rndGlobalHandles = new Random( rndSeed + 1000 );
		
		Point[] globalLocalHandles = new Point[ numCols * numRows ];
		Point[] globalWorldHandles = globalLocalHandles.clone();
		
		float width = TILE_SIZE * numCols - TILE_SIZE * 0.1f * ( numCols - 1 );
		float height = TILE_SIZE * numRows - TILE_SIZE * 0.1f * ( numRows - 1 );
		
		for ( int y = 0; y < numRows; ++y )
		{
			for ( int x = 0; x < numCols; ++x )
			{
				int i = y * numCols + x;
				
//				float[] l = new float[]{
//					rndGlobalHandles.nextFloat() * width,
//					rndGlobalHandles.nextFloat() * height };
//			
				float[] l = new float[]{ TILE_SIZE * 0.9f * x + TILE_SIZE / 2, TILE_SIZE * 0.9f * y + TILE_SIZE / 2 };
				l[ 0 ] += ( rndGlobalHandles.nextFloat() - 0.5f ) * TILE_SIZE * 0.25f;
				l[ 1 ] += ( rndGlobalHandles.nextFloat() - 0.5f ) * TILE_SIZE * 0.25f;
				
				globalLocalHandles[ i ] = new Point( l );
				globalWorldHandles[ i ] = new Point( l );
				
				globalLocalHandles[ i ].getW()[ 0 ] += ( rndGlobalHandles.nextFloat() - 0.5f ) * width * 0.05f;
				globalLocalHandles[ i ].getW()[ 1 ] += ( rndGlobalHandles.nextFloat() - 0.5f ) * height * 0.05f;
			}
		}
		
		String g = "";
		
		for ( int y = 0; y < numRows; ++y )
		{
			for ( int x = 0; x < numRows; ++x )
			{
				MovingLeastSquaresMesh< ? extends AbstractAffineModel2D > mesh;
				
				switch ( method )
				{
				case 0:
					mesh = new MovingLeastSquaresMesh< TranslationModel2D >( TranslationModel2D.class, numX, TILE_SIZE, TILE_SIZE );
					break;
				case 1:
					mesh = new MovingLeastSquaresMesh< RigidModel2D >( RigidModel2D.class, numX, TILE_SIZE, TILE_SIZE );
					break;
				case 2:
					mesh = new MovingLeastSquaresMesh< AffineModel2D >( AffineModel2D.class, numX, TILE_SIZE, TILE_SIZE );
					break;
				default:
					return;
				}
				
				for ( int i = 0; i < globalLocalHandles.length; ++i )
				{
					mesh.addMatchWeightedByDistance( new PointMatch( globalWorldHandles[ i ], globalLocalHandles[ i ], 1f ), alpha );
				}
				try
				{
					mesh.updateModels();
				}
				catch ( NotEnoughDataPointsException ex ){ ex.printStackTrace(); }
				catch ( IllDefinedDataPointsException ex ){ ex.printStackTrace(); }
				
				g += mesh.illustrateMeshSVG();
				g += mesh.illustrateBestRigidSVG();
				
				float xShift = TILE_SIZE * 0.9f;
				xShift += ( rndShift.nextFloat() - 0.5f ) * TILE_SIZE * 0.1f;
				
				float yShift = ( rndShift.nextFloat() - 0.5f ) * TILE_SIZE * 0.1f;
				
				for ( int i = 0; i < globalLocalHandles.length; ++i )
				{
					globalWorldHandles[ i ].getL()[ 0 ] -= xShift;
					globalWorldHandles[ i ].getL()[ 1 ] -= yShift;
					globalWorldHandles[ i ].getW()[ 0 ] -= xShift;
					globalWorldHandles[ i ].getW()[ 1 ] -= yShift;
				}
			}
			
			float xShift = -width;
			xShift += ( rndShift.nextFloat() - 0.5f ) * width * 0.1f;
			
			float yShift = TILE_SIZE * 0.9f;
			yShift += ( rndShift.nextFloat() - 0.5f ) * TILE_SIZE * 0.1f;
			
			for ( int i = 0; i < globalLocalHandles.length; ++i )
			{
				globalWorldHandles[ i ].getL()[ 0 ] -= xShift;
				globalWorldHandles[ i ].getL()[ 1 ] -= yShift;
				globalWorldHandles[ i ].getW()[ 0 ] -= xShift;
				globalWorldHandles[ i ].getW()[ 1 ] -= yShift;	
			}
		}
		
		try
		{
			InputStream is = getClass().getResourceAsStream( "template.svg" );
			byte[] bytes = new byte[ is.available() ];
			is.read( bytes );
			String svg = new String( bytes );
			svg = svg.replaceAll( "<!--g-->", g );
			
			IJ.log( svg );
			
			PrintStream ps = new PrintStream( fileName ); 
			ps.print( svg );
			ps.close();
		}
		catch ( Exception e )
		{
			IJ.error( "Error writing svg-file '" + fileName + "'.\n" + e.getMessage() );
		}
    }
}
