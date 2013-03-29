import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageWindow;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.Toolbar;
import ij.io.SaveDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Event;
import java.awt.Shape;
import java.awt.TextField;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Random;
import java.util.Vector;

import mpicbg.ij.TransformMeshMapping;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.SpringMesh;
import mpicbg.models.Vertex;

public class Transform_SpringMesh implements PlugIn, MouseListener,  MouseMotionListener, KeyListener
{
	// number of vertices in horizontal direction
	private static int numX = 32;
	
	private static String rawFileName = "figure";
	
	ImagePlus imp;
	ImageProcessor ip;
	ImageProcessor ipOrig;
	
	protected TransformMeshMapping mapping; 
	
	final ArrayList< Point > hooks = new ArrayList< Point >();
	PointRoi handles;
	
	protected SpringMesh mesh;
	
	int targetIndex = -1;
	
	boolean pleaseOptimize = false;
	boolean pleaseIllustrate = false;
	boolean showMesh = false;
	boolean showSprings = false;
	
	final Vector< Roi > displayList = new Vector< Roi >();
	
	final class OptimizeThread extends Thread
	{
		@Override
		public void run()
		{
			while ( !isInterrupted() )
			{
				try
				{
					if ( pleaseOptimize )
					{
						mesh.optimize( Float.MAX_VALUE, 10000, 1000 );
						pleaseIllustrate = false;
						apply();
					}
					synchronized ( this ){ wait(); }
				}
				catch ( final NotEnoughDataPointsException ex ){ ex.printStackTrace( System.err ); }
				catch ( final InterruptedException e){ Thread.currentThread().interrupt(); }
			}
		}
	}
	
	final class IllustrateThread extends Thread
	{
		@Override
		public void run()
		{
			while ( !isInterrupted() )
			{
				try
				{
					synchronized ( this )
					{
						illustrate();
						if ( pleaseIllustrate )
							wait( 100 );
						else
							wait();
					}
				}
				catch ( final InterruptedException e)
				{
					illustrate();
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	private Thread opt;
	private Thread ill;
	
	@Override
	public void run( final String arg )
    {
		hooks.clear();
		
		imp = IJ.getImage();
		ip = imp.getProcessor();
		ipOrig = ip.duplicate();
		
		final GenericDialog gd = new GenericDialog( "Elastic Moving Least Squares Transform" );
		gd.addNumericField( "Vertices_per_row :", numX, 0 );
		gd.showDialog();
		
		if (gd.wasCanceled()) return;
		
		numX = ( int )gd.getNextNumber();
		
		// intitialize the transform mesh
		mesh = new SpringMesh( numX, imp.getWidth(), imp.getHeight(), 0.01f, Float.MAX_VALUE, 0.9f );
		
		// test passive vertices
		final Random rnd = new Random( 0 );
		for ( int i = 0; i < 5; ++i )
			mesh.addPassiveVertex( new Vertex( new float[]{ rnd.nextInt( imp.getWidth() - 1 ), rnd.nextInt( imp.getHeight() - 1 ) } ) );
		
		mapping = new TransformMeshMapping( mesh );
		
		
//		Point p = new Point( new float[]{ ip.getWidth() / 4, ip.getHeight() / 4 } );
//		hooks.add( p );
//		mesh.addVertex( new Vertex( p ), 10 );
//			
//		p = new Point( new float[]{ 3 * ip.getWidth() / 4, ip.getHeight() / 2 } );
//		hooks.add( p );
//		mesh.addVertex( new Vertex( p ), 10 );
//			
//		p = new Point( new float[]{ ip.getWidth() / 4, 3 * ip.getHeight() / 4 } );
//		hooks.add( p );
//		mesh.addVertex( new Vertex( p ), 10 );
//		
//		
//		handles = new PointRoi(
//				new int[]{ ip.getWidth() / 4, 3 * ip.getWidth() / 4, ip.getWidth() / 4 },
//				new int[]{ ip.getHeight() / 4, ip.getHeight() / 2, 3 * ip.getHeight() / 4 }, hooks.size() );
//		imp.setRoi( handles );
		
		Toolbar.getInstance().setTool( Toolbar.getInstance().addTool( "Add_and_drag_handles." ) );
		
		
		opt = new OptimizeThread();
		ill = new IllustrateThread();
		opt.start();
		ill.start();
		
		imp.getCanvas().addMouseListener( this );
		imp.getCanvas().addMouseMotionListener( this );
		imp.getCanvas().addKeyListener( this );
    }
	
	void illustrate()
	{
		Shape shape;
		Roi roi;
		synchronized ( displayList )
		{
			displayList.clear();
			synchronized ( mesh )
			{
				if ( showSprings )
				{
					shape = mesh.illustrateSprings();
					roi = new ShapeRoi( shape );
					roi.setInstanceColor( Color.red );
					displayList.addElement( roi );
				}
				if ( showMesh )
				{
					shape = mesh.illustrateMesh();
					roi = new ShapeRoi( shape );
					roi.setInstanceColor( Color.white );
					displayList.addElement( roi );
				}
				imp.getCanvas().setDisplayList( displayList );
			}
		}
	}
	
	public void apply()
	{
		//mapping.mapInterpolated( ipOrig, ip );
		//imp.updateAndDraw();
	}
	
	private void updateRoi()
	{
		final int[] x = new int[ hooks.size() ];
		final int[] y = new int[ hooks.size() ];
		
		for ( int i = 0; i < hooks.size(); ++ i )
		{
			final float[] l = hooks.get( i ).getW();
			x[ i ] = ( int )l[ 0 ];
			y[ i ] = ( int )l[ 1 ];
		}
		handles = new PointRoi( x, y, hooks.size() );
		imp.setRoi( handles );
	}
	
	@Override
	public void keyPressed( final KeyEvent e)
	{
		if ( e.getKeyCode() == KeyEvent.VK_ESCAPE || e.getKeyCode() == KeyEvent.VK_ENTER )
		{
			opt.interrupt();
			
			showMesh = false;
			showSprings = false;
			ill.interrupt();
			
			pleaseIllustrate = false;
			
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
				e.getKeyCode() == KeyEvent.VK_Y ||
				e.getKeyCode() == KeyEvent.VK_U )
		{
			if ( e.getKeyCode() == KeyEvent.VK_Y ) showMesh = !showMesh;
			if ( e.getKeyCode() == KeyEvent.VK_U ) showSprings = !showSprings;
			if ( showMesh || showSprings )
			{
				synchronized ( ill )
				{
					if ( pleaseIllustrate == false )
						illustrate();
					else
						ill.notify();
				}
			}
			else
			{
				pleaseIllustrate = false;
				synchronized ( ill )
				{
					ill.interrupt();
					imp.getCanvas().setDisplayList( null );
				}
			}			
		}
		else if (
				e.getKeyCode() == KeyEvent.VK_G )
		{
			final SaveDialog sd = new SaveDialog( "Save as ...", rawFileName, ".svg" );
			final String directory = sd.getDirectory();
			final String name = sd.getFileName();
			rawFileName = name.replaceAll( "\\.svg$", "" );

			if ( name == null || name == "" ) 
			{
				IJ.error( "No filename selected." );
				return;
			}
					
			final String fileName = directory + name;
			
			final String g = mesh.illustrateMeshSVG();
			
			try
			{
				final InputStream is = getClass().getResourceAsStream( "template.svg" );
				final byte[] bytes = new byte[ is.available() ];
				is.read( bytes );
				String svg = new String( bytes );
				svg = svg.replaceAll( "<!--g-->", g );
				
				IJ.log( svg );
				
				final PrintStream ps = new PrintStream( fileName ); 
				ps.print( svg );
				ps.close();
			}
			catch ( final Exception ex )
			{
				IJ.error( "Error writing svg-file '" + fileName + "'.\n" + ex.getMessage() );
			}
		}
		else if (
				( e.getKeyCode() == KeyEvent.VK_F1 ) &&
				( e.getSource() instanceof TextField ) ){}
	}

	@Override
	public void keyReleased( final KeyEvent e ){}
	@Override
	public void keyTyped( final KeyEvent e ){}
	
	@Override
	public void mousePressed( final MouseEvent e )
	{
		targetIndex = -1;
		if ( e.getButton() == MouseEvent.BUTTON1 )
		{
			final ImageWindow win = WindowManager.getCurrentWindow();
			final int xm = win.getCanvas().offScreenX( e.getX() );
			final int ym = win.getCanvas().offScreenY( e.getY() );
			
			// find the closest handle to drag it
			double target_d = Double.MAX_VALUE;
			for ( int i = 0; i < hooks.size(); ++i )
			{
				final float[] l = hooks.get( i ).getW(); 
				final double dx = win.getCanvas().getMagnification() * ( l[ 0 ] - xm );
				final double dy = win.getCanvas().getMagnification() * ( l[ 1 ] - ym );
				final double d =  dx * dx + dy * dy;
				
				if ( d < 64.0 && d < target_d )
				{
					targetIndex = i;
					target_d = d;
				}
			}
			
			// no handle next to the mouse so create a new one
			if ( targetIndex == -1 )
			{
				final float[] w = new float[]{ xm, ym };
				final float[] l = mesh.findClosestTargetVertex( w ).getL(); 
				
				synchronized ( mesh )
				{
					final Vertex p = new Vertex( l, w );
					hooks.add( p );
					
					mesh.addVertex( p, 1 );
					//mesh.addVertexWeightedByDistance( new Vertex( p ), 10, 1.0f );
				}
				updateRoi();
			}
		}
	}

	@Override
	public void mouseExited( final MouseEvent e ) {}
	@Override
	public void mouseClicked( final MouseEvent e ) {}	
	@Override
	public void mouseEntered( final MouseEvent e ) {}
	@Override
	public void mouseReleased( final MouseEvent e ){}
	
	@Override
	public void mouseDragged( final MouseEvent e )
	{
		if ( targetIndex >= 0 )
		{
			final ImageWindow win = WindowManager.getCurrentWindow();
			final int xm = win.getCanvas().offScreenX( e.getX() );
			final int ym = win.getCanvas().offScreenY( e.getY() );
			
			final float[] l = hooks.get( targetIndex ).getW();
			
			l[ 0 ] = xm;
			l[ 1 ] = ym;
			
			updateRoi();
			
			if ( showMesh || showSprings )
				synchronized ( ill )
				{
					pleaseIllustrate = true;
					ill.notify();
				}
			synchronized ( opt )
			{
				pleaseOptimize = true;
				opt.notify();
			}
		}
	}
	
	@Override
	public void mouseMoved( final MouseEvent e ){}
	
	
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
}
