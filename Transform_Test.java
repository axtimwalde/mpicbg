import java.awt.TextField;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import ij.IJ;
import ij.gui.*;
import ij.io.OpenDialog;

import mpicbg.ij.InteractiveInvertibleCoordinateTransform;
import mpicbg.models.*;

public class Transform_Test extends InteractiveInvertibleCoordinateTransform< HomographyModel2D >
{
	final protected HomographyModel2D model = new HomographyModel2D();
	
	@Override
	final protected HomographyModel2D myModel(){ return model; }
	
	@Override
	final protected void setHandles()
	{
		int[] x = new int[]{ target.getWidth() / 4, 3 * target.getWidth() / 4, 3 * target.getWidth() / 4, target.getWidth() / 4 };
		int[] y = new int[]{ target.getHeight() / 4, target.getHeight() / 4, 3 * target.getHeight() / 4, 3 * target.getHeight() / 4 };
		
		p = new Point[]{
				new Point( new float[]{ ( float )x[ 0 ], ( float )y[ 0 ] } ),
				new Point( new float[]{ ( float )x[ 1 ], ( float )y[ 1 ] } ),
				new Point( new float[]{ ( float )x[ 2 ], ( float )y[ 2 ] } ),
				new Point( new float[]{ ( float )x[ 3 ], ( float )y[ 3 ] } ) };
		
		q = new Point[]{
				p[ 0 ].clone(),
				p[ 1 ].clone(),
				p[ 2 ].clone(),
				p[ 3 ].clone() };
		
		m.add( new PointMatch( p[ 0 ], q[ 0 ] ) );
		m.add( new PointMatch( p[ 1 ], q[ 1 ] ) );
		m.add( new PointMatch( p[ 2 ], q[ 2 ] ) );
		m.add( new PointMatch( p[ 3 ], q[ 3 ] ) );
		
		handles = new PointRoi( x, y, 4 );
		imp.setRoi( handles );
	}
	
	@Override
	final protected void updateHandles( int x, int y )
	{
		float[] fq = q[ targetIndex ].getW();
			
		int[] rx = new int[ q.length ];
		int[] ry = new int[ q.length ];
			
		for ( int i = 0; i < q.length; ++i )
		{
			rx[ i ] = ( int )q[ i ].getW()[ 0 ];
			ry[ i ] = ( int )q[ i ].getW()[ 1 ];
		}
				
		rx[ targetIndex ] = x;
		ry[ targetIndex ] = y;
				
		handles = new PointRoi( rx, ry, 4 );
		imp.setRoi( handles );
				
		fq[ 0 ] = x;
		fq[ 1 ] = y;
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
				mapping.mapInterpolated( source, target );
				
				source = target.duplicate();
				
				CoordinateTransformMap2D map = new CoordinateTransformMap2D( model, imp.getWidth(), imp.getHeight() );
				
				try
				{
					FileOutputStream fos = new FileOutputStream( "map.map", false );
					map.export( fos );
					fos.close();
				}
				catch ( Exception ex )
				{
					IJ.error( "Error writing map-file 'map.map'.\n" + ex.getMessage() );
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
				
				float[] t = new float[ 2 ];
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
				
				imp.updateAndDraw();
			}
		}
		else if (
				( e.getKeyCode() == KeyEvent.VK_F1 ) &&
				( e.getSource() instanceof TextField ) ){}
	}
}
