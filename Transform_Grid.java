import ij.IJ;
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

public class Transform_Grid implements PlugIn, MouseListener,  MouseMotionListener, KeyListener
{
	// number of x-handles
	private static int numX = 4;
	// number of x-handles
	private static int numY = 3;
	
	ImagePlus imp;
	ImageProcessor ip;
	ImageProcessor ipOrig;
	
	Point[] p;
	Point[] q;
	PointRoi handles;
	
	final ArrayList< ArrayList< PointMatch > > m = new ArrayList< ArrayList< PointMatch > >();;
	final ArrayList< HomographyModel2D > h = new ArrayList< HomographyModel2D >();
	
	int targetIndex = -1;
	
	public void run( String arg )
    {
		// cleanup
		m.clear();
		
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
		
		p = new Point[ numX * numY ];
		q = new Point[ p.length ];
		int[] x = new int[ p.length ];
		int[] y = new int[ p.length ];
		
		float dy = ( float )ip.getHeight() / ( numY - 1 );
		float dx = ( float )ip.getWidth() / ( numX - 1 );
		
		int i = 0;
		for ( int yi = 0; yi < numY; ++yi )
		{
			float yip = yi * dy;
			for ( int xi = 0; xi < numX; ++xi )
			{
				float xip = xi * dx;
				p[ i ]  = new Point( new float[]{ xip, yip } );
				q[ i ] = p[ i ].clone();
				
				x[ i ] = ( int )( xip );
				y[ i ] = ( int )( yip );
				
				++i;
			}
		}
		
		for ( int yi = 1; yi < numY; ++yi )
		{
			int yio = yi * numX; 
			for ( int xi = 1; xi < numX; ++xi )
			{
				i = yio + xi;
				
				ArrayList< PointMatch > mm = new ArrayList< PointMatch >(); 
				mm.add( new PointMatch( p[ i ], q[ i ] ) );
				mm.add( new PointMatch( p[ i - 1 ], q[ i - 1 ] ) );
				mm.add( new PointMatch( p[ i - numX - 1 ], q[ i - numX - 1 ] ) );
				mm.add( new PointMatch( p[ i - numX ], q[ i - numX ] ) );
				
				m.add( mm );
				
				h.add( new HomographyModel2D() );
			}
		}
		
		handles = new PointRoi( x, y, x.length );
		imp.setRoi( handles );
		
		Toolbar.getInstance().setTool( Toolbar.getInstance().addTool( "Drag_the_handles." ) );
		
		imp.getCanvas().addMouseListener( this );
		imp.getCanvas().addMouseMotionListener( this );
		imp.getCanvas().addKeyListener( this );
    }
	
	public void apply()
	{
		for ( int i = 0; i < h.size(); ++i )
		{
			try
			{
				HomographyModel2D g = h.get( i );
				ArrayList< PointMatch > l = m.get( i );
				g.fit( l );
				
				int minX = Integer.MAX_VALUE;
				int minY = Integer.MAX_VALUE;
				int maxX = Integer.MIN_VALUE;
				int maxY = Integer.MIN_VALUE;
				
				for ( PointMatch r : l )
				{
					float[] t = r.getP2().getW();
					if ( t[ 0 ] < minX ) minX = ( int )t[ 0 ];
					if ( t[ 0 ] > maxX ) maxX = ( int )t[ 0 ];
					if ( t[ 1 ] < minY ) minY = ( int )t[ 1 ];
					if ( t[ 1 ] > maxY ) maxY = ( int )t[ 1 ];
				}
				
				for ( int y = minY; y <= maxY; ++y )
				{
X:					for ( int x = minX; x <= maxX; ++x )
					{
						for ( int j = 0; j < l.size(); ++j )
						{
							PointMatch r1 = l.get( j );
							PointMatch r2 = l.get( ( j + 1 ) % l.size() );
							float[] t1 = r1.getP2().getW();	
							float[] t2 = r2.getP2().getW();
							
							float x1 = t2[ 0 ] - t1[ 0 ];
							float y1 = t2[ 1 ] - t1[ 1 ];
							float x2 = ( float )x - t1[ 0 ];
							float y2 = ( float )y - t1[ 1 ];
							
							if ( x1 * y2 - y1 * x2 < 0 ) continue X;
						}
						float[] t = new float[]{ x, y };
						//System.out.println( t[ 0 ] + " " + t[ 1 ] );
						g.applyInverseInPlace( t );
						ip.putPixel( x, y, ipOrig.getPixel( ( int )t[ 0 ], ( int )t[ 1 ] ) );
						//System.out.println( t[ 0 ] + " " + t[ 1 ] );
					}
				}
				
				imp.updateAndDraw();
			}
			catch ( Exception e )
			{
				IJ.error( e.getMessage() );
				e.printStackTrace( System.err );
			}
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
			
			handles = new PointRoi( rx, ry, q.length );
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
