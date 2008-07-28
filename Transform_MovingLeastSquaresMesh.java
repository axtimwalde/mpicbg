/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 *
 */
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;
import ij.process.*;
import ij.gui.*;

import mpicbg.models.*;

import java.awt.Color;
import java.awt.Event;
import java.awt.TextField;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;
import java.util.ArrayList;

public class Transform_MovingLeastSquaresMesh implements PlugIn, MouseListener,  MouseMotionListener, KeyListener
{
	public static final String NL = System.getProperty( "line.separator" );
	public final static String man =
		"Add some control points with your mouse" + NL +
		"and drag them to deform the image." + NL + " " + NL +
		"ENTER - Apply the deformation." + NL +
		"ESC - Return to the original image." + NL +
		"Y - Toggle mesh display.";
	
	// number of vertices in horizontal direction
	private static int numX = 16;
	// alpha [0 smooth, 1 less smooth ;)]
	private static float alpha = 1.0f;
	// local transformation model
	final static String[] methods = new String[]{ "Translation", "Rigid", "Affine" };
	private static int method = 1;
	
	ImagePlus imp;
	ImageProcessor ip;
	ImageProcessor ipOrig;
	
	//final ArrayList< PointMatch > pq = new ArrayList< PointMatch >();
	final ArrayList< Point > hooks = new ArrayList< Point >();
	PointRoi handles;
	
	protected MovingLeastSquaresMesh< ? extends AbstractAffineModel2D > mesh;
	
	int targetIndex = -1;
	
	boolean showMesh = false;
	
	public void run( String arg )
    {
		hooks.clear();
		
		imp = IJ.getImage();
		ip = imp.getProcessor();
		ipOrig = ip.duplicate();
		
		final GenericDialog gd = new GenericDialog( "Moving Least Squares Transform" );
		gd.addNumericField( "Vertices_per_row :", numX, 0 );
		//gd.addNumericField( "vertical_handles :", numY, 0 );
		gd.addNumericField( "Alpha :", alpha, 2 );
		gd.addChoice( "Local_transformation :", methods, methods[ 1 ] );
		gd.addMessage( man );
		gd.showDialog();
		
		if (gd.wasCanceled()) return;
		
		numX = ( int )gd.getNextNumber();
		alpha = ( float )gd.getNextNumber();
		
		method = gd.getNextChoiceIndex();
		
		// TODO Implement other models for choice
		switch ( method )
		{
		case 0:
			mesh = new MovingLeastSquaresMesh< TranslationModel2D >( TranslationModel2D.class, numX, imp.getWidth(), imp.getHeight() );
			break;
		case 1:
			mesh = new MovingLeastSquaresMesh< RigidModel2D >( RigidModel2D.class, numX, imp.getWidth(), imp.getHeight() );
			break;
		case 2:
			mesh = new MovingLeastSquaresMesh< AffineModel2D >( AffineModel2D.class, numX, imp.getWidth(), imp.getHeight() );
			break;
		default:
			return;
		}
		
		handles = new PointRoi(
				new int[]{ ip.getWidth() / 4, 3 * ip.getWidth() / 4, ip.getWidth() / 4 },
				new int[]{ ip.getHeight() / 4, ip.getHeight() / 2, 3 * ip.getHeight() / 4 }, hooks.size() );
		imp.setRoi( handles );
		
		Toolbar.getInstance().setTool( Toolbar.getInstance().addTool( "Add_and_drag_handles." ) );
		
		imp.getCanvas().addMouseListener( this );
		imp.getCanvas().addMouseMotionListener( this );
		imp.getCanvas().addKeyListener( this );
    }
	
	void updateIllustration()
	{
		if ( showMesh )
			imp.getCanvas().setDisplayList( mesh.illustrateMesh(), Color.white, null );
		else
			imp.getCanvas().setDisplayList( null );
	}
	
	public void apply()
	{
		mesh.paint( ipOrig, ip );
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
					InvertibleModel m = ( InvertibleModel )mesh.findClosest( l ).getModel();
					try
					{
						m.applyInverseInPlace( l );
						Point here = new Point( l );
						Point there = new Point( l );
						hooks.add( here );
						here.apply( m );
						mesh.addMatchWeightedByDistance( new PointMatch( there, here, 10f ), alpha );
					}
					catch ( NoninvertibleModelException x ){ x.printStackTrace(); }
				}
				updateRoi();
			}
		}
	}

	public void mouseExited( MouseEvent e ) {}
	public void mouseClicked( MouseEvent e ) {}	
	public void mouseEntered( MouseEvent e ) {}
	
	public void mouseReleased( MouseEvent e )
	{
		updateIllustration();
		apply();
	}
	
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
			
			try
			{
				mesh.updateModels();
			}
			catch ( NotEnoughDataPointsException x ){ x.printStackTrace(); }
			
			updateRoi();
			updateIllustration();
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
