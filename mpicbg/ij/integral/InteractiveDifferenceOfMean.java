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
 */
package mpicbg.ij.integral;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

import java.awt.Canvas;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * 
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
public class InteractiveDifferenceOfMean implements KeyListener, MouseListener, MouseMotionListener, PlugIn
{
	final static private String NL = System.getProperty( "line.separator" );
	
	private int blockRadiusX1 = 0, blockRadiusY1 = 0, blockRadiusX2 = 0, blockRadiusY2 = 0;
	private ImageJ ij;
	private ImagePlus imp;
	private ImageWindow window;
	private Canvas canvas;
	private DifferenceOfMean dom;
	private PaintThread painter;
	private double min;
	private double max;
	
	@Override
	public void run( String arg )
	{
		ij = IJ.getInstance();
		imp = IJ.getImage();
		window = imp.getWindow();
		canvas = imp.getCanvas();
		
		canvas.addKeyListener( this );
		window.addKeyListener( this );
		canvas.addMouseMotionListener( this );
		canvas.addMouseListener( this );
		ij.addKeyListener( this );
		
		final ImageProcessor ip = imp.getProcessor();
		min = ip.getMin();
		max = ip.getMax();
		ip.snapshot();
		if ( imp.getType() == ImagePlus.GRAY32 )
			ip.setMinAndMax( -max / 2.0, max / 2.0 );
		else if ( imp.getType() == ImagePlus.GRAY16 )
			ip.setMinAndMax( 32767 - max / 2.0, 32767 + max / 2.0 );

		dom = DifferenceOfMean.create( ip );
		
		Toolbar.getInstance().setTool( Toolbar.RECTANGLE );
		
		painter = new PaintThread();
		painter.start();
		
	}
	
	final private void draw()
	{
		dom.differenceOfMean( blockRadiusX1, blockRadiusY1, blockRadiusX2, blockRadiusY2 );
	}
	
	public class PaintThread extends Thread
	{
		private boolean pleaseRepaint;
		
		PaintThread()
		{
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
					draw();
					imp.updateAndDraw();
				}
				synchronized ( this )
				{
					try
					{
						if ( !pleaseRepaint ) wait();
					}
					catch ( InterruptedException e ){}
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
	}
	
	public void keyPressed( KeyEvent e )
	{
		if ( e.getKeyCode() == KeyEvent.VK_ESCAPE || e.getKeyCode() == KeyEvent.VK_ENTER )
		{
			painter.interrupt();
			
			canvas.removeKeyListener( this );
			window.removeKeyListener( this );
			ij.removeKeyListener( this );
			canvas.removeMouseListener( this );
			canvas.removeMouseMotionListener( this );
			
			if ( imp != null )
			{
				if ( e.getKeyCode() == KeyEvent.VK_ESCAPE )
				{
					final ImageProcessor ip = imp.getProcessor();
					ip.reset();
					ip.setMinAndMax( min, max );
				}
				else if ( e.getKeyCode() == KeyEvent.VK_ENTER )
				{
				}
			}
			imp.updateAndDraw();
		}
		else if ( e.getKeyCode() == KeyEvent.VK_F1 )
		{
			IJ.showMessage(
					"Interactive Mean Smooth",
					"Click and drag to change the size of the smoothing kernel." + NL +
					"ENTER - Apply" + NL +
					"ESC - Cancel" );
		}
	}
	
	public void keyReleased( KeyEvent e ) {}
	public void keyTyped( KeyEvent e ) {}

	public void mouseDragged( final MouseEvent e )
	{
		final Roi roi = imp.getRoi();
		if ( roi != null )
		{
			final Rectangle bounds = imp.getRoi().getBounds();
			if ( ( e.getModifiers() & InputEvent.SHIFT_DOWN_MASK ) == 0 )
			{
				blockRadiusX2 = bounds.width / 2;
				blockRadiusY2 = bounds.height / 2;				
				blockRadiusX1 = Math.round( blockRadiusX2 * 0.5f );
				blockRadiusY1 = Math.round( blockRadiusY2 * 0.5f );
			}
			else
			{
				blockRadiusX1 = bounds.width / 2;
				blockRadiusY1 = bounds.height / 2;
			}
		}
		else
		{
			if ( ( e.getModifiers() & InputEvent.SHIFT_DOWN_MASK ) == 0 )
			{
				blockRadiusX1 = 0;
				blockRadiusY1 = 0;
			}
			else
			{
				blockRadiusX2 = 0;
				blockRadiusY2 = 0;				
			}
		}
		painter.repaint();
	}

	public void mouseMoved( MouseEvent e ){}
	public void mouseClicked( MouseEvent e ){}
	public void mouseEntered( MouseEvent e ){}
	public void mouseExited( MouseEvent e ){}
	public void mouseReleased( MouseEvent e ){}
	public void mousePressed( MouseEvent e )
	{
		mouseDragged( e );
	}
}
