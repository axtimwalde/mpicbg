import java.awt.Color;
import java.awt.TextField;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.*;
import ij.io.OpenDialog;

import mpicbg.ij.InteractiveMapping;
import mpicbg.ij.TransformMapping;
import mpicbg.ij.TransformMeshMapping;
import mpicbg.models.*;

public class Mapping_Test extends InteractiveMapping
{
	public static final String NL = System.getProperty( "line.separator" );
	public final static String man =
		"Add some control points with your mouse" + NL +
		"and drag them to deform the image." + NL + " " + NL +
		"ENTER - Apply the deformation." + NL +
		"ESC - Return to the original image." + NL +
		"Y - Toggle mesh display.";
	
	// number of vertices in horizontal direction
	private static int numX = 16;
	// alpha [0 smooth, 1 less smooth ;)]
	private static float alpha = 1.0f;
	// local transformation model
	final static private String[] methods = new String[]{ "Translation", "Rigid", "Affine" };
	static private int method = 1;
	
	protected MovingLeastSquaresMesh< ? extends AbstractAffineModel2D > mesh;
	
	@Override
	final protected void createMapping()
	{
		mapping = new TransformMeshMapping( mesh );
	}
	
	@Override
	final protected void updateMapping() throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		mesh.updateModels();
		mesh.updateAffines();
		updateIllustration();
	}
	
	@Override
	final protected void addHandle( int x, int y )
	{
		float[] l = new float[]{ x, y };
		synchronized ( mesh )
		{
			InvertibleModel m = ( InvertibleModel )mesh.findClosest( l ).getModel();
			try
			{
				m.applyInverseInPlace( l );
				Point here = new Point( l );
				Point there = new Point( l );
				hooks.add( here );
				here.apply( m );
				mesh.addMatchWeightedByDistance( new PointMatch( there, here, 10f ), alpha );
			}
			catch ( NoninvertibleModelException e ){ e.printStackTrace(); }
		}	
	}
	
	@Override
	final protected void updateHandles( int x, int y )
	{
		float[] l = hooks.get( targetIndex ).getW();
	
		l[ 0 ] = x;
		l[ 1 ] = y;
	}
	
	@Override
	final public void init()
	{
		final GenericDialog gd = new GenericDialog( "Moving Least Squares Transform" );
		gd.addNumericField( "Vertices_per_row :", numX, 0 );
		//gd.addNumericField( "vertical_handles :", numY, 0 );
		gd.addNumericField( "Alpha :", alpha, 2 );
		gd.addChoice( "Local_transformation :", methods, methods[ method ] );
		gd.addCheckbox( "_Interactive_preview", showPreview );
		gd.addMessage( man );
		gd.showDialog();
		
		if (gd.wasCanceled()) return;
		
		numX = ( int )gd.getNextNumber();
		alpha = ( float )gd.getNextNumber();
		
		method = gd.getNextChoiceIndex();
		
		showPreview = gd.getNextBoolean();
		
		// TODO Implement other models for choice
		switch ( method )
		{
		case 0:
			mesh = new MovingLeastSquaresMesh< TranslationModel2D >( TranslationModel2D.class, numX, imp.getWidth(), imp.getHeight() );
			break;
		case 1:
			mesh = new MovingLeastSquaresMesh< RigidModel2D >( RigidModel2D.class, numX, imp.getWidth(), imp.getHeight() );
			break;
		case 2:
			mesh = new MovingLeastSquaresMesh< AffineModel2D >( AffineModel2D.class, numX, imp.getWidth(), imp.getHeight() );
			break;
		default:
			return;
		}
    }
	
	@Override
	final protected void updateIllustration()
	{
		if ( showIllustration )
			imp.getCanvas().setDisplayList( mesh.illustrateMesh(), Color.white, null );
		else
			imp.getCanvas().setDisplayList( null );
	}
	
	@Override
	public void keyPressed( KeyEvent e)
	{
		if ( e.getKeyCode() == KeyEvent.VK_ESCAPE || e.getKeyCode() == KeyEvent.VK_ENTER )
		{
			painter.interrupt();
			if ( imp != null )
			{
				imp.getCanvas().removeMouseListener( this );
				imp.getCanvas().removeMouseMotionListener( this );
				imp.getCanvas().removeKeyListener( this );
				imp.getCanvas().setDisplayList( null );
				imp.setRoi( ( Roi )null );
			}
			if ( e.getKeyCode() == KeyEvent.VK_ESCAPE )
			{
				imp.setProcessor( null, source );
			}
			else
			{
				//mapping.mapInterpolated( source, target );
				
				CoordinateTransformMap2D map = new CoordinateTransformMap2D( mesh, imp.getWidth(), imp.getHeight() );
				InvertibleCoordinateTransformMap2D mapInverse = new InvertibleCoordinateTransformMap2D( mesh, imp.getWidth(), imp.getHeight() );
				
				try
				{
					FileOutputStream fos = new FileOutputStream( "map.map", false );
					map.export( fos );
					fos.close();
					
					fos = new FileOutputStream( "map.inverse.map", false );
					mapInverse.export( fos );
					fos.close();
				}
				catch ( Exception ex )
				{
					IJ.error( "Error writing map-files 'map.map' and 'map.inverse.map'.\n" + ex.getMessage() );
					return;
				}
				
				File file = new File( "map.map" );
				if ( !file.exists() )
				{
					OpenDialog od = new OpenDialog( "Open map file.", null );
					String dir = od.getDirectory();
					String name = od.getFileName();
					file = new File( dir + name );
					if ( !file.exists() )
					{
						IJ.error( "File not found." );
						return;
					}
				}
				CoordinateTransformMap2D loadedMap;
				try
				{
					FileInputStream fis = new FileInputStream( file );
					loadedMap = new CoordinateTransformMap2D( fis );
				}
				catch ( Exception ex )
				{
					IJ.error( "Opening '" + file + "' as map failed.\n" + ex.getMessage() );
					return;
				}
				
				file = new File( "map.inverse.map" );
				if ( !file.exists() )
				{
					OpenDialog od = new OpenDialog( "Open inverse map file.", null );
					String dir = od.getDirectory();
					String name = od.getFileName();
					file = new File( dir + name );
					if ( !file.exists() )
					{
						IJ.error( "File not found." );
						return;
					}
				}
				InvertibleCoordinateTransformMap2D loadedInverseMap;
				try
				{
					FileInputStream fis = new FileInputStream( file );
					loadedInverseMap = new InvertibleCoordinateTransformMap2D( fis );
				}
				catch ( Exception ex )
				{
					IJ.error( "Opening '" + file + "' as inverse map failed.\n" + ex.getMessage() );
					return;
				}
				
				float[] t = new float[ 2 ];
				for ( int y = 0; y < target.getHeight(); ++y )
				{
					for ( int x = 0; x < target.getWidth(); ++x )
					{
						t[ 0 ] = x;
						t[ 1 ] = y;
						
						loadedInverseMap.applyInverseInPlace( t );
						
						target.putPixel( x, y, source.getPixelInterpolated( t[ 0 ], t[ 1 ] ) );
					}
				}
				
				ImagePlus impTransform = new ImagePlus( "transformed", target.duplicate() );
				impTransform.show();
				
				source = target.duplicate();
				for ( int y = 0; y < target.getHeight(); ++y )
				{
					for ( int x = 0; x < target.getWidth(); ++x )
					{
						t[ 0 ] = x;
						t[ 1 ] = y;
						
						loadedMap.applyInPlace( t );
						
						target.putPixel( x, y, source.getPixelInterpolated( t[ 0 ], t[ 1 ] ) );
					}
				}
				
				ImagePlus impInverseTransform = new ImagePlus( "re-transformed", target.duplicate() );
				impInverseTransform.show();
				
				imp.updateAndDraw();
			}
		}
		else if ( e.getKeyCode() == KeyEvent.VK_Y )
		{
			showIllustration = !showIllustration;
			updateIllustration();			
		}
		else if (
				( e.getKeyCode() == KeyEvent.VK_F1 ) &&
				( e.getSource() instanceof TextField ) ){}
	}
}
