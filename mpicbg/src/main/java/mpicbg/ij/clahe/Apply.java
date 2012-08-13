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

import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

/**
 * Apply a piece of CLAHE to the different kinds of {@link ImageProcessor ImageProcessors}.
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.3b
 */
abstract class Apply< T extends ImageProcessor >
{
	final protected T ip;
	final protected int width;
	final protected int height;
	
	final protected byte[] srcPixels;
	final protected byte[] dstPixels;
	final protected byte[] maskPixels;
	
	final protected int boxXMin, boxYMin, boxXMax, boxYMax;
	
	Apply(
			final T ip,
			final ByteProcessor src,
			final ByteProcessor dst,
			final ByteProcessor mask,
			final int boxXMin,
			final int boxYMin,
			final int boxXMax,
			final int boxYMax ) throws Exception
	{
		this.ip = ip;
		width = ip.getWidth();
		height = ip.getHeight();
		
		this.boxXMin = boxXMin;
		this.boxYMin = boxYMin;
		this.boxXMax = boxXMax;
		this.boxYMax = boxYMax;
		
		if ( !(
				src.getWidth() == width && src.getHeight() == height &&
				dst.getWidth() == width && dst.getHeight() == height ) )
			throw new Exception( "Image sizes do not match." );
		
		srcPixels = ( byte[] )src.getPixels();
		dstPixels = ( byte[] )dst.getPixels();
		if ( mask == null )
		{
			maskPixels = new byte[ srcPixels.length ];
			mpicbg.util.Util.memset( maskPixels, ( byte )255 );
		}
		else
		{
			if (
					boxXMin == 0 &&
					boxYMin == 0 &&
					boxXMax == mask.getWidth() &&
					boxYMax == mask.getHeight() )
				maskPixels = ( byte[] )mask.getPixels();
			else
			{
				final ByteProcessor extendedMask = new ByteProcessor( width, height );
				extendedMask.copyBits( mask, boxXMin, boxYMin, Blitter.COPY );
				maskPixels = ( byte[] )extendedMask.getPixels();
			}
		}
	}
	
	abstract public void apply( final int xMin, final int yMin, final int xMax, final int yMax );
}
