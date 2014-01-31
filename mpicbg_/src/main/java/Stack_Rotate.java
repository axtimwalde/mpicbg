import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.ImageWindow;
import ij.gui.StackWindow;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

import java.awt.Canvas;
import java.awt.Event;
import java.awt.Panel;
import java.awt.Scrollbar;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import mpicbg.ij.stack.InverseTransformMapping;
import mpicbg.models.AffineModel3D;
import mpicbg.models.InvertibleCoordinateTransform;
import mpicbg.models.InvertibleCoordinateTransformList;
import mpicbg.models.TranslationModel3D;

public class Stack_Rotate implements PlugIn, KeyListener, AdjustmentListener, MouseWheelListener, MouseListener, MouseMotionListener
{
	final static private String NL = System.getProperty( "line.separator" );
	
	final private class GUI
	{
		final private ImageWindow window;
		final private Canvas canvas;
		final private Scrollbar scrollBar;
		final private int scrollBarValue;
		final private int scrollBarVisible;
		final private int scrollBarMin;
		final private int scrollBarMax;
		
		final private ImageJ ij;
		
		/* backup */
		private KeyListener[] windowKeyListeners;
		private KeyListener[] canvasKeyListeners;
		private KeyListener[] ijKeyListeners;
		
		private MouseListener[] canvasMouseListeners;
		private MouseMotionListener[] canvasMouseMotionListeners;
		
		private MouseWheelListener[] windowMouseWheelListeners;
		
		private AdjustmentListener[] scrollBarAdjustmentListeners;
		
		
		GUI( final ImagePlus imp )
		{
			window = imp.getWindow();
			canvas = imp.getCanvas();
			scrollBar = ( Scrollbar )( ( Panel )window.getComponent( 1 ) ).getComponent( 1 );
			scrollBarValue = scrollBar.getValue();
			scrollBarVisible = scrollBar.getVisibleAmount();
			scrollBarMin = scrollBar.getMinimum();
			scrollBarMax = scrollBar.getMaximum();
			
			ij = IJ.getInstance();
		}
		
		/**
		 * Add new event handlers.
		 */
		final void takeOverGui()
		{
			canvas.addKeyListener( Stack_Rotate.this );
			window.addKeyListener( Stack_Rotate.this );
			
			canvas.addMouseMotionListener( Stack_Rotate.this );
			
			canvas.addMouseListener( Stack_Rotate.this );
			
			ij.addKeyListener( Stack_Rotate.this );
			
			window.addMouseWheelListener( Stack_Rotate.this );
			
			scrollBar.addAdjustmentListener( Stack_Rotate.this );
			updateScrollBar();

		}
		
		/**
		 * Backup old event handlers for restore.
		 */
		final void backupGui()
		{
			canvasKeyListeners = canvas.getKeyListeners();
			windowKeyListeners = window.getKeyListeners();
			ijKeyListeners = IJ.getInstance().getKeyListeners();
			canvasMouseListeners = canvas.getMouseListeners();
			canvasMouseMotionListeners = canvas.getMouseMotionListeners();
			windowMouseWheelListeners = window.getMouseWheelListeners();
			scrollBarAdjustmentListeners = scrollBar.getAdjustmentListeners();
			clearGui();	
		}
		
		/**
		 * Restore the previously active Event handlers.
		 */
		final void restoreGui()
		{
			clearGui();
			for ( final KeyListener l : canvasKeyListeners )
				canvas.addKeyListener( l );
			for ( final KeyListener l : windowKeyListeners )
				window.addKeyListener( l );
			for ( final KeyListener l : ijKeyListeners )
				ij.addKeyListener( l );
			for ( final MouseListener l : canvasMouseListeners )
				canvas.addMouseListener( l );
			for ( final MouseMotionListener l : canvasMouseMotionListeners )
				canvas.addMouseMotionListener( l );
			for ( final MouseWheelListener l : windowMouseWheelListeners )
				window.addMouseWheelListener( l );
			for ( final AdjustmentListener l : scrollBarAdjustmentListeners )
				scrollBar.addAdjustmentListener( l );
			scrollBar.setValues( scrollBarValue, scrollBarVisible, scrollBarMin, scrollBarMax );
		}
		
		/**
		 * Remove both ours and the backed up event handlers.
		 */
		final void clearGui()
		{
			for ( final KeyListener l : canvasKeyListeners )
				canvas.removeKeyListener( l );
			for ( final KeyListener l : windowKeyListeners )
				window.removeKeyListener( l );
			for ( final KeyListener l : ijKeyListeners )
				ij.removeKeyListener( l );
			for ( final MouseListener l : canvasMouseListeners )
				canvas.removeMouseListener( l );
			for ( final MouseMotionListener l : canvasMouseMotionListeners )
				canvas.removeMouseMotionListener( l );
			for ( final MouseWheelListener l : windowMouseWheelListeners )
				window.removeMouseWheelListener( l );
			for ( final AdjustmentListener l : scrollBarAdjustmentListeners )
				scrollBar.removeAdjustmentListener( l );	
			
			canvas.removeKeyListener( Stack_Rotate.this );
			window.removeKeyListener( Stack_Rotate.this );
			ij.removeKeyListener( Stack_Rotate.this );
			canvas.removeMouseListener( Stack_Rotate.this );
			canvas.removeMouseMotionListener( Stack_Rotate.this );
			window.removeMouseWheelListener( Stack_Rotate.this );
			scrollBar.removeAdjustmentListener( Stack_Rotate.this );
		}
	}
	
	private ImagePlus imp;
	private ImageProcessor ip;
	private ImageStack stack;
	private GUI gui;
	
	final private InvertibleCoordinateTransformList< InvertibleCoordinateTransform > ictl = new InvertibleCoordinateTransformList< InvertibleCoordinateTransform >();
	final private AffineModel3D rotation = new AffineModel3D();
	final private AffineModel3D mouseRotation = new AffineModel3D();
	final static private float step = ( float )Math.PI / 180;
	final private TranslationModel3D sliceShift = new TranslationModel3D();
	final private AffineModel3D reducedAffine = new AffineModel3D();
	final private InverseTransformMapping< AffineModel3D > mapping = new InverseTransformMapping< AffineModel3D >( reducedAffine ); 
	
	/*
	 * the original z scaling relative to x,y
	 * (ImageJ supports only isotropic resolution in x.y or at least I do not
	 * understand how it does it for non-isotropic resolution.) 
	 */
	private float zScale;
	
	/* the current rotation axis, indexed x->0, y->1, z->2 */
	private int axis = 0;
	
	/* the current slice index (rotated z) in isotropic x,y,z space */
	private float currentSlice = 0;
	
	/* coordinates where mouse dragging started and the drag distance */
	private int oX, oY, dX, dY;
	
	public class MappingThread extends Thread
	{
		final protected ImageStack source;
		final protected ImageProcessor target;
		final protected ImageProcessor temp;
		protected boolean interpolate;
		private boolean pleaseRepaint;
		
		public MappingThread(
				final ImageStack source,
				final ImageProcessor target,
				final boolean interpolate )
		{
			this.source = source;
			this.target = target;
			this.temp = target.createProcessor( target.getWidth(), target.getHeight() );
			temp.snapshot();
			this.interpolate = interpolate;
			this.setName( "MappingThread" );
		}
		
		@Override
		public void run()
		{
			while ( !isInterrupted() )
			{
				final boolean b;
				synchronized ( this )
				{
					b = pleaseRepaint;
					pleaseRepaint = false;
				}
				if ( b )
				{
					temp.reset();
					if ( interpolate )
						mapping.mapInterpolated( source, temp );
					else
						mapping.map( source, temp );
					
					final Object targetPixels = target.getPixels();
					target.setPixels( temp.getPixels() );
					temp.setPixels( targetPixels );
					imp.updateImage();
					imp.draw();
				}
				synchronized ( this )
				{
					try
					{
						if ( !pleaseRepaint ) wait();
					}
					catch ( final InterruptedException e )
					{
						break;
					}
				}
			}
		}
		
		public void repaint()
		{
			synchronized ( this )
			{
				pleaseRepaint = true;
				notify();
			}
		}
		
		public void toggleInterpolation()
		{
			interpolate = !interpolate;
		}		 
	}

	private MappingThread painter;
	
	@Override
	public void run( final String arg )
    {
		imp = IJ.getImage();
		if ( imp == null || imp.getStackSize() == 1 )
		{
			IJ.error( "This is not a stack." );
			return;
		}
		
		ictl.clear();
		
		try
		{
			gui = new GUI( imp );
		}
		catch ( final ClassCastException e )
		{
			IJ.log( "Could not acquire GUI.  Probably, the AWT components of the stack window changed.  Write an e-mail to saalfed@mpi-cbg.de to fix this." );
			final StackTraceElement[] stackTraceElements = e.getStackTrace();
			for ( final StackTraceElement stackTraceElement : stackTraceElements )
				IJ.log( stackTraceElement.toString() );
		}
		
		final Calibration c = imp.getCalibration();
		zScale = ( float )( c.pixelDepth / c.pixelWidth );
		
		stack = imp.getStack();
		
		currentSlice = ( imp.getCurrentSlice() - 1 ) * zScale;
		final int w = stack.getWidth();
		final int h = stack.getHeight();
		final int d = stack.getSize();
		
		ip = stack.getProcessor( ( int )( currentSlice / zScale + 1.5f ) ).duplicate();
		
		/* un-scale */
		final AffineModel3D unScale = new AffineModel3D();
		unScale.set(
			1.0f, 0.0f, 0.0f, 0.0f,
			0.0f, 1.0f, 0.0f, 0.0f,
			0.0f, 0.0f, zScale, 0.0f );

		/* slice shift */
		sliceShift.set( 0, 0, -currentSlice );

		/* center shift */
		final TranslationModel3D centerShift = new TranslationModel3D();
		centerShift.set( -w / 2, -h / 2, -d / 2 * zScale );

		/* center un-shift */
		final TranslationModel3D centerUnShift = new TranslationModel3D();
		centerUnShift.set( w / 2, h / 2, d / 2 * zScale );

		/* initialize rotation */
		rotation.set(
			1.0f, 0.0f, 0.0f, 0.0f,
			0.0f, 1.0f, 0.0f, 0.0f,
			0.0f, 0.0f, 1.0f, 0.0f );

		ictl.add( unScale );
		ictl.add( centerShift );
		ictl.add( rotation );
		ictl.add( centerUnShift );
		ictl.add( sliceShift );
		
		reduceAffineTransformList( ictl, reducedAffine );

		imp.setProcessor( imp.getTitle(), ip );
		
		gui.backupGui();
		gui.takeOverGui();
		
		painter = new MappingThread( stack, ip, true );
		
		painter.start();
    }
	
	final private void updateScrollBar()
	{
		final float[] min = new float[]{ 0, 0, 0 };
		final float[] max = new float[]{ stack.getWidth() - 1, stack.getHeight() - 1, stack.getSize() - 1 };
		reducedAffine.estimateBounds( min, max );
		min[ 2 ] += currentSlice;
		max[ 2 ] += currentSlice;
		gui.scrollBar.setValues( Math.round( currentSlice ), 1, Math.round( min[ 2 ] ), Math.round( max[ 2 ] ) );
	}
	
	final private void update()
	{
		reduceAffineTransformList( ictl, reducedAffine );
		painter.repaint();
	}
	
	final private void apply()
	{
		new Thread(
				new Runnable(){
					@Override
					final public void run()
					{
						imp.lock();
						/* combine un-scale, center shift, center un-shift and rotation into a single AffineModel3D */
						final AffineModel3D a = new AffineModel3D();
						a.preConcatenate( ( AffineModel3D )ictl.get( 0 ) );
						a.preConcatenate( ( TranslationModel3D )ictl.get( 1 ) );
						a.preConcatenate( ( AffineModel3D )ictl.get( 2 ) );
						a.preConcatenate( ( TranslationModel3D )ictl.get( 3 ) );
						
						/* bounding volume */
						final float[] min = new float[]{ 0, 0, 0 };
						final float[] max = new float[]{ stack.getWidth(), stack.getHeight(), stack.getSize() };
						a.estimateBounds( min, max );
						final int w = ( int )Math.ceil( max[ 0 ] - min[ 0 ] );
						final int h = ( int )Math.ceil( max[ 1 ] - min[ 1 ] );
						final int d = ( int )Math.ceil( max[ 2 ] - min[ 2 ] );
						final TranslationModel3D minShift = new TranslationModel3D();
						minShift.set( -min[ 0 ], -min[ 1 ], -min[ 2 ] );
						a.preConcatenate( minShift );
						
						/* TODO calculate optimal slice thickness, for now uses the previous x,y spacing isotropicly */
						final TranslationModel3D sliceOffset = new TranslationModel3D();
						sliceOffset.set( 0, 0, -1 );
						final InverseTransformMapping< AffineModel3D> aMapping = new InverseTransformMapping< AffineModel3D >( a );
						
						final ImageProcessor source = stack.getProcessor( 1 );
						
						final ImageStack result = new ImageStack(
								( int )Math.ceil( w ),
								( int )Math.ceil( h ) );
						
						for ( int i = 0; i <= d; ++i )
						{
							final ImageProcessor ipSlice = source.createProcessor( w, h );
							aMapping.mapInterpolated( stack, ipSlice );
//							aMapping.map( stack, ip );
							result.addSlice( "" + i, ipSlice );
							a.preConcatenate( sliceOffset );
							IJ.showProgress( i, d );
						}
						final Calibration resultCalibration = imp.getCalibration().copy();
						resultCalibration.pixelDepth = resultCalibration.pixelWidth;
						gui.restoreGui();
						imp.setStack( imp.getTitle(), result );
						( ( StackWindow )imp.getWindow() ).updateSliceSelector();
						imp.setCalibration( resultCalibration );
						imp.updateAndDraw();
						imp.unlock();
					}
				} ).start();
	}
	
	private void rotate( final int a, final float d )
	{
		rotation.rotate( a, d * step );
	}
	
	final private void shift( final float d )
	{
		currentSlice += d;
		sliceShift.set( 0, 0, -currentSlice );
	}
	
	/**
	 * Fragile typeless reduction of ictls of which is assumed that they contain only affine transformations
	 * 
	 * @param ictl
	 * @param affine
	 */
	final private static void reduceAffineTransformList( final InvertibleCoordinateTransformList< ? > ictl, final AffineModel3D affine )
	{
		final AffineModel3D a = new AffineModel3D();
		for ( final InvertibleCoordinateTransform t : ictl.getList( null ) )
		{
			if ( AffineModel3D.class.isInstance( t ) )
				a.preConcatenate( ( AffineModel3D )t );
			else if ( TranslationModel3D.class.isInstance( t ) )
				a.preConcatenate( ( TranslationModel3D )t );
		}
		affine.set( a );
	}
	
	@Override
	public void keyPressed( final KeyEvent e )
	{
		if ( e.getKeyCode() == KeyEvent.VK_ESCAPE || e.getKeyCode() == KeyEvent.VK_ENTER )
		{
			painter.interrupt();
			
			if ( imp != null )
			{
				if ( e.getKeyCode() == KeyEvent.VK_ESCAPE )
				{
					imp.setStack( imp.getTitle(), stack );
					gui.restoreGui();
				}
				else if ( e.getKeyCode() == KeyEvent.VK_ENTER )
				{
					gui.clearGui();
					apply();
				}
			}
		}
		else if ( e.getKeyCode() == KeyEvent.VK_SHIFT )
		{
			oX -= 9 * dX / 10;
			oY -= 9 * dY / 10;
		}
		else if ( e.getKeyCode() == KeyEvent.VK_CONTROL )
		{
			oX += 9 * dX;
			oY += 9 * dY;
		}
		else if ( e.getKeyCode() == KeyEvent.VK_X )
		{
			axis = 0;
		}
		else if ( e.getKeyCode() == KeyEvent.VK_Y )
		{
			axis = 1;
		}
		else if ( e.getKeyCode() == KeyEvent.VK_Z )
		{
			axis = 2;
		}
		else
		{
			final float v = keyModfiedSpeed( e.getModifiersEx() );
			if ( e.getKeyCode() == KeyEvent.VK_LEFT )
			{
				rotate( axis, -v );
				updateScrollBar();
				update();
			}
			else if ( e.getKeyCode() == KeyEvent.VK_RIGHT )
			{
				rotate( axis, v );
				updateScrollBar();
				update();
			}
			else if ( e.getKeyCode() == KeyEvent.VK_COMMA )
			{
				shift( -v );
				gui.scrollBar.setValue( Math.round( currentSlice ) );
				update();
			}
			else if ( e.getKeyCode() == KeyEvent.VK_PERIOD )
			{
				shift( v );
				gui.scrollBar.setValue( Math.round( currentSlice ) );
				update();
			}
			else if ( e.getKeyCode() == KeyEvent.VK_I )
			{
				painter.toggleInterpolation();
				update();
			}
			else if ( e.getKeyCode() == KeyEvent.VK_E )
			{
				IJ.log( rotation.toString() );
			}
			else if ( e.getKeyCode() == KeyEvent.VK_T )
			{
				GenericDialog gd = new GenericDialog( "Define Rotation Matrix" );
				
				final float[] m = rotation.getMatrix( null );
				
				gd.addNumericField( "m00", m[ 0 ], 5 );
				gd.addNumericField( "m01", m[ 1 ], 5 );
				gd.addNumericField( "m02", m[ 2 ], 5 );
				gd.addNumericField( "m10", m[ 4 ], 5 );
				gd.addNumericField( "m11", m[ 5 ], 5 );
				gd.addNumericField( "m12", m[ 6 ], 5 );
				gd.addNumericField( "m20", m[ 8 ], 5 );
				gd.addNumericField( "m21", m[ 9 ], 5 );
				gd.addNumericField( "m22", m[ 10 ], 5 );
				
				gd.showDialog();
				
				if ( !gd.wasCanceled() )
				{
					m[ 0 ] = (float)gd.getNextNumber();
					m[ 1 ] = (float)gd.getNextNumber();
					m[ 2 ] = (float)gd.getNextNumber();
					m[ 4 ] = (float)gd.getNextNumber();
					m[ 5 ] = (float)gd.getNextNumber();
					m[ 6 ] = (float)gd.getNextNumber();
					m[ 8 ] = (float)gd.getNextNumber();
					m[ 9 ] = (float)gd.getNextNumber();
					m[ 10 ] = (float)gd.getNextNumber();
					
					rotation.set( m[ 0 ], m[ 1 ], m[ 2 ], 0, m[ 4 ], m[ 5 ], m[ 6 ], 0, m[ 8 ], m[ 9 ], m[ 10 ], 0 );
					
					// we need to compute the reducedaffine first, otherwise the scrollbars are off ...
					reduceAffineTransformList( ictl, reducedAffine );
					updateScrollBar();
					painter.repaint();
				}
			}
			else if ( e.getKeyCode() == KeyEvent.VK_F1 )
			{
				IJ.showMessage(
						"Interactive Stack Rotation",
						"Mouse control:" + NL + " " + NL +
						"Pan and tilt the stack by dragging the image in the canvas and" + NL +
						"browse alongside the z-axis using the mouse-wheel and the slice bar." + NL + " " + NL +
						"Key control:" + NL + " " + NL +
						"X - Select x-axis as rotation axis." + NL +
						"Y - Select y-axis as rotation axis." + NL +
						"Z - Select z-axis as rotation axis." + NL +
						"CURSOR LEFT - Rotate counter-clockwise around the choosen rotation axis." + NL +
						"CURSOR RIGHT - Rotate clockwise around the choosen rotation axis." + NL +
						"./> - Forward alongside z-axis." + NL +
						",/< - Backward alongside z-axis." + NL +
						"SHIFT - Rotate and browse 10x faster." + NL +
						"CTRL - Rotate and browse 10x slower." + NL +
						"CURSOR RIGHT - Rotate clockwise around the choosen rotation axis." + NL +
						"ENTER - Apply the rotation and render the full stack." + NL +
						"ESC - Return to the original stack." + NL +
						"I - Toggle interpolation." + NL +
						"E - Export the current rotation to the log window." + NL +
						"T - Define affine transformation matrix" );
			}
		}
	}

	final private float keyModfiedSpeed( final int modifiers )
	{
		if ( ( modifiers & KeyEvent.SHIFT_DOWN_MASK ) != 0 )
			return 10;
		else if ( ( modifiers & KeyEvent.CTRL_DOWN_MASK ) != 0 )
			return 0.1f;
		else
			return 1;
	}

	@Override
	public void keyReleased( final KeyEvent e )
	{
		if ( e.getKeyCode() == KeyEvent.VK_SHIFT )
		{
			oX += 9 * dX;
			oY += 9 * dY;
		}
		else if ( e.getKeyCode() == KeyEvent.VK_CONTROL )
		{
			oX -= 9 * dX / 10;
			oY -= 9 * dY / 10;
		}
	}
	@Override
	public void keyTyped( final KeyEvent e ){}
	
	public static String modifiers( final int flags )
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

	@Override
	public void adjustmentValueChanged( final AdjustmentEvent e )
	{
		currentSlice = e.getValue();
		sliceShift.set( 0, 0, -currentSlice );
		update();
	}

	@Override
	public void mouseWheelMoved( final MouseWheelEvent e )
	{
		final float v = keyModfiedSpeed( e.getModifiersEx() );
		final int s = e.getWheelRotation();
		shift( v * s );
		gui.scrollBar.setValue( Math.round( currentSlice ) );
		update();		
	}

	@Override
	public void mouseDragged( final MouseEvent e )
	{
		final float v = 10 * step * keyModfiedSpeed( e.getModifiersEx() );
		dX = oX - e.getX();
		dY = oY - e.getY();
		rotation.set( mouseRotation );
		rotate( 0, dY * v );
		rotate( 1, dX * v );
		updateScrollBar();
		update();
	}

	@Override
	public void mouseMoved( final MouseEvent e ){}
	@Override
	public void mouseClicked( final MouseEvent e ){}
	@Override
	public void mouseEntered( final MouseEvent e ){}
	@Override
	public void mouseExited( final MouseEvent e ){}
	@Override
	public void mouseReleased( final MouseEvent e ){}
	@Override
	public void mousePressed( final MouseEvent e )
	{
		oX = e.getX();
		oY = e.getY();
		mouseRotation.set( rotation );
	}
}
