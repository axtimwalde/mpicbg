import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;
import ij.process.*;
import ij.gui.*;

import mpicbg.ij.TransformMeshMapping;
import mpicbg.models.*;

import java.awt.Event;
import java.awt.TextField;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;
import java.util.Set;

public class Transform_TriangularGrid implements PlugIn, MouseListener,  MouseMotionListener, KeyListener
{
	// number of x-handles
	private static int numX = 4;
	// number of x-handles
	private static int numY = 3;
	
	ImagePlus imp;
	ImageProcessor ip;
	ImageProcessor ipOrig;
	
	protected TransformMeshMapping mapping; 
	
	PointMatch[] pq;
	int[] x;
	int[] y;
	PointRoi handles;
	
	protected TransformMesh mt;
	
	int targetIndex = -1;
	
	public void run( String arg )
    {
		
		imp = IJ.getImage();
		ip = imp.getProcessor();
		ipOrig = ip.duplicate();
		
		GenericDialog gd = new GenericDialog( "Grid Transform" );
		gd.addNumericField( "horizontal_handles :", numX, 0 );
		gd.addNumericField( "vertical_handles :", numY, 0 );
		gd.showDialog();
		
		if (gd.wasCanceled()) return;
		
		numX = ( int )gd.getNextNumber();
		numY = ( int )gd.getNextNumber();
		
		// intitialize the transform mesh
		mt = new TransformMesh( numX, numY, imp.getWidth(), imp.getHeight() );
		
		mapping = new TransformMeshMapping( mt );
		
		Set< PointMatch > pqs = mt.getVA().keySet();
		pq = new PointMatch[ pqs.size() ];
		pqs.toArray( pq );
		
		System.out.println( pq.length );
		
		x = new int[ pq.length ];
		y = new int[ pq.length ];
		
		for ( int i = 0; i < pq.length; ++i )
		{
			x[ i ] = ( int )pq[ i ].getP2().getW()[ 0 ];
			y[ i ] = ( int )pq[ i ].getP2().getW()[ 1 ];
		}
		
		handles = new PointRoi( x, y, x.length );
		imp.setRoi( handles );
		
		Toolbar.getInstance().setTool( Toolbar.getInstance().addTool( "Drag_the_handles." ) );
		
		imp.getCanvas().addMouseListener( this );
		imp.getCanvas().addMouseMotionListener( this );
		imp.getCanvas().addKeyListener( this );
    }
	
	public void keyPressed( KeyEvent e)
	{
		if ( e.getKeyCode() == KeyEvent.VK_ESCAPE || e.getKeyCode() == KeyEvent.VK_ENTER )
		{
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
				imp.setProcessor( null, ipOrig );
			}
		}
		else if (
				( e.getKeyCode() == KeyEvent.VK_F1 ) &&
				( e.getSource() instanceof TextField ) ){}
	}

	public void keyReleased( KeyEvent e ){}
	public void keyTyped( KeyEvent e ){}
	
	public void mousePressed( MouseEvent e )
	{
		targetIndex = -1;
		if ( e.getButton() == MouseEvent.BUTTON1 )
		{
			ImageWindow win = WindowManager.getCurrentWindow();
			int x = win.getCanvas().offScreenX( e.getX() );
			int y = win.getCanvas().offScreenY( e.getY() );
			
			double target_d = Double.MAX_VALUE;
			for ( int i = 0; i < pq.length; ++i )
			{
				double dx = win.getCanvas().getMagnification() * ( pq[ i ].getP2().getW()[ 0 ] - x );
				double dy = win.getCanvas().getMagnification() * ( pq[ i ].getP2().getW()[ 1 ]  - y );
				double d =  dx * dx + dy * dy;
				if ( d < 64.0 && d < target_d )
				{
					targetIndex = i;
					target_d = d;
				}
			}
		}
		
		//IJ.log( "Mouse pressed: " + x + ", " + y + " " + modifiers( e.getModifiers() ) );
	}

	public void mouseReleased( MouseEvent e ){}
	public void mouseExited( MouseEvent e ) {}
	public void mouseClicked( MouseEvent e ) {}	
	public void mouseEntered( MouseEvent e ) {}
	
	public void mouseDragged( MouseEvent e )
	{
		if ( targetIndex >= 0 )
		{
			ImageWindow win = WindowManager.getCurrentWindow();
			int x = win.getCanvas().offScreenX( e.getX() );
			int y = win.getCanvas().offScreenY( e.getY() );
			
			float[] fq = pq[ targetIndex ].getP2().getW();
			
			int[] rx = new int[ pq.length ];
			int[] ry = new int[ pq.length ];
			
			for ( int i = 0; i < pq.length; ++i )
			{
				rx[ i ] = ( int )pq[ i ].getP2().getW()[ 0 ];
				ry[ i ] = ( int )pq[ i ].getP2().getW()[ 1 ];
			}
				
			rx[ targetIndex ] = x;
			ry[ targetIndex ] = y;
			
			handles = new PointRoi( rx, ry, pq.length );
			imp.setRoi( handles );
				
			fq[ 0 ] = x;
			fq[ 1 ] = y;
			
			mt.updateAffine( pq[ targetIndex ] );
					
			mapping.mapInterpolated( ipOrig, ip );
		}
	}
	
	public void mouseMoved( MouseEvent e ){}
	
	
	public static String modifiers( int flags )
	{
		String s = " [ ";
		if ( flags == 0 )
			return "";
		if ( ( flags & Event.SHIFT_MASK ) != 0 )
			s += "Shift ";
		if ( ( flags & Event.CTRL_MASK ) != 0 )
			s += "Control ";
		if ( ( flags & Event.META_MASK ) != 0 )
			s += "Meta (right button) ";
		if ( ( flags & Event.ALT_MASK ) != 0 )
			s += "Alt ";
		s += "]";
		if ( s.equals( " [ ]" ) )
			s = " [no modifiers]";
		return s;
	}
}
