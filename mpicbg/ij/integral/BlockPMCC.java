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
 * Pearson's Product-Moment Correlation Coefficient (PMCC) for overlapping
 * blocks of pixels in two single channel images accelerated by
 * {@link IntegralImage integral images}.
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
final public class BlockPMCC
{
	final private DoubleIntegralImage sumsX;
	final private DoubleIntegralImage sumsY;
	final private DoubleIntegralImage sumsXX;
	final private DoubleIntegralImage sumsYY;
	final private DoubleIntegralImage sumsXY;
	
	final private FloatProcessor fpX;
	final private FloatProcessor fpY;
	final private FloatProcessor fpR;
	
	private int fpXYWidth, fpXYHeight, offsetXX, offsetYX, offsetXY, offsetYY;
	
	final static private void sumAndSumOfSquares(
			final FloatProcessor fp,
			final double[] sum,
			final double[] sumOfSquares )
	{
		final int width = fp.getWidth();
		final int height = fp.getHeight();
		
		final int w = width + 1;
		final int w1 = w + 1;
		final int w2 = w + w;
		
		final int n = w * height + w;
		final int n1 = n - w1;
		final int n2 = n1 - w + 2;
		
		/* rows */
		for ( int i = 0, j = w1; j < n; ++j )
		{
			final int end = i + width;
			double s = sum[ j ] = fp.getf( i );
			double ss = sumOfSquares[ j ] = s * s;
			for ( ++i, ++j; i < end; ++i, ++j )
			{
				final float a = fp.getf( i );
				s += a;
				ss += a * a;
				sum[ j ] = s;
				sumOfSquares[ j ] = ss;
			}
		}
		
		/* columns */
		for ( int j = w1; j < w2; j -= n1 )
		{
			final int end = j + n2;
			
			double s = sum[ j ];
			double ss = sumOfSquares[ j ];
			for ( j += w; j < end; j += w )
			{
				s += sum[ j ];
				ss += sumOfSquares[ j ];
				
				sum[ j ] = s;
				sumOfSquares[ j ] = ss;
			}
		}
	}
	
	final static private void sumAndSumOfSquares(
			final int width,
			final int height,
			final FloatProcessor fp1,
			final double[] sum1,
			final double[] sumOfSquares1,
			final FloatProcessor fp2,
			final double[] sum2,
			final double[] sumOfSquares2 )
	{
		final int w = width + 1;
		final int w1 = w + 1;
		final int w2 = w + w;
		
		final int n = w * height + w;
		final int n1 = n - w1;
		final int n2 = n1 - w + 2;
		
		/* rows */
		for ( int i = 0, j = w1; j < n; ++j )
		{
			final int end = i + width;
			double s1 = sum1[ j ] = fp1.getf( i );
			double ss1 = sumOfSquares1[ j ] = s1 * s1;
			double s2 = sum2[ j ] = fp2.getf( i );
			double ss2 = sumOfSquares2[ j ] = s2 * s2;
			for ( ++i, ++j; i < end; ++i, ++j )
			{
				final float a1 = fp1.getf( i );
				s1 += a1;
				ss1 += a1 * a1;
				sum1[ j ] = s1;
				sumOfSquares1[ j ] = ss1;
				
				final float a2 = fp2.getf( i );
				s2 += a2;
				ss2 += a2 * a2;
				sum2[ j ] = s2;
				sumOfSquares2[ j ] = ss2;
			}
		}
		
		/* columns */
		for ( int j = w1; j < w2; j -= n1 )
		{
			final int end = j + n2;
			
			double s1 = sum1[ j ];
			double ss1 = sumOfSquares1[ j ];
			double s2 = sum2[ j ];
			double ss2 = sumOfSquares2[ j ];
			for ( j += w; j < end; j += w )
			{
				s1 += sum1[ j ];
				ss1 += sumOfSquares1[ j ];
				s2 += sum2[ j ];
				ss2 += sumOfSquares2[ j ];
				
				sum1[ j ] = s1;
				sumOfSquares1[ j ] = ss1;
				sum2[ j ] = s2;
				sumOfSquares2[ j ] = ss2;
			}
		}
	}
	
	
	/**
	 * Special case constructor for fpX and fpY having equal dimensions.
	 * 
	 * <p>Note, that this constructor does not initialize &Sigma;XY.  That has
	 * to be done for a specified offset through {@link #setOffset(int, int)}
	 * afterwards.</p>
	 * 
	 * @param width
	 * @param height
	 * @param fpX
	 * @param fpY
	 */
	public BlockPMCC(
			final int width,
			final int height,
			final FloatProcessor fpX,
			final FloatProcessor fpY )
	{
		this.fpX = fpX;
		this.fpY = fpY;
		
		fpR = new FloatProcessor( width, height );
		
		final double[] sumX = new double[ ( width + 1 ) * ( height + 1 ) ];
		final double[] sumXX = new double[ sumX.length ];
		final double[] sumY = new double[ ( width + 1 ) * ( height + 1 ) ];
		final double[] sumYY = new double[ sumY.length ];
		final double[] sumXY = new double[ ( width + 1 ) * ( height + 1 ) ];
		
		sumAndSumOfSquares( width, height, fpX, sumX, sumXX, fpY, sumY, sumYY );
		
		sumsX = new DoubleIntegralImage( sumX, width, height );
		sumsXX = new DoubleIntegralImage( sumXX, width, height );
		sumsY = new DoubleIntegralImage( sumY, width, height );
		sumsYY = new DoubleIntegralImage( sumYY, width, height );
		sumsXY = new DoubleIntegralImage( sumXY, width, height );
	}
	
	
	/**
	 * Special case constructor for fpX and fpY having equal dimensions.
	 * 
	 * @param width
	 * @param height
	 * @param fpX source (moving)
	 * @param fpY target
	 * @param offsetX source <em>x</em> offset relative to target
	 * @param offsetY source <em>y</em> offset relative to target
	 */
	public BlockPMCC(
			final int width,
			final int height,
			final FloatProcessor fpX,
			final FloatProcessor fpY,
			final int offsetX,
			final int offsetY )
	{
		this( width, height, fpX, fpY );
		
		setOffset( offsetX, offsetY );
	}
	
	
	/**
	 * Two loops because fpX and fpY can have different dimensions.
	 * 
	 * <p>Note, that this constructor does not initialize &Sigma;XY.  That has
	 * to be done for a specified offset through {@link #setOffset(int, int)}
	 * afterwards.</p>
	 * 
	 * @param fpX
	 * @param fpY
	 */
	public BlockPMCC( final FloatProcessor fpX, final FloatProcessor fpY )
	{
		this.fpX = fpX;
		this.fpY = fpY;
		
		final int widthX = fpX.getWidth();
		final int heightX = fpX.getHeight();
		final int widthY = fpY.getWidth();
		final int heightY = fpY.getHeight();
		final int widthXY = widthX < widthY ? widthX : widthY;
		final int heightXY = heightX < heightY ? heightX : heightY;
		
		fpR = new FloatProcessor( widthY, heightY );
		
		final double[] sumX = new double[ ( widthX + 1 ) * ( heightX + 1 ) ];
		final double[] sumXX = new double[ sumX.length ];
		final double[] sumY = new double[ ( widthY + 1 ) * ( heightY + 1 ) ];
		final double[] sumYY = new double[ sumY.length ];
		final double[] sumXY = new double[ ( widthXY + 1 ) * ( heightXY + 1 ) ];
		
		sumAndSumOfSquares( fpX, sumX, sumXX );
		sumAndSumOfSquares( fpY, sumY, sumYY );
		
		sumsX = new DoubleIntegralImage( sumX, widthX, heightX );
		sumsXX = new DoubleIntegralImage( sumXX, widthX, heightX );
		sumsY = new DoubleIntegralImage( sumY, widthY, heightY );
		sumsYY = new DoubleIntegralImage( sumYY, widthY, heightY );
		sumsXY = new DoubleIntegralImage( sumXY, widthXY, heightXY );
	}
	
	
	/**
	 * Two loops because fpX and fpY can have different dimensions.
	 * 
	 * @param fpX source (moving)
	 * @param fpY target
	 * @param offsetX source <em>x</em> offset relative to target
	 * @param offsetY source <em>y</em> offset relative to target
	 */
	public BlockPMCC(
			final FloatProcessor fpX,
			final FloatProcessor fpY,
			final int offsetX,
			final int offsetY )
	{
		this( fpX, fpY );
		
		setOffset( offsetX, offsetY );
	}
	
	
	final public FloatProcessor getTargetProcessor()
	{
		return fpR;
	}
	
	
	/**
	 * Set the offset and re-calculate sumsXY respectively.
	 * 
	 * @param fp
	 * @param sum
	 * @param sumOfSquares
	 */
	final public void setOffset( final int offsetX, final int offsetY )
	{
		/* TODO simple and stupid first, fast later if it works! */
		
		final int fpXWidth = fpX.getWidth();
		final int fpYWidth = fpY.getWidth();
		
		int a, b;
		if ( offsetX < 0 )
		{
			offsetXX = -offsetX;
			offsetXY = 0;
			a = fpX.getWidth() + offsetX;
			b = fpY.getWidth();
		}
		else
		{
			offsetXX = 0;
			offsetXY = offsetX;
			a = fpX.getWidth();
			b = fpY.getWidth() - offsetX;
		}
		fpXYWidth = ( a < b ? a : b );
		
		if ( offsetY < 0 )
		{
			offsetYX = -offsetY;
			offsetYY = 0;
			a = fpX.getHeight() + offsetY;
			b = fpY.getHeight();
		}
		else
		{
			offsetYX = 0;
			offsetYY = offsetY;
			a = fpX.getHeight();
			b = fpY.getHeight() - offsetY;
		}
		fpXYHeight = ( a < b ? a : b );
		
		
		/* first row */
		
		final int w = fpR.getWidth() + 1;
		final int w1 = w + 1;
		
		final double[] sum = sumsXY.getData();
		
		double s = 0;
		for ( int x = 0; x < fpXYWidth; )
		{
			final float vX = fpX.getf( offsetYX * fpXWidth + x + offsetXX );
			final float vY = fpY.getf( offsetYY * fpYWidth + x + offsetXY );
			
			s += vX * vY;
//			s += vX + vY;
			sum[ ++x + w ] = s;
		}
		for ( int y = 1; y < fpXYHeight; ++y )
		{
			final int rowX = ( y + offsetYX ) * fpXWidth;
			final int rowY = ( y + offsetYY ) * fpYWidth;
			
			float vX = fpX.getf( rowX + offsetXX );
			float vY = fpY.getf( rowY + offsetXY );
			
			final int yw = y * w + w1;
			sum[ yw ] = sum[ yw - w ] + vX * vY;
//			sum[ yw ] = sum[ yw - w ] + vX + vY;
			for ( int x = 1; x < fpXYWidth; ++x )
			{
				final int ywx = yw + x;
				
				vX = fpX.getf( rowX + x + offsetXX );
				vY = fpY.getf( rowY + x + offsetXY );
				
				sum[ ywx ] = sum[ ywx - w ] + sum[ ywx - 1 ] + vX * vY - sum[ ywx - w - 1 ];
//				sum[ ywx ] = sum[ ywx - w ] + sum[ ywx - 1 ] + vX + vY - sum[ ywx - w - 1 ];
			}
		}
//		
//		final FloatProcessor fp = sumsXY.toProcessor();
//		fp.setMinAndMax( 0, 255 * 255 );
//		
//		new ImagePlus( "", fp ).show();
	}
	
	
	
	
	/**
	 * Set all pixels in <code>ip</code> to their block PMCC
	 * <em>r<sub>XY</sub></em> for the current offset with given radius.
	 * 
	 * @param blockRadiusX
	 * @param blockRadiusY
	 */
	final public void r( final int blockRadiusX, final int blockRadiusY )
	{
		final int width = fpR.getWidth();
		final int w = fpXYWidth - 1;
		final int h = fpXYHeight - 1;
		
		for ( int y = 0; y <= h; ++y )
		{
			final int row = y * width;
			
			final int yMin = Math.max( -1, y - blockRadiusY - 1 );
			final int yMax = Math.min( h, y + blockRadiusY );
			final int yMinX = yMin + offsetYX;
			final int yMaxX = yMax + offsetYX;
			final int yMinY = yMin + offsetYY;
			final int yMaxY = yMax + offsetYY;
			
			final int bh = yMax - yMin;
			
			for ( int x = 0; x <= w; ++x )
			{
				final int xMin = Math.max( -1, x - blockRadiusX - 1 );
				final int xMax = Math.min( w, x + blockRadiusX );
				final int xMinX = xMin + offsetXX;
				final int xMaxX = xMax + offsetXX;
				final int xMinY = xMin + offsetXY;
				final int xMaxY = xMax + offsetXY;
				
				final int n = ( xMax - xMin ) * bh;
				
				final double sumX = sumsX.getDoubleSum( xMinX, yMinX, xMaxX, yMaxX );
				final double sumXX = sumsXX.getDoubleSum( xMinX, yMinX, xMaxX, yMaxX );
				final double sumY = sumsY.getDoubleSum( xMinY, yMinY, xMaxY, yMaxY );
				final double sumYY = sumsYY.getDoubleSum( xMinY, yMinY, xMaxY, yMaxY );
				final double sumXY = sumsXY.getDoubleSum( xMin, yMin, xMax, yMax );
				
				final double a = n * sumXY - sumX * sumY;
				final double b = Math.sqrt( n * sumXX - sumX * sumX ) * Math.sqrt( n * sumYY - sumY * sumY );
				
				fpR.setf( row + x, ( float )( a / b ) );
			}
		}
	}

	
	final public void r( final int blockRadius )
	{
		r( blockRadius, blockRadius );
	}
	
	
	/**
	 * Set all pixels in <code>ip</code> to their signed square block PMCC
	 * <em>r<sub>XY</sub><sup>2</sup></em> for the current offset with given
	 * radius.
	 * 
	 * @param blockRadiusX
	 * @param blockRadiusY
	 */
	final public void rSignedSquare( final int blockRadiusX, final int blockRadiusY )
	{
		final int width = fpR.getWidth();
		final int w = fpXYWidth - 1;
		final int h = fpXYHeight - 1;
		
		for ( int y = 0; y <= h; ++y )
		{
			final int row = y * width;
			
			final int yMin = Math.max( -1, y - blockRadiusY - 1 );
			final int yMax = Math.min( h, y + blockRadiusY );
			final int yMinX = yMin + offsetYX;
			final int yMaxX = yMax + offsetYX;
			final int yMinY = yMin + offsetYY;
			final int yMaxY = yMax + offsetYY;
			
			final int bh = yMax - yMin;
			
			for ( int x = 0; x <= w; ++x )
			{
				final int xMin = Math.max( -1, x - blockRadiusX - 1 );
				final int xMax = Math.min( w, x + blockRadiusX );
				final int xMinX = xMin + offsetXX;
				final int xMaxX = xMax + offsetXX;
				final int xMinY = xMin + offsetXY;
				final int xMaxY = xMax + offsetXY;
				
				final int n = ( xMax - xMin ) * bh;
				
				final double sumX = sumsX.getDoubleSum( xMinX, yMinX, xMaxX, yMaxX );
				final double sumXX = sumsXX.getDoubleSum( xMinX, yMinX, xMaxX, yMaxX );
				final double sumY = sumsY.getDoubleSum( xMinY, yMinY, xMaxY, yMaxY );
				final double sumYY = sumsYY.getDoubleSum( xMinY, yMinY, xMaxY, yMaxY );
				final double sumXY = sumsXY.getDoubleSum( xMin, yMin, xMax, yMax );
				
				final double a = n * sumXY - sumX * sumY;
				final double b = ( n * sumXX - sumX * sumX ) * ( n * sumYY - sumY * sumY );
				
				if ( a < 0 )
					fpR.setf( row + x, ( float )( -a * a / b ) );
				else
					fpR.setf( row + x, ( float )( a * a / b ) );
			}
		}
	}
	
	final public void rSignedSquare( final int blockRadius )
	{
		rSignedSquare( blockRadius, blockRadius );
	}
}
