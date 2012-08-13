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

import ij.process.FloatProcessor;

/**
 * Normalize contrast based on per-pixel mean and STD.
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
public class NormalizeLocalContrast extends BlockStatistics
{
	final protected FloatProcessor fpOriginal;
	
	public NormalizeLocalContrast( final FloatProcessor fp )
	{
		super( fp );
		fpOriginal = ( FloatProcessor )fp.duplicate();
		fpOriginal.setMinAndMax( fp.getMin(), fp.getMax() );
	}
	
	/**
	 * 
	 * @param ip
	 * @param v
	 */
	protected void runCenter(
			final int blockRadiusX,
			final int blockRadiusY )
	{
		fp.setPixels( fpOriginal.getPixelsCopy() );
		
		final int width = fp.getWidth();
		final int height = fp.getHeight();
		
		final double fpMin = fp.getMin();
		final double fpLength = fp.getMax() - fpMin;
		final double fpMean = fpLength / 2.0 + fpMin;
		
		final int w = width - 1;
		final int h = height - 1;
		for ( int y = 0; y < height; ++y )
		{
			final int row = y * width;
			final int yMin = Math.max( -1, y - blockRadiusY - 1 );
			final int yMax = Math.min( h, y + blockRadiusY );
			final int bh = yMax - yMin;
			for ( int x = 0; x < width; ++x )
			{
				final int xMin = Math.max( -1, x - blockRadiusX - 1 );
				final int xMax = Math.min( w, x + blockRadiusX );
				final long bs = ( xMax - xMin ) * bh;
				final double scale = 1.0 / bs;
				final double sum = sums.getDoubleSum( xMin, yMin, xMax, yMax );
				final int i = row + x;
				
				final double mean = sum * scale;
				final float v = fp.getf( i );
				
				fp.setf(  i, ( float )( ( v - mean ) + fpMean ) );
			}
		}
	}
	
	/**
	 * 
	 * @param ip
	 * @param v
	 */
	protected void runStretch(
			final int blockRadiusX,
			final int blockRadiusY,
			final float meanFactor )
	{
		fp.setPixels( fpOriginal.getPixelsCopy() );
		
		final int width = fp.getWidth();
		final int height = fp.getHeight();
		
		final double fpMin = fp.getMin();
		final double fpLength = fp.getMax() - fpMin;
		final double fpMean = fpLength / 2.0 + fpMin;
		
		final int w = width - 1;
		final int h = height - 1;
		for ( int y = 0; y < height; ++y )
		{
			final int row = y * width;
			final int yMin = Math.max( -1, y - blockRadiusY - 1 );
			final int yMax = Math.min( h, y + blockRadiusY );
			final int bh = yMax - yMin;
			for ( int x = 0; x < width; ++x )
			{
				final int xMin = Math.max( -1, x - blockRadiusX - 1 );
				final int xMax = Math.min( w, x + blockRadiusX );
				final long bs = ( xMax - xMin ) * bh;
				final double scale1 = 1.0 / ( bs - 1 );
				final double scale2 = 1.0 / ( bs * bs - bs );
				final double sum = sums.getDoubleSum( xMin, yMin, xMax, yMax );
				final double var = scale1 * sumsOfSquares.getDoubleSum( xMin, yMin, xMax, yMax ) - scale2 * sum * sum;
				final int i = row + x;
				
				final double std = var < 0 ? 0 : Math.sqrt( var );
				final float v = fp.getf( i );
				final double d = meanFactor * std;
				
				fp.setf(  i, ( float )( ( v - fpMean ) / 2 / d * fpLength + fpMean ) );
			}
		}
	}
	
	
	/**
	 * 
	 * @param ip
	 * @param v
	 */
	protected void runCenterStretch(
			final int blockRadiusX,
			final int blockRadiusY,
			final float meanFactor )
	{
		fp.setPixels( fpOriginal.getPixelsCopy() );
		
		final int width = fp.getWidth();
		final int height = fp.getHeight();
		
		final double fpMin = fp.getMin();
		final double fpLength = fp.getMax() - fpMin;
		
		final int w = width - 1;
		final int h = height - 1;
		for ( int y = 0; y < height; ++y )
		{
			final int row = y * width;
			final int yMin = Math.max( -1, y - blockRadiusY - 1 );
			final int yMax = Math.min( h, y + blockRadiusY );
			final int bh = yMax - yMin;
			for ( int x = 0; x < width; ++x )
			{
				final int xMin = Math.max( -1, x - blockRadiusX - 1 );
				final int xMax = Math.min( w, x + blockRadiusX );
				final long bs = ( xMax - xMin ) * bh;
				final double scale = 1.0 / bs;
				final double scale1 = 1.0 / ( bs - 1 );
				final double scale2 = 1.0 / ( bs * bs - bs );
				final double sum = sums.getDoubleSum( xMin, yMin, xMax, yMax );
				final double var = scale1 * sumsOfSquares.getDoubleSum( xMin, yMin, xMax, yMax ) - scale2 * sum * sum;
				final int i = row + x;
				
				final double mean = sum * scale;
				final double std = var < 0 ? 0 : Math.sqrt( var );
				final float v = fp.getf( i );
				final double d = meanFactor * std;
				final double min = mean - d;
				
				fp.setf(  i, ( float )( ( v - min ) / 2 / d * fpLength + fpMin ) );
			}
		}
	}
	
	
	/**
	 * 
	 * @param ip
	 * @param v
	 */
	public void run(
			final int blockRadiusX,
			final int blockRadiusY,
			final float meanFactor,
			final boolean center,
			final boolean stretch )
	{
		if ( center )
		{
			if ( stretch )
				runCenterStretch( blockRadiusX, blockRadiusY, meanFactor );
			else
				runCenter( blockRadiusX, blockRadiusY );
		}
		else
		{
			if ( stretch )
				runStretch( blockRadiusX, blockRadiusY, meanFactor );
			else
				fp.setPixels( fpOriginal.getPixelsCopy() );
		}
	}
}
