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
package mpicbg.ij.clahe;

import ij.process.ByteProcessor;

/**
 * Apply a piece of CLAHE to a {@link ByteProcessor}.
 *
 * @author Stephan Saalfeld &lt;saalfeld@mpi-cbg.de&gt;
 * @version 0.3b
 */
final class ByteApply extends Apply< ByteProcessor >
{
	final protected byte[] ipPixels;
    
	public ByteApply(
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
				final float v = ipPixels[ i ] & 0xff;
				final float vSrc = srcPixels[ i ] & 0xff;
				final float a;
				if ( vSrc == 0 )
					a = 1.0f;
				else
					a = ( float )( dstPixels[ i ] & 0xff ) / vSrc;
				final float b = v * ( 1.0f + m * ( a - 1.0f ) );
				ipPixels[ i ] = ( byte )Math.max( 0, Math.min( 255, mpicbg.util.Util.roundPos( b ) ) );
				++i;
			}
		}
	}
}
