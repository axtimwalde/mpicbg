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

import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/**
 * 
 *
 * @author Stephan Saalfeld &lt;saalfeld@mpi-cbg.de&gt;
 */
final public class Mean
{
	final private IntegralImage integral;
	final private ImageProcessor ip;
	
	public Mean( final ColorProcessor ip )
	{
		this.ip = ip;
		integral = new LongRGBIntegralImage( ip );
	}
	
	public Mean( final ByteProcessor ip )
	{
		this.ip = ip;
		integral = new LongIntegralImage( ip );
	}
	
	public Mean( final ShortProcessor ip )
	{
		this.ip = ip;
		integral = new LongIntegralImage( ip );
	}

	public Mean( final FloatProcessor ip )
	{
		this.ip = ip;
		integral = new DoubleIntegralImage( ip );
	}
	
	public void mean( final int blockRadiusX, final int blockRadiusY )
	{
		final int w = ip.getWidth() - 1;
		final int h = ip.getHeight() - 1;
		for ( int y = 0; y <= h; ++y )
		{
			final int yMin = Math.max( -1, y - blockRadiusY - 1 );
			final int yMax = Math.min( h, y + blockRadiusY );
			final int bh = yMax - yMin;
			for ( int x = 0; x <= w; ++x )
			{
				final int xMin = Math.max( -1, x - blockRadiusX - 1 );
				final int xMax = Math.min( w, x + blockRadiusX );
				final float scale = 1.0f / ( xMax - xMin ) / bh;
				ip.set( x, y, integral.getScaledSum( xMin, yMin, xMax, yMax, scale ) );
			}
		}
	}
	
	public void mean( final int blockRadius )
	{
		mean( blockRadius, blockRadius );
	}
	
	
	/**
	 * Factory method that decides on the type of <code>ip</code> which
	 * {@link Mean} constructor to call.  Returns <code>null</code> for unknown
	 * types.
	 * 
	 * @param ip
	 * @return
	 */
	static public Mean create( final ImageProcessor ip )
	{
		if (ip instanceof FloatProcessor)
			return new Mean( ( FloatProcessor )ip );
		else if (ip instanceof ByteProcessor)
			return new Mean( ( ByteProcessor )ip );
		else if (ip instanceof ShortProcessor)
			return new Mean( ( ShortProcessor )ip );
		else if (ip instanceof ColorProcessor)
			return new Mean( ( ColorProcessor )ip );
		else
			return null;
	}
}
