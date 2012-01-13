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
import ij.ImageStack;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.process.Blitter;
import ij.process.FloatProcessor;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
public class InteractivePMCC extends AbstractInteractiveBlockFilter
{
	protected BlockPMCC bc;
	private int offsetX = 0, offsetY = 0;
	private AtomicBoolean move = new AtomicBoolean( true );
	
	private ImageStack stackOriginal;
	private FloatProcessor fpR, fpR2;
	
	@Override
	final protected void init()
	{
		stackOriginal = imp.getStack();
		
		final FloatProcessor fp1, fp2;
		if ( imp.getType() == ImagePlus.GRAY32 )
		{
			fp1 = ( FloatProcessor )stackOriginal.getProcessor( 1 ).duplicate();
			fp2 = ( FloatProcessor )stackOriginal.getProcessor( 2 ).duplicate();
		}
		else
		{
			fp1 = ( FloatProcessor )stackOriginal.getProcessor( 1 ).convertToFloat();
			fp2 = ( FloatProcessor )stackOriginal.getProcessor( 2 ).convertToFloat();
		}
		
		bc = new BlockPMCC( fp1, fp2 );
		
		fpR = bc.getTargetProcessor();
		fpR.setMinAndMax( -1.0, 1.0 );
		fpR2 = ( FloatProcessor )fpR.duplicate();
		final ImageStack stack = new ImageStack( fpR.getWidth(), fpR.getHeight() );
		stack.addSlice( "r", fpR2 );
		
		imp.setStack( stack );
	}
	
	@Override
	public void run( String arg )
	{
		ij = IJ.getInstance();
		imp = IJ.getImage();
		
		if ( imp.getStackSize() < 2 )
		{
			IJ.error( "This plugin only works on stacks with at least two slices." );
			return;
		}
		window = imp.getWindow();
		canvas = imp.getCanvas();
		
		canvas.addKeyListener( this );
		window.addKeyListener( this );
		canvas.addMouseMotionListener( this );
		canvas.addMouseListener( this );
		ij.addKeyListener( this );
		
		init();
		
		imp.getProcessor().snapshot();
		
		Toolbar.getInstance().setTool( Toolbar.RECTANGLE );
		
		painter = new PaintThread();
		painter.start();
	}
	
	protected void calculate()
	{
		bc.r( blockRadiusX, blockRadiusY );
	}
	
	@Override
	final protected void draw()
	{
		final int shiftX = Math.max( 0, offsetX );
		final int shiftY = Math.max( 0, offsetY );
		
		if ( move.compareAndSet( true, false ) )
			bc.setOffset( offsetX, offsetY );
		
		for ( int i = fpR2.getPixelCount() - 1; i >= 0;--i )
		{
			fpR.setf( i, 0 );
			fpR2.setf( i, 0 );
		}
		
		calculate();
		
		fpR2.copyBits( fpR, shiftX, shiftY, Blitter.COPY );
	}
	
	@Override
	protected void showHelp()
	{
		IJ.showMessage(
				"Interactive Block Correlation",
				"Click and drag to change the block size." + NL +
				"ENTER - Apply" + NL +
				"ESC - Cancel" );
	}
	
	@Override
	final public void mouseDragged( final MouseEvent e )
	{
		final Roi roi = imp.getRoi();
		if ( roi != null )
		{
			final Rectangle bounds = imp.getRoi().getBounds();
			blockRadiusX = bounds.width / 2;
			blockRadiusY = bounds.height / 2;
			
			offsetX = bounds.x + ( bounds.width - imp.getWidth() ) / 2;
			offsetY = bounds.y + ( bounds.height - imp.getHeight() ) / 2;
			
			move.set( true );
		}
		else
		{
			blockRadiusX = 0;
			blockRadiusY = 0;	
		}
		painter.repaint();
	}
	
	@Override
	final public void keyPressed( final KeyEvent e )
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
					imp.setStack( stackOriginal );
				}
				else if ( e.getKeyCode() == KeyEvent.VK_ENTER )
				{
				}
			}
			imp.updateAndDraw();
		}
		else if (
				e.getKeyCode() == KeyEvent.VK_LEFT ||
				e.getKeyCode() == KeyEvent.VK_RIGHT ||
				e.getKeyCode() == KeyEvent.VK_UP ||
				e.getKeyCode() == KeyEvent.VK_DOWN )
		{
			if ( e.getKeyCode() == KeyEvent.VK_LEFT )
				--offsetX;
			else if ( e.getKeyCode() == KeyEvent.VK_RIGHT )
				++offsetX;
			else if ( e.getKeyCode() == KeyEvent.VK_UP )
				--offsetY;
			else if ( e.getKeyCode() == KeyEvent.VK_DOWN )
				++offsetY;
			
			move.set( true );
			painter.repaint();
		}
		else if ( e.getKeyCode() == KeyEvent.VK_F1 )
			showHelp();
	}
}
