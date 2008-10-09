import ini.trakem2.display.*;

import ij.plugin.*;
import ij.*;

import java.util.ArrayList;
import java.awt.TextField;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import java.io.*;

import mpicbg.models.AffineModel2D;
import mpicbg.models.CoordinateTransformMap2D;

public class Generate_TrakEM2_Deformation implements PlugIn, KeyListener
{
	public void run( String args )
	{
		Display front = Display.getFront();
		if ( front == null )
		{
			System.err.println( "no open displays" );
			return;
		}
		
		final LayerSet set = front.getLayer().getParent();
		if ( set == null )
		{
			System.err.println( "no open layer-set" );
			return;
		}
		
		final ArrayList< Layer > layers = set.getLayers();
		
		final ArrayList< Displayable > patches = new ArrayList< Displayable >();
		
		for ( Layer layer : layers )
		{
			IJ.log( "Layer " + ( set.indexOf( layer ) + 1 ) + " of " + set.size() + "\n" );

			patches.clear();
			patches.addAll( layer.getDisplayables( Patch.class ) );
			
			int num_patches = patches.size();

			for ( int k = 0; k < num_patches; ++k )
			{
				final Patch patch = ( Patch )patches.get( k );
				
				final AffineModel2D model = new AffineModel2D();
				model.getAffine().setTransform( patch.getAffineTransform() );
				
				final String mapName = patch.getFilePath().replaceAll( "\\.tif$", ".r.map" );
				IJ.log( mapName + " " + ( int )patch.getWidth() + "x" + ( int )patch.getHeight() );
				
				IJ.log( "Creating transformation map.  This may take some time..." );
				final CoordinateTransformMap2D map = new CoordinateTransformMap2D( model, ( int )patch.getWidth(), ( int )patch.getHeight() );
				
				try
				{
					final FileOutputStream fos = new FileOutputStream( mapName, false );
					map.export( fos );
					fos.close();
					IJ.log( "  ...done." );
				}
				catch ( Exception ex )
				{
					IJ.error( "Error writing map-files '" + mapName + "'.\n" + ex.getMessage() );
					return;
				}
			}
		}
	}

	public void keyPressed(KeyEvent e)
	{
		if (
				( e.getKeyCode() == KeyEvent.VK_F1 ) &&
				( e.getSource() instanceof TextField ) )
		{
		}
		else if ( e.getKeyCode() == KeyEvent.VK_ESCAPE )
		{
			return;
		}
	}

	public void keyReleased(KeyEvent e) { }

	public void keyTyped(KeyEvent e) { }
}
