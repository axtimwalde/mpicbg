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
import ij.gui.Roi;
import ij.gui.Toolbar;

import java.awt.Polygon;
import java.awt.event.MouseEvent;

/**
 * Fake the tilt-lens effect as used for 'Smallgantics' pictures
 * by applying gradually increasing smooth from a 'focal line'.
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
public class InteractiveTilt extends AbstractInteractiveBlockFilter
{	
	protected int x1, y1, x2, y2;
	
	protected Tilt tilt;
	
	@Override
	final protected void init()
	{
		tilt = Tilt.create( imp.getProcessor() );
	}
	
	@Override
	final protected void draw()
	{
		tilt.render( x1, y1, x2, y2 );
	}
	
	@Override
	final public void showHelp()
	{
		IJ.showMessage(
				"Interactive Tilt Shift",
				"Use the line tool to specify location and orientation of the tilt axis and the tilt angle." + NL +
				"ENTER - Apply" + NL +
				"ESC - Cancel" );
	}
	
	@Override
	public void run( final String arg )
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
		
		init();
		
		imp.getProcessor().snapshot();
		
		Toolbar.getInstance().setTool( Toolbar.LINE );
		
		painter = new PaintThread();
		painter.start();
	}

	@Override
	public void mouseDragged( final MouseEvent e )
	{
		final Roi roi = imp.getRoi();
		if ( roi != null )
		{
			final Polygon poly = roi.getPolygon();
			if ( roi.isLine() )
			{
				x1 = poly.xpoints[ 0 ];
				y1 = poly.ypoints[ 0 ];
				x2 = poly.xpoints[ poly.xpoints.length - 1 ];
				y2 = poly.ypoints[ poly.ypoints.length - 1 ];
			}
		}
		else
		{
			x1 = x2 = imp.getWidth() / 2;
			y1 = y2 = imp.getHeight() / 2;
				
		}
		painter.repaint();
	}
}
