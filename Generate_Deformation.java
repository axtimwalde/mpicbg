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
import ij.plugin.PlugIn;
import ij.process.*;
import ij.gui.*;
import ij.io.FileSaver;
import ij.io.SaveDialog;

import mpicbg.models.*;

import java.awt.geom.AffineTransform;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Random;

public class Generate_Deformation implements PlugIn
{
	static protected int tileSize = 64;
	// number of tiles in horizontal direction
	private static int numCols = 4;
	// number of tiles in vertical direction
	private static int numRows = 4;
	// tile overlap as a fraction of 1
	private static float tileOverlap = 0.1f;
	// odometry error as a fraction of 1
	private static float odometryError = 0.05f;
	// number of vertices in horizontal direction
	private static int numVerticesX = 32;
	// coarse number of vertices in horizontal direction (visualisation)
	private static int numVerticesXCoarse = 8;
	// alpha [0 smooth, 1 less smooth ;)]
	private static float alpha = 1.0f;
	// global pseudo random seed
	private static int globalRndSeed = 69997;
	// local pseudo random seed
	private static int localRndSeed = 69997;
	// maximal global deformation in pixel
	private static int maxGlobalDeform = 8;
	// maximal local deformation in pixel
	private static int maxLocalDeform = 4;
	// local transformation model
	final static String[] methods = new String[]{ "Translation", "Rigid", "Affine" };
	private static int method = 2;
	
	static private String rawFileName;
	
	//final ArrayList< PointMatch > pq = new ArrayList< PointMatch >();
	final static protected ArrayList< Point > hooks = new ArrayList< Point >();
	static protected PointRoi handles;
	
	static protected Random rndShift;
	static protected Random rndGlobalHandles;
	
	static protected Point[] globalLocalHandles = new Point[ numCols * numRows ];
	static protected Point[] globalWorldHandles = globalLocalHandles.clone();
	
	static int globalWidth;
	static int globalHeight;
	
	static protected ImagePlus imp;
	
	final static RigidModel2D r = new RigidModel2D();
	
	static String directory;
	static String name;
	
	final String arrowSVG()
	{
		float pd = -tileSize / 10;
		
		String a = "M " + pd + " " + pd + " ";
		a += "L " + ( 4 * pd ) + " " + ( 2.5 * pd ) + " ";
		a += "L " + ( 2.75 * pd ) + " " + ( 2.75 * pd ) + " ";
		a += "L " + ( 2.5 * pd ) + " " + ( 4 * pd ) + " Z";
		
		return a;
	}
	
	final protected void createDeformedTiles(
			final StringBuffer deformed,
			final StringBuffer rigid,
			final ArrayList< ImageProcessor > images,
			final ArrayList< CoordinateTransformMap2D > maps )
	{
		for ( int y = 0; y < numRows; ++y )
		{
			for ( int x = 0; x < numCols; ++x )
			{
				IJ.log( x + ", " + y );
				final MovingLeastSquaresMesh< ? extends AbstractAffineModel2D > mesh;				
				final MovingLeastSquaresMesh< ? extends AbstractAffineModel2D > meshCoarse;				
				final CoordinateTransformList transformList = new CoordinateTransformList();
				final ImageProcessor ip = imp.getProcessor().createProcessor( tileSize, tileSize );
				
				switch ( method )
				{
				case 0:
					mesh = new MovingLeastSquaresMesh< TranslationModel2D >( TranslationModel2D.class, numVerticesX, tileSize, tileSize );
					meshCoarse = new MovingLeastSquaresMesh< TranslationModel2D >( TranslationModel2D.class, numVerticesXCoarse, tileSize, tileSize );
					break;
				case 1:
					mesh = new MovingLeastSquaresMesh< RigidModel2D >( RigidModel2D.class, numVerticesX, tileSize, tileSize );
					meshCoarse = new MovingLeastSquaresMesh< RigidModel2D >( RigidModel2D.class, numVerticesXCoarse, tileSize, tileSize );
					break;
				case 2:
					mesh = new MovingLeastSquaresMesh< AffineModel2D >( AffineModel2D.class, numVerticesX, tileSize, tileSize );
					meshCoarse = new MovingLeastSquaresMesh< AffineModel2D >( AffineModel2D.class, numVerticesXCoarse, tileSize, tileSize );
					break;
				default:
					return;
				}
				
				for ( int i = 0; i < globalLocalHandles.length; ++i )
				{
					// local per tile deformation
					globalWorldHandles[ i ].getL()[ 0 ] += ( rndGlobalHandles.nextFloat() - 0.5f ) * 2 * maxLocalDeform;
					globalWorldHandles[ i ].getL()[ 1 ] += ( rndGlobalHandles.nextFloat() - 0.5f ) * 2 * maxLocalDeform;
					
					globalWorldHandles[ i ].getW()[ 0 ] = globalWorldHandles[ i ].getL()[ 0 ];
					globalWorldHandles[ i ].getW()[ 1 ] = globalWorldHandles[ i ].getL()[ 1 ];
					
					mesh.addMatchWeightedByDistance( new PointMatch( globalWorldHandles[ i ], globalLocalHandles[ i ], 1f ), alpha );
					meshCoarse.addMatchWeightedByDistance( new PointMatch( globalWorldHandles[ i ], globalLocalHandles[ i ], 1f ), alpha );
				}
				try
				{
					mesh.updateModels();
					mesh.updateAffines();
					meshCoarse.updateModels();
					meshCoarse.updateAffines();
				}
				catch ( NotEnoughDataPointsException ex ){ ex.printStackTrace(); }
				catch ( IllDefinedDataPointsException ex ){ ex.printStackTrace(); }
				
				final TransformMeshMap2D meshMap = new TransformMeshMap2D( mesh, tileSize, tileSize );
				transformList.add( meshMap );
				transformList.add( r );
				
				IJ.log( "Creating transformation map.  This may take some time..." );
				final CoordinateTransformMap2D map = new CoordinateTransformMap2D( transformList, tileSize, tileSize );
				IJ.log( "  ...done." );
				
				float[] t = new float[ 2 ];
				for ( int yi = 0; yi < tileSize; ++yi )
				{
					for ( int xi = 0; xi < tileSize; ++xi )
					{
						t[ 0 ] = xi;
						t[ 1 ] = yi;
						
						map.applyInPlace( t );
						
						ip.putPixel( xi, yi, imp.getProcessor().getPixelInterpolated( t[ 0 ], t[ 1 ] ) );
					}
				}
//				final ImagePlus impTile = new ImagePlus( x + " " + y, ip );
//				impTile.show();
//				
				images.add( ip );
				maps.add( map );
				
				deformed.append( meshCoarse.illustrateMeshSVG() );
				rigid.append( mesh.illustrateBestRigidSVG() );
				
				float xShift = tileSize * ( 1.0f - tileOverlap );
				xShift += ( rndShift.nextFloat() - 0.5f ) * 2 * xShift * odometryError;
				
				float yShift = ( rndShift.nextFloat() - 0.5f ) * 2 * xShift * odometryError;
				
				for ( int i = 0; i < globalLocalHandles.length; ++i )
				{
					globalWorldHandles[ i ].getL()[ 0 ] -= xShift;
					globalWorldHandles[ i ].getL()[ 1 ] -= yShift;
					globalWorldHandles[ i ].getW()[ 0 ] -= xShift;
					globalWorldHandles[ i ].getW()[ 1 ] -= yShift;
				}
			}
			
			float xShift = -numCols * tileSize * ( 1.0f - tileOverlap );
			xShift += ( rndShift.nextFloat() - 0.5f ) * 2 * xShift * odometryError;
			
			float yShift = tileSize * ( 1 - tileOverlap );
			yShift += ( rndShift.nextFloat() - 0.5f ) * 2 * yShift * odometryError;
			
			for ( int i = 0; i < globalLocalHandles.length; ++i )
			{
				globalWorldHandles[ i ].getL()[ 0 ] -= xShift;
				globalWorldHandles[ i ].getL()[ 1 ] -= yShift;
				globalWorldHandles[ i ].getW()[ 0 ] -= xShift;
				globalWorldHandles[ i ].getW()[ 1 ] -= yShift;
				
				// local per tile deformation
				//globalWorldHandles[ i ].getW()[ 0 ] += ( rndGlobalHandles.nextFloat() - 0.5f ) * 2 * maxLocalDeform;
				//globalWorldHandles[ i ].getW()[ 1 ] += ( rndGlobalHandles.nextFloat() - 0.5f ) * 2 * maxLocalDeform;
			}
		}
	}
	
	final protected void createGlobalDeformation()
	{
		final double maxTranslationX = Math.max( 0.0f, globalWidth - tileSize * numCols + tileSize * tileOverlap * ( numCols - 1 ) );
		final double maxTranslationY = Math.max( 0.0f, globalHeight - tileSize * numRows + tileSize * tileOverlap * ( numRows - 1 ) );
		
		final AffineTransform a = r.getAffine();
		a.setToIdentity();
		a.translate(
				( 0.5 - rndGlobalHandles.nextFloat() ) * maxTranslationX / 4,
				( 0.5 - rndGlobalHandles.nextFloat() ) * maxTranslationY / 4 );
		a.rotate( Math.PI * 2 * rndGlobalHandles.nextFloat(), globalWidth / 2, globalHeight / 2 );
		a.translate(
				maxTranslationX / 2,
				maxTranslationY / 2 );
		
		globalLocalHandles = new Point[ numCols * numRows ];
		globalWorldHandles = globalLocalHandles.clone();
		
		for ( int y = 0; y < numRows; ++y )
		{
			for ( int x = 0; x < numCols; ++x )
			{
				int i = y * numCols + x;
				
				// seed one global deformation handle per tile
				float[] l = new float[]{ tileSize * ( 1.0f - tileOverlap ) * x + tileSize / 2, tileSize * ( 1.0f - tileOverlap ) * y + tileSize / 2 };
				l[ 0 ] += ( rndGlobalHandles.nextFloat() - 0.5f ) * maxGlobalDeform;
				l[ 1 ] += ( rndGlobalHandles.nextFloat() - 0.5f ) * maxGlobalDeform;
				
				globalLocalHandles[ i ] = new Point( l );
				globalWorldHandles[ i ] = new Point( l );
				
				// move it randomly
				globalLocalHandles[ i ].getW()[ 0 ] += ( rndGlobalHandles.nextFloat() - 0.5f ) * 2 * maxGlobalDeform;
				globalLocalHandles[ i ].getW()[ 1 ] += ( rndGlobalHandles.nextFloat() - 0.5f ) * 2 * maxGlobalDeform;
			}
		}
	}
	
	public void run( String arg )
    {
		imp = IJ.getImage();
		rawFileName = imp.getShortTitle();
		
		globalWidth = imp.getWidth();
		globalHeight = imp.getHeight();
		
		final GenericDialog gd = new GenericDialog( "Create Deformed Tiles" );
		gd.addNumericField( "Tile_size :", tileSize, 0 );
		gd.addNumericField( "Tile_columns :", numCols, 0 );
		gd.addNumericField( "Tile_rows :", numRows, 0 );
		gd.addNumericField( "Tile_overlap :", tileOverlap, 2 );
		gd.addNumericField( "Odometry_error :", odometryError, 2 );
		gd.addNumericField( "Vertices_per_row :", numVerticesX, 0 );
		gd.addNumericField( "Visual_vertices_per_row :", numVerticesXCoarse, 0 );
		gd.addNumericField( "Alpha :", alpha, 2 );
		gd.addNumericField( "Global_pseudo_random_seed :", globalRndSeed, 0 );
		gd.addNumericField( "Local_pseudo_random_seed :", localRndSeed, 0 );
		gd.addNumericField( "Maximum_global_deformation :", maxGlobalDeform, 0 );
		gd.addNumericField( "Maximum_local_deformation :", maxLocalDeform, 0 );
		gd.addChoice( "Local_transformation :", methods, methods[ method ] );
		gd.showDialog();
		
		if (gd.wasCanceled()) return;
		
		final SaveDialog sd = new SaveDialog( "Save as ...", rawFileName, ".tif" );
//		final SaveDialog sd = new SaveDialog( "Save as ...", rawFileName, ".svg" );
		directory = sd.getDirectory();
		name = sd.getFileName();
		rawFileName = name.replaceAll( "\\.tif$", "" );
//		rawFileName = name.replaceAll( "\\.svg$", "" );

		if ( name == null || name == "" ) 
		{
			IJ.error( "No filename selected." );
			return;
		}
				
		String svgName = rawFileName + ".svg";
		
		tileSize = ( int )gd.getNextNumber();
		numCols = ( int )gd.getNextNumber();
		numRows = ( int )gd.getNextNumber();
		tileOverlap = ( float )gd.getNextNumber();
		odometryError = ( float )gd.getNextNumber();
		numVerticesX = ( int )gd.getNextNumber();
		numVerticesXCoarse = ( int )gd.getNextNumber();
		alpha = ( float )gd.getNextNumber();
		globalRndSeed = ( int )gd.getNextNumber();
		localRndSeed = ( int )gd.getNextNumber();
		maxGlobalDeform = ( int )gd.getNextNumber();
		maxLocalDeform = ( int )gd.getNextNumber();
		method = gd.getNextChoiceIndex();
		
		rndShift = new Random( localRndSeed );
		rndGlobalHandles = new Random( localRndSeed + 1000 );
		
		createGlobalDeformation();
		
		final StringBuffer deformed = new StringBuffer();
		final StringBuffer rigid = new StringBuffer();
		final ArrayList< ImageProcessor > images = new ArrayList< ImageProcessor >();
		final ArrayList< CoordinateTransformMap2D > maps = new ArrayList< CoordinateTransformMap2D >();
		
		createDeformedTiles( deformed, rigid, images, maps );
		
		try
		{
			InputStream is = getClass().getResourceAsStream( "deformation.tpl.svg" );
			byte[] bytes = new byte[ is.available() ];
			is.read( bytes );
			String svg = new String( bytes );
			
			svg = svg.replaceAll( "<!--global_width-->", "" + globalWidth );
			svg = svg.replaceAll( "<!--global_height-->", "" + globalHeight );
			
			svg = svg.replaceAll( "<!--image_path-->", imp.getOriginalFileInfo().directory + imp.getOriginalFileInfo().fileName );
			
			final double[] a = new double[ 6 ];
			r.getAffine().getMatrix( a );
			
			svg = svg.replaceAll( "<!--rigid_transform-->", a[ 0 ] + " " + a[ 1 ] + " " + a[ 2 ] + " " + a[ 3 ] + " " + a[ 4 ] + " " + a[ 5 ] );
			
			svg = svg.replaceAll( "<!--deformed_grid-->", deformed.toString() );
			svg = svg.replaceAll( "<!--rigid_grid-->", rigid.toString() );
			
			svg = svg.replaceAll( "<!--arrow-->", arrowSVG() );
			
			svg = svg.replaceAll( "stroke-width:1px;", "stroke-width:" + ( tileSize / 128 ) + "px;" );
			
			PrintStream ps = new PrintStream( directory + svgName ); 
			ps.print( svg );
			ps.close();
			
			for ( int i = 0; i < images.size(); ++i )
			{
				IJ.log( "Saving tile " + i + "..." );
				
				final ImageProcessor ip = images.get( i );
				final CoordinateTransformMap2D map = maps.get( i );
				
				final String tileName = rawFileName + "_" + i;
				
				final ImagePlus impTile = new ImagePlus( tileName, ip );
				final FileSaver fs = new FileSaver( impTile );
				fs.saveAsTiff( directory + tileName + ".tif" );
				try
				{
					final FileOutputStream fos = new FileOutputStream( directory + tileName + ".map", false );
					map.export( fos );
					fos.close();
					IJ.log( "  ...done." );
				}
				catch ( Exception ex )
				{
					IJ.error( "Error writing map-files '" + directory + tileName + ".map'.\n" + ex.getMessage() );
					return;
				}
			}
		}
		catch ( Exception e )
		{
			IJ.error( "Error writing svg-file '" + svgName + "'.\n" + e.getMessage() );
		}
    }
}
