/**
 * 
 */
package mpicbg.models;

import java.util.concurrent.atomic.AtomicBoolean;

import ij.ImagePlus;
import ij.process.ImageProcessor;

public class PaintInvertibleCoordinateTransformThread extends Thread
{
	final protected ImagePlus imp;
	final protected ImageProcessor source;
	final protected ImageProcessor target;
	final protected AtomicBoolean pleaseRepaint;
	final protected InvertibleCoordinateTransform transform;
	
	public PaintInvertibleCoordinateTransformThread(
			ImagePlus imp,
			ImageProcessor source,
			ImageProcessor target,
			AtomicBoolean pleaseRepaint,
			InvertibleCoordinateTransform transform )
	{
		this.imp = imp;
		this.source = source;
		this.target = target;
		this.pleaseRepaint = pleaseRepaint;
		this.transform = transform;
		this.setName( "PaintInvertibleCoordinateTransformThread" );
	}
	
	public void run()
	{
		while ( !isInterrupted() )
		{
			try
			{
				if ( pleaseRepaint.compareAndSet( true, false ) )
				{
					for ( int y = 0; y < target.getHeight(); ++y )
					{
						for ( int x = 0; x < target.getWidth(); ++x )
						{
							float[] t = new float[]{ x, y };
							try
							{
								transform.applyInverseInPlace( t );
								target.putPixel( x, y, source.getPixel( ( int )t[ 0 ], ( int )t[ 1 ] ) );
							}
							catch ( NoninvertibleModelException e ){ e.printStackTrace(); }
						}
						imp.updateAndDraw();
					}
				}
				else
					synchronized ( this ){ wait(); }
			}
			catch ( InterruptedException e){ Thread.currentThread().interrupt(); }
		}
	}
}
