package mpicbg.ij;

import ij.CompositeImage;
import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.ImageStack;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 * An interactive parent class for point based image deformation.
 *
 * @param <M> the transformation model to be used
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.2b
 */
public abstract class InteractiveInvertibleCoordinateTransform< M extends Model< M > & InvertibleCoordinateTransform > implements PlugIn, MouseListener, MouseMotionListener, KeyListener, ImageListener
{
	static public class Tuple
	{
		final public ImageProcessor source;
		final public ImageProcessor target;
		final public AtomicBoolean pleaseRepaint = new AtomicBoolean( false );
		public MappingThread painter = null;
		
		Tuple( final ImageProcessor source, final ImageProcessor target )
		{
			this.source = source;
			this.target = target;
		}
	}
	
	protected InverseTransformMapping< M > mapping;
	protected ImagePlus imp;
	final protected ArrayList< Tuple > tuples = new ArrayList< Tuple >();
	
	protected Point[] p;
	protected Point[] q;
	final protected ArrayList< PointMatch > m = new ArrayList< PointMatch >();
	protected PointRoi handles;
	
	protected int targetIndex = -1;
	
	abstract protected M myModel();
	abstract protected void setHandles();
	abstract protected void updateHandles( int x, int y );
	
	public void run( String arg )
    {
		// cleanup
		m.clear();
		tuples.clear();
		
		imp = IJ.getImage();
		if ( imp.isComposite() && ( ( CompositeImage )imp ).getMode() == CompositeImage.COMPOSITE )
		{
			final int z = imp.getSlice();
			final int t = imp.getFrame();
			for ( int c = 1; c <= imp.getNChannels(); ++c )
			{
				final int i = imp.getStackIndex( c, z, t );
				final ImageProcessor target = imp.getStack().getProcessor( i );
				final ImageProcessor source = target.duplicate();
				source.setInterpolationMethod( ImageProcessor.BILINEAR );
				tuples.add( new Tuple( source, target ) );
			}
		}
		else
		{
			final ImageProcessor target = imp.getProcessor();
			final ImageProcessor source = target.duplicate();
			source.setInterpolationMethod( ImageProcessor.BILINEAR );
			tuples.add( new Tuple( source, target ) );
		}		
		
		mapping = new InverseTransformMapping< M >( myModel() );
		for ( final Tuple tuple : tuples )
		{
			tuple.painter = new MappingThread(
					imp,
					tuple.source,
					tuple.target,
					tuple.pleaseRepaint,
					mapping,
					false,
					imp.getStackIndex( imp.getChannel(), imp.getSlice(), imp.getFrame() ) );
			tuple.painter.start();
		}
		
		setHandles();
		
		Toolbar.getInstance().setTool( Toolbar.getInstance().addTool( "Drag_the_handles." ) );
		
		imp.getCanvas().addMouseListener( this );
		imp.getCanvas().addMouseMotionListener( this );
		imp.getCanvas().addKeyListener( this );
    }
	
	public void imageClosed( ImagePlus imp )
	{
		if ( imp == this.imp )
			for ( final Tuple tuple : tuples )
				tuple.painter.interrupt();
	}
	public void imageOpened( ImagePlus imp ){}
	public void imageUpdated( ImagePlus imp ){}
	
	public void keyPressed( KeyEvent e)
	{
		if ( e.getKeyCode() == KeyEvent.VK_ESCAPE || e.getKeyCode() == KeyEvent.VK_ENTER )
		{
			for ( final Tuple tuple : tuples )
				tuple.painter.interrupt();
			if ( imp != null )
			{
				imp.getCanvas().removeMouseListener( this );
				imp.getCanvas().removeMouseMotionListener( this );
				imp.getCanvas().removeKeyListener( this );
				imp.getCanvas().setDisplayList( null );
				imp.setRoi( ( Roi )null );
			}
			
			/* reset pixels */
			final int z = imp.getSlice();
			final int t = imp.getFrame();
			if ( imp.isComposite() && ( ( CompositeImage )imp ).getMode() == CompositeImage.COMPOSITE )
			{
				for ( int c = 1; c <= imp.getNChannels(); ++c )
				{
					final int i = imp.getStackIndex( c, z, t );
					final ImageProcessor ip = tuples.get( c - 1 ).source;
					imp.getStack().setPixels( ip.getPixels(), i );
					if ( c == imp.getChannel() )
						imp.setProcessor( ip );
				}
			}
			else
			{
				final ImageProcessor ip = tuples.get( 0 ).source;
				imp.setProcessor( ip );
				imp.getStack().setPixels( ip.getPixels(), imp.getStackIndex( imp.getChannel(), z, t ) );
			}
			if ( e.getKeyCode() == KeyEvent.VK_ENTER )
			{
				final Thread thread = new Thread(
						new Runnable()
						{
							@Override
							public void run()
							{
								final int si = imp.getStackIndex( imp.getChannel(), imp.getSlice(), imp.getFrame() );
								final ImageStack stack = imp.getStack();
								for ( int i = 1; i <= stack.getSize(); ++i )
								{
									final ImageProcessor source = stack.getProcessor( i ).duplicate();
									final ImageProcessor target = source.createProcessor( source.getWidth(), source.getHeight() );
									source.setInterpolationMethod( ImageProcessor.BILINEAR );
									mapping.mapInterpolated( source, target );
									if ( i == si )
										imp.getProcessor().setPixels( target.getPixels() );
									stack.setPixels( target.getPixels(), i );
									IJ.showProgress( i, stack.getSize() );
								}
								if ( imp.isComposite() )
									( ( CompositeImage )imp ).setChannelsUpdated();
								imp.updateAndDraw();
							}
						} );
				thread.start();
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
				double dy = win.getCanvas().getMagnification() * ( q[ i ].getW()[ 1 ] - y );
				double d =  dx * dx + dy * dy;
				if ( d < 64.0 && d < target_d )
				{
					targetIndex = i;
					target_d = d;
				}
			}
		}
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
			
			updateHandles( x, y );
					
			try
			{
				myModel().fit( m );
				for ( final Tuple tuple : tuples )
				{
					synchronized ( tuple.painter )
					{
						tuple.pleaseRepaint.set( true );
						tuple.painter.notify();
					}
				}
			}
			catch ( NotEnoughDataPointsException ex ) { ex.printStackTrace(); }
			catch ( IllDefinedDataPointsException ex ) { ex.printStackTrace(); }
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
