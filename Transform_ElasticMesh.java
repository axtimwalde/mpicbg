import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;
import ij.process.*;
import ij.gui.*;

import mpicbg.models.*;

import java.awt.Color;
import java.awt.Event;
import java.awt.Shape;
import java.awt.TextField;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;
import java.util.ArrayList;

public class Transform_ElasticMesh implements PlugIn, MouseListener,  MouseMotionListener, KeyListener
{
	// number of x-handles
	private static int numX = 4;
	// number of x-handles
	private static int numY = 3;
	
	ImagePlus imp;
	ImageProcessor ip;
	ImageProcessor ipOrig;
	
	PointMatch[] pq;
	int[] x;
	int[] y;
	Point[] hooks;
	PointRoi handles;
	
	final ElasticMesh mesh = new ElasticMesh();
	
	int targetIndex = -1;
	
	boolean showMesh = false;
	
	public void run( String arg )
    {
		// cleanup
		mesh.clear();
		
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
		mesh.init( numX, numY, imp.getWidth(), imp.getHeight() );		
		mesh.init();
		
		x = new int[]{ ip.getWidth() / 4, 3 * ip.getWidth() / 4, ip.getWidth() / 4 };
		y = new int[]{ ip.getHeight() / 4, ip.getHeight() / 2, 3 * ip.getHeight() / 4 };
		hooks = new Point[ 3 ];
		Tile screen = new Tile( imp.getWidth(), imp.getHeight(), new RigidModel2D() );
		
		for ( int i = 0; i < hooks.length; ++i )
		{
			System.out.println( i );
			float[] here = new float[]{ x[ i ], y[ i ] };
			hooks[ i ] = new Point( here );
			Tile o = mesh.findClosest( here );
			float[] there = here.clone();
			try
			{
				System.out.println( o );
				
				o.getModel().applyInverseInPlace( there );
			}
			catch ( NoninvertibleModelException e )
			{
				e.printStackTrace( System.err );
			}
			Point p2 = new Point( there );
			
			o.addMatch( new PointMatch( p2, hooks[ i ], 100f ) );
			screen.addMatch( new PointMatch( hooks[ i ], p2 ) );
			
		}
		
		handles = new PointRoi( x, y, hooks.length );
		imp.setRoi( handles );
		
		Toolbar.getInstance().setTool( Toolbar.getInstance().addTool( "Drag_the_handles." ) );
		
		imp.getCanvas().addMouseListener( this );
		imp.getCanvas().addMouseMotionListener( this );
		imp.getCanvas().addKeyListener( this );
    }
	
	public void optimize()
	{
		try
		{
			mesh.optimize( Float.MAX_VALUE, 10000, 100 );
		}
		catch ( NotEnoughDataPointsException ex )
		{
			ex.printStackTrace( System.err );
		}
	}
	
	public void showMesh()
	{
		Shape meshIllustration = mesh.illustrateMesh();
		imp.getCanvas().setDisplayList( meshIllustration, Color.white, null );
		mesh.updateMesh();
	}
	
	public void apply()
	{
		mesh.apply( ipOrig, ip );
		imp.updateAndDraw();
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
		else if ( e.getKeyCode() == KeyEvent.VK_Y )
		{
			showMesh = !showMesh;
			if ( showMesh )
				showMesh();
			else
				imp.getCanvas().setDisplayList( null );			
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
			int xm = win.getCanvas().offScreenX( e.getX() );
			int ym = win.getCanvas().offScreenY( e.getY() );
			
			double target_d = Double.MAX_VALUE;
			for ( int i = 0; i < hooks.length; ++i )
			{
				double dx = win.getCanvas().getMagnification() * ( x[ i ] - xm );
				double dy = win.getCanvas().getMagnification() * ( y[ i ] - ym );
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

	public void mouseExited( MouseEvent e ) {}
	public void mouseClicked( MouseEvent e ) {}	
	public void mouseEntered( MouseEvent e ) {}
	
	public void mouseReleased( MouseEvent e )
	{
		if ( e.getButton() == MouseEvent.BUTTON1 && targetIndex >= 0 )
		{
			ImageWindow win = WindowManager.getCurrentWindow();
			int xm = win.getCanvas().offScreenX( e.getX() );
			int ym = win.getCanvas().offScreenY( e.getY() );
			
			float[] fq = hooks[ targetIndex ].getW();
			
			fq[ 0 ] = x[ targetIndex ] = xm;
			fq[ 1 ] = y[ targetIndex ] = ym;
			
			handles = new PointRoi( x, y, hooks.length );
			imp.setRoi( handles );
				
			fq[ 0 ] = xm;
			fq[ 1 ] = ym;
			
			optimize();
			apply();
			if ( showMesh )
				showMesh();
			else
				imp.getCanvas().setDisplayList( null );
		}
	}
	
	public void mouseDragged( MouseEvent e )
	{
		if ( targetIndex >= 0 )
		{
			ImageWindow win = WindowManager.getCurrentWindow();
			int xm = win.getCanvas().offScreenX( e.getX() );
			int ym = win.getCanvas().offScreenY( e.getY() );
			
			float[] fq = hooks[ targetIndex ].getW();
			
			fq[ 0 ] = x[ targetIndex ] = xm;
			fq[ 1 ] = y[ targetIndex ] = ym;
			
			handles = new PointRoi( x, y, hooks.length );
			imp.setRoi( handles );
				
			fq[ 0 ] = xm;
			fq[ 1 ] = ym;
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
