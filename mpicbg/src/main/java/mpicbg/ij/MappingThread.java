package mpicbg.ij;

import ij.CompositeImage;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

import java.awt.Canvas;
import java.awt.Cursor;
import java.util.concurrent.atomic.AtomicBoolean;

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
	final protected Mapping< ? > mapping;
	final protected boolean interpolate;
	final protected int stackIndex;
	
	public MappingThread(
			final ImagePlus imp,
			final ImageProcessor source,
			final ImageProcessor target,
			final AtomicBoolean pleaseRepaint,
			final Mapping< ? > mapping,
			final boolean interpolate,
			final int stackIndex )
	{
		this.imp = imp;
		this.source = source;
		this.target = target;
		this.temp = target.createProcessor( target.getWidth(), target.getHeight() );
		temp.snapshot();
		this.pleaseRepaint = pleaseRepaint;
		this.mapping = mapping;
		this.interpolate = interpolate;
		this.setName( "MappingThread" );
		this.stackIndex = stackIndex;
	}
	
	public MappingThread(
			final ImagePlus imp,
			final ImageProcessor source,
			final ImageProcessor target,
			final AtomicBoolean pleaseRepaint,
			final Mapping< ? > mapping,
			final boolean interpolate )
	{
		this( imp, source, target, pleaseRepaint, mapping, interpolate, 0 );
	}
	
	@Override
	public void run()
	{
		final ImageStack stack = imp.getStack();
		while ( !isInterrupted() )
		{
			final Canvas canvas = imp.getCanvas();
			final Cursor cursor = canvas == null ? Cursor.getDefaultCursor() : canvas.getCursor();
			try
			{
				if ( pleaseRepaint.getAndSet( false ) )
				{
					if ( canvas != null )
						canvas.setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );
					temp.reset();
					if ( interpolate )
						mapping.mapInterpolated( source, temp );
					else
						mapping.map( source, temp );
					if ( !pleaseRepaint.get() )
					{
						final Object targetPixels = target.getPixels();
						target.setPixels( temp.getPixels() );
						temp.setPixels( targetPixels );
						if ( stackIndex > 0 && imp.isComposite() )
						{
							final CompositeImage cimp = ( CompositeImage )imp;
							stack.setPixels( target.getPixels(), stackIndex );
							cimp.setChannelsUpdated();
						}
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
