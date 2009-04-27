package mpicbg.ij;

import java.awt.Canvas;
import java.awt.Cursor;
import java.util.concurrent.atomic.AtomicBoolean;

import ij.ImagePlus;
import ij.process.ImageProcessor;

/**
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 */
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
			final Mapping< ? > mapping,
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
