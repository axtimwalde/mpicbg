package mpicbg.ij;

import java.util.concurrent.atomic.AtomicBoolean;

import ij.ImagePlus;
import ij.process.ImageProcessor;

public class MappingThread extends Thread
{
	final protected ImagePlus imp;
	final protected ImageProcessor source;
	final protected ImageProcessor target;
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
		this.pleaseRepaint = pleaseRepaint;
		this.mapping = mapping;
		this.setName( "PaintInvertibleCoordinateTransformThread" );
		this.interpolate = interpolate;
	}
	
	@Override
	public void run()
	{
		while ( !isInterrupted() )
		{
			try
			{
				if ( pleaseRepaint.compareAndSet( true, false ) )
				{
					if ( interpolate )
						mapping.mapInterpolated( source, target );
					else
						mapping.map( source, target );
					imp.updateAndDraw();
				}
				else
					synchronized ( this ){ wait(); }
			}
			catch ( InterruptedException e){ Thread.currentThread().interrupt(); }
		}
	}
}
