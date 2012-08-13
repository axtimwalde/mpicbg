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
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * 
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
final public class InteractiveDifferenceOfMean extends AbstractInteractiveBlockFilter implements MouseWheelListener
{
	private int blockRadiusX1 = 0, blockRadiusY1 = 0, blockRadiusX2 = 0, blockRadiusY2 = 0;
	private DifferenceOfMean dom;
	private double min;
	private double max;
	final static private int dpScale = 10;
	private int dp = -1 * dpScale;
	
	final private static float d( final int dp )
	{
		return ( float )Math.pow( 2.0, ( double )dp / dpScale );
	}
	
	@Override
	final protected void init()
	{
		window.addMouseWheelListener( this );
		
		final ImageProcessor ip = imp.getProcessor();
		min = ip.getMin();
		max = ip.getMax();
		ip.snapshot();
		if ( imp.getType() == ImagePlus.GRAY32 )
			ip.setMinAndMax( ( min - max ) / 2.0, ( max - min ) / 2.0 );
		else if ( imp.getType() == ImagePlus.GRAY16 )
			ip.setMinAndMax( 32767 - ( max - min ) / 2.0, 32767 + ( max - min ) / 2.0 );

		dom = DifferenceOfMean.create( ip );
		
		dp = -1 * dpScale;
	}
	
	@Override
	final protected void draw()
	{
		dom.differenceOfMean( blockRadiusX1, blockRadiusY1, blockRadiusX2, blockRadiusY2 );
	}
	
	@Override
	final protected void showHelp()
	{
		IJ.showMessage(
				"Interactive Difference of Means",
				"Click and drag to change the size of the smoothing kernel." + NL +
				"ENTER - Apply" + NL +
				"ESC - Cancel" );
	}
	
	public void keyPressed( KeyEvent e )
	{
		if ( e.getKeyCode() == KeyEvent.VK_ESCAPE || e.getKeyCode() == KeyEvent.VK_ENTER )
		{
			painter.interrupt();
			
			canvas.removeKeyListener( this );
			window.removeKeyListener( this );
			window.removeMouseWheelListener( this );
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
			showHelp();
	}
	
	public void mouseWheelMoved( final MouseWheelEvent e )
	{
		final int s = e.getWheelRotation();
		if ( s < 0 )
		{
			e.consume();
			++dp;
		}
		else
		{
			e.consume();
			--dp;
		}
		blockRadiusX1 = Math.round( blockRadiusX2 * d( dp ) );
		blockRadiusY1 = Math.round( blockRadiusY2 * d( dp ) );
		
		painter.repaint();
	}
	
	public void mouseDragged( final MouseEvent e )
	{
		final Roi roi = imp.getRoi();
		if ( roi != null )
		{
			final Rectangle bounds = imp.getRoi().getBounds();
			blockRadiusX2 = bounds.width / 2;
			blockRadiusY2 = bounds.height / 2;				
			blockRadiusX1 = Math.round( blockRadiusX2 * d( dp ) );
			blockRadiusY1 = Math.round( blockRadiusY2 * d( dp ) );
			
		}
		else
		{
			blockRadiusX1 = 0;
			blockRadiusY1 = 0;
			blockRadiusX2 = 0;
			blockRadiusY2 = 0;				
		}
		painter.repaint();
	}
}
