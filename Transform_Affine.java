import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;
import ij.process.*;
import ij.gui.*;

import mpicbg.models.*;

import java.awt.Event;
import java.awt.TextField;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;
import java.util.ArrayList;

public class Transform_Affine implements PlugIn, MouseListener,  MouseMotionListener, KeyListener
{
	ImagePlus imp;
	ImageProcessor ip;
	ImageProcessor ipOrig;
	
	Point[] p;
	Point[] q;
	final ArrayList< PointMatch > m = new ArrayList< PointMatch >();
	PointRoi handles;
	AffineModel2D h;
	
	int targetIndex = -1;
	
	public void run( String arg )
    {
		// cleanup
		m.clear();
		
		imp = IJ.getImage();
		ip = imp.getProcessor();
		ipOrig = ip.duplicate();
		
		int[] x = new int[]{ ip.getWidth() / 4, 3 * ip.getWidth() / 4, ip.getWidth() / 4 };
		int[] y = new int[]{ ip.getHeight() / 4, ip.getHeight() / 2, 3 * ip.getHeight() / 4 };
		
		p = new Point[]{
				new Point( new float[]{ ( float )x[ 0 ], ( float )y[ 0 ] } ),
				new Point( new float[]{ ( float )x[ 1 ], ( float )y[ 1 ] } ),
				new Point( new float[]{ ( float )x[ 2 ], ( float )y[ 2 ] } ) };
		
		q = new Point[]{
				p[ 0 ].clone(),
				p[ 1 ].clone(),
				p[ 2 ].clone() };
		
		m.add( new PointMatch( p[ 0 ], q[ 0 ] ) );
		m.add( new PointMatch( p[ 1 ], q[ 1 ] ) );
		m.add( new PointMatch( p[ 2 ], q[ 2 ] ) );
		
		handles = new PointRoi( x, y, 3 );
		imp.setRoi( handles );
		
		Toolbar.getInstance().setTool( Toolbar.getInstance().addTool( "Drag_the_handles." ) );
		
		imp.getCanvas().addMouseListener( this );
		imp.getCanvas().addMouseMotionListener( this );
		imp.getCanvas().addKeyListener( this );
    }
	
	public void apply()
	{
		h = new AffineModel2D();
		try
		{
			h.fit( m );
			
			//System.out.println( h );
			
			for ( int y = 0; y < ip.getHeight(); ++y )
			{
				for ( int x = 0; x < ip.getWidth(); ++x )
				{
					float[] t = new float[]{ x, y };
					//System.out.println( t[ 0 ] + " " + t[ 1 ] );
					h.applyInverseInPlace( t );
					ip.putPixel( x, y, ipOrig.getPixel( ( int )t[ 0 ], ( int )t[ 1 ] ) );
					//System.out.println( t[ 0 ] + " " + t[ 1 ] );
				}
			}
			
			imp.updateAndDraw();
		}
		catch ( Exception e )
		{
			//IJ.error( e.getMessage() );
			//e.printStackTrace( System.err );
		}
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
			for ( int i = 0; i < q.length; ++i )
			{
				double dx = win.getCanvas().getMagnification() * ( q[ i ].getW()[ 0 ] - x );
				double dy = win.getCanvas().getMagnification() * ( q[ i ].getW()[ 1 ]  - y );
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
				
			handles = new PointRoi( rx, ry, 3 );
			imp.setRoi( handles );
				
			fq[ 0 ] = x;
			fq[ 1 ] = y;
					
			apply();
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
