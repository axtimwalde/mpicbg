package mpicbg.ij;

import java.awt.Canvas;
import java.awt.Cursor;

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
	final protected Mapping< ? > mapping;
	final protected boolean interpolate;
	private boolean pleaseRepaint;
	
	public MappingThread(
			final ImagePlus imp,
			final ImageProcessor source,
			final ImageProcessor target,
			final Mapping< ? > mapping,
			final boolean interpolate )
	{
		this.imp = imp;
		this.source = source;
		this.target = target;
		this.temp = target.createProcessor( target.getWidth(), target.getHeight() );
		temp.snapshot();
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
			
			final boolean b;
			synchronized ( this )
			{
				b = pleaseRepaint;
				pleaseRepaint = false;
			}
			if ( b )
			{
				if ( canvas != null )
					canvas.setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );
				temp.reset();
				if ( interpolate )
					mapping.mapInterpolated( source, temp );
				else
					mapping.map( source, temp );
				
				final Object targetPixels = target.getPixels();
				target.setPixels( temp.getPixels() );
				temp.setPixels( targetPixels );
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
			
			if ( canvas != null )
				canvas.setCursor( cursor );
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
