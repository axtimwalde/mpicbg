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
import java.util.Vector;

public class Transform_ElasticMesh implements PlugIn, MouseListener,  MouseMotionListener, KeyListener
{
	// number of x-handles
	private static int numX = 16;
	// number of x-handles
	private static int numY = 3;
	// alpha [0 smooth, 1 less smooth ;)]
	private static float alpha = 1.0f;
	
	ImagePlus imp;
	ImageProcessor ip;
	ImageProcessor ipOrig;
	
	
	/**
	 * Visualisation
	 */
	final ByteProcessor ipPlot = new ByteProcessor( 200, 200 );
	final ImagePlus impPlot = new ImagePlus( "Optimization", ipPlot );
	
	//final ArrayList< PointMatch > pq = new ArrayList< PointMatch >();
	final ArrayList< Point > hooks = new ArrayList< Point >();
	PointRoi handles;
	Tile screen;
	
	protected ElasticMesh mesh;
	
	int targetIndex = -1;
	
	boolean showMesh = false;
	boolean showPointMatches = false;
	boolean pleaseIllustrate = false;
	
	final Vector< Roi > displayList = new Vector< Roi >();
	
	final class OptimizeThread extends Thread
	{
		public void run()
		{
			while ( !isInterrupted() && hooks.size() > 0 )
			{
				try
				{
					//mesh.optimizeByWeight( Float.MAX_VALUE, 100 * mesh.numVertices(), mesh.numVertices() );
					//mesh.optimize( Float.MAX_VALUE, 10000, 100 );
					
					mesh.optimizeByStrength( Float.MAX_VALUE, 10000, 100, ipPlot, impPlot );
					pleaseIllustrate = false;
					apply();
					synchronized ( this ){ wait(); }
				}
				catch ( NotEnoughDataPointsException ex ){ ex.printStackTrace( System.err ); }
				catch ( InterruptedException e){ Thread.currentThread().interrupt(); }
				catch ( Throwable t ){ t.printStackTrace(); }
			}
		}
	}
	
	final class IllustrateThread extends Thread
	{
		public void run()
		{
			while ( true )
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
				catch ( Throwable t ){ t.printStackTrace(); }
			}
		}
	}

	private Thread opt;
	private Thread ill;
	
	public void run( String arg )
    {
		hooks.clear();
		//pq.clear();
		
		imp = IJ.getImage();
		ip = imp.getProcessor();
		ipOrig = ip.duplicate();
		
		GenericDialog gd = new GenericDialog( "Grid Transform" );
		gd.addNumericField( "handles_per_row :", numX, 0 );
		//gd.addNumericField( "vertical_handles :", numY, 0 );
		gd.addNumericField( "alpha :", alpha, 2 );
		gd.showDialog();
		
		if (gd.wasCanceled()) return;
		
		impPlot.show();
		
		numX = ( int )gd.getNextNumber();
		//numY = ( int )gd.getNextNumber();
		alpha = ( float )gd.getNextNumber();
		float dx = ( float )imp.getWidth() / ( float )( numX - 1 );
		float dy = 2.0f * ( float )Math.sqrt(4.0f / 5.0f * dx * dx );
		//System.out.println( dy );
		numY = Math.round( imp.getHeight() / dy ) + 1;
		//System.out.println( numY );
		
		// intitialize the transform mesh
		mesh = new ElasticMesh( numX, numY, imp.getWidth(), imp.getHeight() );		
		
		screen = new Tile( imp.getWidth(), imp.getHeight(), new RigidModel2D() );
		
//		for ( int i = 0; i < 3; ++i )
//		{
//			hooks[ i ] = new Point( new float[]{ x[ i ], y[ i ] } );
//			Tile o = mesh.findClosest( hooks[ i ].getL() );
//			Point p2 = new Point( new float[]{ x[ i ], y[ i ] } );
//			
//			o.addMatch( new PointMatch( p2, hooks[ i ], 10f ) );
//			screen.addMatch( new PointMatch( hooks[ i ], p2 ) );
			
			hooks.add( new Point( new float[]{ ip.getWidth() / 4, ip.getHeight() / 4 } ) );
			Point p2 = new Point( new float[]{ ip.getWidth() / 4, ip.getHeight() / 4 } ); // use the same local point for each handle (is this correct?)
			mesh.addMatchWeightedByDistance( new PointMatch( p2, hooks.get( 0 ), 10f ), alpha );
			
			hooks.add( new Point( new float[]{ 3 * ip.getWidth() / 4, ip.getHeight() / 2 } ) );
			p2 = new Point( new float[]{ 3 * ip.getWidth() / 4, ip.getHeight() / 2 } ); // use the same local point for each handle (is this correct?)
			mesh.addMatchWeightedByDistance( new PointMatch( p2, hooks.get( 1 ), 10f ), alpha );
			
			hooks.add( new Point( new float[]{ ip.getWidth() / 4, 3 * ip.getHeight() / 4 } ) );
			p2 = new Point( new float[]{ ip.getWidth() / 4, 3 * ip.getHeight() / 4 } ); // use the same local point for each handle (is this correct?)
			mesh.addMatchWeightedByDistance( new PointMatch( p2, hooks.get( 2 ), 10f ), alpha );
//		}
		
		handles = new PointRoi(
				new int[]{ ip.getWidth() / 4, 3 * ip.getWidth() / 4, ip.getWidth() / 4 },
				new int[]{ ip.getHeight() / 4, ip.getHeight() / 2, 3 * ip.getHeight() / 4 }, hooks.size() );
		imp.setRoi( handles );
		
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
		displayList.clear();
		synchronized ( mesh )
		{
			if ( showMesh )
			{
				shape = mesh.illustrateMesh();
				roi = new ShapeRoi( shape );
				roi.setInstanceColor( Color.white );
				displayList.addElement( roi );
			}
			if ( showPointMatches )
			{
				shape = mesh.illustratePointMatches();
				roi = new ShapeRoi( shape );
				roi.setInstanceColor( Color.green );
				displayList.addElement( roi );
				shape = mesh.illustratePointMatchDisplacements();
				roi = new ShapeRoi( shape );
				roi.setInstanceColor( Color.red );
				displayList.addElement( roi );
			}
			imp.getCanvas().setDisplayList( displayList );
		}
	}
	
	public void apply()
	{
		mesh.apply( ipOrig, ip );
		imp.updateAndDraw();
	}
	
	private void updateRoi()
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
	
	public void keyPressed( KeyEvent e)
	{
		if ( e.getKeyCode() == KeyEvent.VK_ESCAPE || e.getKeyCode() == KeyEvent.VK_ENTER )
		{
			Thread t = opt;
			opt = null;
			t.interrupt();
			
			t = ill;
			ill = null;
			t.interrupt();
			
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
		else if ( e.getKeyCode() == KeyEvent.VK_Y )
		{
			showMesh = !showMesh;
			if ( showMesh || showPointMatches )
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
		else if ( e.getKeyCode() == KeyEvent.VK_U )
		{
			showPointMatches = !showPointMatches;
			if ( showMesh || showPointMatches )
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
			
			if ( targetIndex == -1 )
			{
				float[] l = new float[]{ xm, ym };
				synchronized ( mesh )
				{
					Model m = mesh.findClosest( l ).getModel();
					try { m.applyInverseInPlace( l ); }
					catch ( NoninvertibleModelException x ){ x.printStackTrace(); }
					Point hook = new Point( l );
					hook.apply( m );
					hooks.add( hook );
				
					mesh.addMatchWeightedByDistance( new PointMatch( new Point( new float[]{ xm, ym } ), hook, 10f ), alpha );
				}
				
				updateRoi();
			}
		}
		//IJ.log( "Mouse pressed: " + x + ", " + y + " " + modifiers( e.getModifiers() ) );
	}

	public void mouseExited( MouseEvent e ) {}
	public void mouseClicked( MouseEvent e ) {}	
	public void mouseEntered( MouseEvent e ) {}
	
	public void mouseReleased( MouseEvent e ){}
	
	public void mouseDragged( MouseEvent e )
	{
		if ( targetIndex >= 0 )
		{
			ImageWindow win = WindowManager.getCurrentWindow();
			int xm = win.getCanvas().offScreenX( e.getX() );
			int ym = win.getCanvas().offScreenY( e.getY() );
			
			float[] l = hooks.get( targetIndex ).getW();
			
			l[ 0 ] = xm;
			l[ 1 ] = ym;
			
			updateRoi();
				
			if ( showMesh || showPointMatches )
				synchronized ( ill )
				{
					pleaseIllustrate = true;
					ill.notify();
				}
			synchronized ( opt ){ opt.notify(); }
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
