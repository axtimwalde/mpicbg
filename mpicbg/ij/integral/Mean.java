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
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
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
	
	final public void mean( final int blockRadiusX, final int blockRadiusY )
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
	
	final public void mean( final int blockRadius )
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
	final static public Mean create( final ImageProcessor ip )
	{
		if ( FloatProcessor.class.isInstance( ip ) )
			return new Mean( ( FloatProcessor )ip );
		else if ( ByteProcessor.class.isInstance( ip ) )
			return new Mean( ( ByteProcessor )ip );
		else if ( ShortProcessor.class.isInstance( ip ) )
			return new Mean( ( ShortProcessor )ip );
		else if ( ColorProcessor.class.isInstance( ip ) )
			return new Mean( ( ColorProcessor )ip );
		else
			return null;
	}
}
