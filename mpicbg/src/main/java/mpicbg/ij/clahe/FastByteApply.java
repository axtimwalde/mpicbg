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

/**
 * Apply a piece of CLAHE to a {@link ByteProcessor} for ip.equals( src ).
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.3b
 */
final class FastByteApply extends Apply< ByteProcessor >
{
	final protected byte[] ipPixels;
    
	public FastByteApply(
			final ByteProcessor ip,
			final ByteProcessor src,
			final ByteProcessor dst,
			final ByteProcessor mask,
			final int boxXMin,
			final int boxYMin,
			final int boxXMax,
			final int boxYMax ) throws Exception
	{
		super( ip, src, dst, mask, boxXMin, boxYMin, boxXMax, boxYMax );
		ipPixels = ( byte[] )ip.getPixels();
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
				final float v = srcPixels[ i ] & 0xff;
				final float a = dstPixels[ i ] & 0xff;
				final float b = m * a + ( 1.0f - m ) * v;
				ipPixels[ i ] = ( byte )mpicbg.util.Util.roundPos( b );
				++i;
			}
		}
	}
}
