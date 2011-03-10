package mpicbg.ij;

import java.util.concurrent.atomic.AtomicBoolean;
import mpicbg.models.InverseCoordinateTransform;
import ij.ImagePlus;
import ij.process.ImageProcessor;

/**
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 */
public class PaintInvertibleCoordinateTransformThread extends Thread
{
	final protected ImagePlus imp;
	final protected ImageProcessor source;
	final protected ImageProcessor target;
	final protected AtomicBoolean pleaseRepaint;
	final protected InverseCoordinateTransform transform;
	final protected InverseTransformMapping< ? > mapping;
	
	public PaintInvertibleCoordinateTransformThread(
			ImagePlus imp,
			ImageProcessor source,
			ImageProcessor target,
			AtomicBoolean pleaseRepaint,
			InverseCoordinateTransform transform )
	{
		this.imp = imp;
		this.source = source;
		this.target = target;
		this.pleaseRepaint = pleaseRepaint;
		this.transform = transform;
		this.mapping = new InverseTransformMapping< InverseCoordinateTransform >( transform );
		this.setName( "PaintInvertibleCoordinateTransformThread" );
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
