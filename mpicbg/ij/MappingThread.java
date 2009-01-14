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
package mpicbg.ij;

import java.awt.Canvas;
import java.awt.Cursor;
import java.util.concurrent.atomic.AtomicBoolean;

import ij.ImagePlus;
import ij.process.ImageProcessor;

public class MappingThread extends Thread
{
	final protected ImagePlus imp;
	final protected ImageProcessor source;
	final protected ImageProcessor target;
	final protected ImageProcessor temp;
	final protected AtomicBoolean pleaseRepaint;
	final protected Mapping mapping;
	final protected boolean interpolate;
	
	public MappingThread(
			final ImagePlus imp,
			final ImageProcessor source,
			final ImageProcessor target,
			final AtomicBoolean pleaseRepaint,
			final Mapping mapping,
			final boolean interpolate )
	{
		this.imp = imp;
		this.source = source;
		this.target = target;
		this.temp = target.createProcessor( target.getWidth(), target.getHeight() );
		this.pleaseRepaint = pleaseRepaint;
		this.mapping = mapping;
		this.interpolate = interpolate;
		this.setName( "MappingThread" );
	}
	
	@Override
	public void run()
	{
		while ( !isInterrupted() )
		{
			final Canvas canvas = imp.getCanvas();
			final Cursor cursor = canvas == null ? canvas.getCursor() : Cursor.getDefaultCursor();
			try
			{
				if ( pleaseRepaint.compareAndSet( true, false ) )
				{
					if ( canvas != null )
						canvas.setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );
					if ( interpolate )
						mapping.mapInterpolated( source, temp );
					else
						mapping.map( source, temp );
					if ( !pleaseRepaint.get() )
					{
						final Object targetPixels = target.getPixels();
						target.setPixels( temp.getPixels() );
						temp.setPixels( targetPixels );
						imp.updateAndDraw();
					}
				}
				else
					synchronized ( this ){ wait(); }
			}
			catch ( InterruptedException e ){ Thread.currentThread().interrupt(); }
			finally
			{
				if ( canvas != null )
					canvas.setCursor( cursor );				
			}
		}
	}
}
