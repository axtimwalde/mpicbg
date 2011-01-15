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
package mpicbg.ij.clahe;

import ij.process.ByteProcessor;
import ij.process.FloatProcessor;

/**
 * Apply a piece of CLAHE to a {@link FloatProcessor}.
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.3b
 */
final class FloatApply extends Apply< FloatProcessor >
{
	final protected float[] ipPixels;
    
	public FloatApply(
			final FloatProcessor ip,
			final ByteProcessor src,
			final ByteProcessor dst,
			final ByteProcessor mask,
			final int boxXMin,
			final int boxYMin,
			final int boxXMax,
			final int boxYMax ) throws Exception
	{
		super( ip, src, dst, mask, boxXMin, boxYMin, boxXMax, boxYMax );
		ipPixels = ( float[] )ip.getPixels();
	}
	
	@Override
	final public void apply( final int cellXMin, final int cellYMin, final int cellXMax, final int cellYMax )
	{
		final int xMin = Math.max( boxXMin, cellXMin );
		final int yMin = Math.max( boxYMin, cellYMin );
		final int xMax = Math.min( boxXMax, cellXMax );
		final int yMax = Math.min( boxYMax, cellYMax );
	
		final float min = ( float )ip.getMin();
		for ( int y = yMin; y < yMax; ++y )
		{
			int i = y * width + xMin;
			for ( int x = xMin; x < xMax; ++x )
			{
				final float m = ( maskPixels[ i ] & 0xff ) / 255.0f;
				final float v = ipPixels[ i ];
				final float vSrc = srcPixels[ i ] & 0xff;
				final float a;
				if ( vSrc == 0 )
					a = 1.0f;
				else
					a = ( float )( dstPixels[ i ] & 0xff ) / vSrc;
				ipPixels[ i ] = m * ( a * ( v - min ) + min - v ) + v;
				++i;
			}
		}
	}
}
