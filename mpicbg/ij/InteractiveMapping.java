package mpicbg.ij;


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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 
 * An interactive parent class for point based image deformation.
 *
 * @param <M> the transformation model to be used
 */
public abstract class InteractiveMapping implements PlugIn, MouseListener, MouseMotionListener, KeyListener, ImageListener
{
	protected ImagePlus imp;
	protected ImageProcessor target;
	protected ImageProcessor source;
	
	protected Point[] p;
	protected Point[] q;
	final protected ArrayList< PointMatch > m = new ArrayList< PointMatch >();
	protected PointRoi handles;
	final protected ArrayList< Point > hooks = new ArrayList< Point >();
	
	protected Mapping mapping;
	protected MappingThread painter;
	final protected AtomicBoolean pleaseRepaint = new AtomicBoolean( false );
	
	static protected boolean showIllustration = false;
	static protected boolean showPreview = false;
	
	protected int targetIndex = -1;
	static protected boolean interpolate;
	
	abstract protected void updateHandles( int x, int y );
	
	public void init(){}
	abstract protected void createMapping();
	abstract protected void updateMapping() throws NotEnoughDataPointsException, IllDefinedDataPointsException;
	abstract protected void addHandle( int x, int y );
	abstract protected void updateIllustration();
	
	public void run( String arg )
    {
		// cleanup
		m.clear();
		
		imp = IJ.getImage();
		target = imp.getProcessor();
		source = target.duplicate();
		
		init();
		
		createMapping();
		
		painter = new MappingThread(
				imp,
				source,
				target,
				pleaseRepaint,
				mapping,
				interpolate );
		painter.start();
		
		Toolbar.getInstance().setTool( Toolbar.getInstance().addTool( "Add_and_drag_handles." ) );
		
		imp.getCanvas().addMouseListener( this );
		imp.getCanvas().addMouseMotionListener( this );
		imp.getCanvas().addKeyListener( this );
    }
	
	final protected void updateRoi()
	{
		int[] x = new int[ hooks.size() ];
		int[] y = new int[ hooks.size() ];
		
		for ( int i = 0; i < hooks.size(); ++ i )
		{
			float[] l = hooks.get( i ).getW();
			x[ i ] = ( int )l[ 0 ];
			y[ i ] = ( int )l[ 1 ];
		}
		handles = new PointRoi( x, y, hooks.size() );
		imp.setRoi( handles );
	}
	
	public void imageClosed( ImagePlus imp )
	{
		if ( imp == this.imp )
			painter.interrupt();
	}
	public void imageOpened( ImagePlus imp ){}
	public void imageUpdated( ImagePlus imp ){}
	
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

	public void keyReleased( KeyEvent e ){}
	public void keyTyped( KeyEvent e ){}
	
	public void mousePressed( MouseEvent e )
	{
		targetIndex = -1;
		ImageWindow win = WindowManager.getCurrentWindow();
		int xm = win.getCanvas().offScreenX( e.getX() );
		int ym = win.getCanvas().offScreenY( e.getY() );
		
		double target_d = Double.MAX_VALUE;
		for ( int i = 0; i < hooks.size(); ++i )
		{
			float[] l = hooks.get( i ).getW(); 
			double dx = win.getCanvas().getMagnification() * ( l[ 0 ] - xm );
			double dy = win.getCanvas().getMagnification() * ( l[ 1 ] - ym );
			double d =  dx * dx + dy * dy;
			
			if ( d < 64.0 && d < target_d )
			{
				targetIndex = i;
				target_d = d;
			}
		}
		
		if ( e.getButton() == MouseEvent.BUTTON1 )
		{		
			if ( targetIndex == -1 )
			{
				addHandle( xm, ym );
				updateRoi();
			}
		}
	}
	public void mouseReleased( MouseEvent e )
	{
		if ( !showPreview )
			try
			{
				updateMapping();
				synchronized ( painter )
				{
					if ( pleaseRepaint.compareAndSet( false, true ) )
						painter.notify();
				}
			}
			catch ( NotEnoughDataPointsException ex ){}
			catch ( IllDefinedDataPointsException ex ){}
	}
	
	public void mouseExited( MouseEvent e ){}
	public void mouseClicked( MouseEvent e ){}	
	public void mouseEntered( MouseEvent e ){}
	
	public void mouseDragged( MouseEvent e )
	{
		if ( targetIndex >= 0 )
		{
			ImageWindow win = WindowManager.getCurrentWindow();
			int x = win.getCanvas().offScreenX( e.getX() );
			int y = win.getCanvas().offScreenY( e.getY() );
			
			updateHandles( x, y );
			updateIllustration();
			updateRoi();	
			try
			{
				updateMapping();
				if ( showPreview )
				{
					synchronized ( painter )
					{
						if ( pleaseRepaint.compareAndSet( false, true ) )
							painter.notify();
					}
				}
			}
			catch ( NotEnoughDataPointsException ex ){}
			catch ( IllDefinedDataPointsException ex ){}
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
