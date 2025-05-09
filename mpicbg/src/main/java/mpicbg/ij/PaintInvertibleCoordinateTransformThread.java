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
package mpicbg.ij;

import java.util.concurrent.atomic.AtomicBoolean;
import mpicbg.models.InverseCoordinateTransform;
import ij.ImagePlus;
import ij.process.ImageProcessor;

/**
 * 
 * @author Stephan Saalfeld &lt;saalfeld@mpi-cbg.de&gt;
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
