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
import ij.process.ColorProcessor;

/**
 * Apply a piece of CLAHE to a {@link ColorProcessor}.
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.3b
 */
final class RGBApply extends Apply< ColorProcessor >
{
	final protected int[] ipPixels;
    
	public RGBApply(
			final ColorProcessor ip,
			final ByteProcessor src,
			final ByteProcessor dst,
			final ByteProcessor mask,
			final int boxXMin,
			final int boxYMin,
			final int boxXMax,
			final int boxYMax ) throws Exception
	{
		super( ip, src, dst, mask, boxXMin, boxYMin, boxXMax, boxYMax );
		ipPixels = ( int[] )ip.getPixels();
	}
	
	@Override
	final public void apply( final int cellXMin, final int cellYMin, final int cellXMax, final int cellYMax )
	{
		final int xMin = Math.max( boxXMin, cellXMin );
		final int yMin = Math.max( boxYMin, cellYMin );
		final int xMax = Math.min( boxXMax, cellXMax );
		final int yMax = Math.min( boxYMax, cellYMax );
	
		for ( int y = yMin; y < yMax; ++y )
		{
			int i = y * width + xMin;
			for ( int x = xMin; x < xMax; ++x )
			{
				final float m = ( maskPixels[ i ] & 0xff ) / 255.0f;
				final int argb = ipPixels[ i ];
				final float vr = ( argb >> 16 ) & 0xff;
				final float vg = ( argb >> 8 ) & 0xff;
				final float vb = argb & 0xff;
				final float vSrc = srcPixels[ i ] & 0xff;
				final float a;
				if ( vSrc == 0 )
					a = 1.0f;
				else
					a = ( float )( dstPixels[ i ] & 0xff ) / vSrc;
				final float br = vr * ( 1.0f + m * ( a - 1.0f ) );
				final float bg = vg * ( 1.0f + m * ( a - 1.0f ) );
				final float bb = vb * ( 1.0f + m * ( a - 1.0f ) );
				
				final int r = Math.max( 0, Math.min( 255, mpicbg.util.Util.roundPos( br ) ) );  
				final int g = Math.max( 0, Math.min( 255, mpicbg.util.Util.roundPos( bg ) ) );
				final int b = Math.max( 0, Math.min( 255, mpicbg.util.Util.roundPos( bb ) ) );
				ipPixels[ i ] = ( r << 16 ) | ( g << 8 ) | b;
				++i;
			}
		}
	}
}
