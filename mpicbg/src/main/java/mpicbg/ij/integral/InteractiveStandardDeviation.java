/*-
 * #%L
 * MPICBG Core Library.
 * %%
 * Copyright (C) 2008 - 2025 Stephan Saalfeld et. al.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
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

import java.awt.event.KeyEvent;

import ij.IJ;
import ij.ImagePlus;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/**
 * 
 *
 * @author Stephan Saalfeld &lt;saalfeld@mpi-cbg.de&gt;
 */
final public class InteractiveStandardDeviation extends AbstractInteractiveBlockFilter
{
	private FloatProcessor fp;
	private ImageProcessor ipOriginal;
	private BlockStatistics std;
	
	@Override
	final protected void init()
	{
		ipOriginal = imp.getProcessor();
		if ( imp.getType() == ImagePlus.GRAY32 )
			fp = ( FloatProcessor )ipOriginal.duplicate();
		else
			fp = ( FloatProcessor )ipOriginal.convertToFloat();
		
		imp.setProcessor( fp );
		
		std = new BlockStatistics( fp );
	}
	
	@Override
	final protected void draw()
	{
		std.std( blockRadiusX, blockRadiusY );
	}
	
	@Override
	final protected void showHelp()
	{
		IJ.showMessage(
				"Interactive Block Standard Deviation",
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
			ij.removeKeyListener( this );
			canvas.removeMouseListener( this );
			canvas.removeMouseMotionListener( this );
			
			if ( imp != null )
			{
				if ( e.getKeyCode() == KeyEvent.VK_ESCAPE )
				{
					imp.setProcessor( ipOriginal );
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
}
