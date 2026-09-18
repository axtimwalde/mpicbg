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
package mpicbg.ij.util;

import ij.plugin.filter.GaussianBlur;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import mpicbg.util.Util;

/**
 *
 * @author Stephan Saalfeld &lt;saalfeld@mpi-cbg.de&gt;
 * @version 0.2b
 */
public class Filter
{
	/**
	 * Normalize data numerically such that the sum of all fields is 1.0
	 *
	 * @param data
	 */
	final static public void normalize( final float[] data )
	{
		float sum = 0;
		for ( final float d : data )
			sum += d;

		for ( int i = 0; i < data.length; ++i )
			data[ i ] /= sum;
	}

	/**
     * Create a non-normalized 1d-Gaussian kernel of appropriate size.
     *
     * @param sigma Standard deviation (&sigma;) of the Gaussian kernel
     *
     * @return Gaussian kernel of appropriate size
     */
    final static public float[] createGaussianKernel( final float sigma )
	{
		final float[] kernel;

		if ( sigma <= 0 )
		{
			kernel = new float[ 1 ];
			kernel[ 0 ] = 1;
		}
		else
		{
			final int size = Math.max( 3, ( int ) ( 2 * ( int )( 3 * sigma + 0.5f ) + 1 ) );

			final float twoSquareSigma = 2 * sigma * sigma;
			kernel = new float[ size ];

			for ( int x = size / 2; x >= 0; --x )
			{
				final float val = ( float ) Math.exp( -( float )( x * x ) / twoSquareSigma );

				kernel[ size / 2 - x ] = val;
				kernel[ size / 2 + x ] = val;
			}
		}

		return kernel;
	}

    /**
     * Create a normalized 1d-Gaussian kernel of appropriate size.
     * Normalization is performed numerically such that the sum of all fields
     * is 1.0.  It turned out to be better to normalize with respect to the sum
     * instead of the integral which would be per-field division by
     * &sigma;/&radic;2&pi;
     *
     * @param sigma Standard deviation (&sigma;) of the Gaussian kernel
     *
     * @return Gaussian kernel of appropriate size
     */
    final static public float[] createNormalizedGaussianKernel( final float sigma )
	{
		final float[] kernel = createGaussianKernel( sigma );
		normalize( kernel );
		return kernel;
	}




    /**
	 * Create a non-normalized 2d-Gaussian impulse with appropriate size whose
	 * center is slightly shifted away from the middle.
	 *
	 * @param sigma Standard deviation (&sigma;) of the Gaussian kernel
     * @param offsetX horizontal center shift [0.0,0.5]
     * @param offsetY vertical center shift [0.0,0.5]
     *
     * @return
     */
    final static public FloatProcessor createShiftedGaussianKernel(
			final float sigma,
			final float offsetX,
			final float offsetY )
	{
		final FloatProcessor kernel;
		if ( sigma <= 0 )
		{
			kernel = new FloatProcessor( 1, 1 );
			kernel.setf( 0, 1 );
		}
		else
		{
			final int size = Math.max( 3, ( int ) ( 2 * Math.round( 3 * sigma ) + 1 ) );
			final float twoSquareSigma = 2 * sigma * sigma;

			kernel = new FloatProcessor( size, size );
			for ( int x = size - 1; x >= 0; --x )
			{
				final float fx = ( float ) ( x - size / 2 );
				for ( int y = size - 1; y >= 0; --y )
				{
					final float fy = ( float ) ( y - size / 2 );
					final float val = ( float ) ( Math.exp( -( Math.pow( fx - offsetX, 2 ) + Math.pow( fy - offsetY, 2 ) ) / twoSquareSigma ) );
					kernel.setf( x, y, val );
				}
			}
		}
		return kernel;
	}

    /**
     * Create a non-normalized 2d-Gaussian impulse with appropriate size whose
	 * center is slightly shifted away from the middle.  It turned out to be
	 * better to normalize with respect to the sum instead of the integral
	 * which would be per-field division by
     * &sigma;<sup>2</sup>/2&pi;)
     *
     * @param sigma Standard deviation (&sigma;) of the Gaussian kernel
     *
     * @return Gaussian kernel of appropriate size
     */
    final static public FloatProcessor createNormalizedShiftedGaussianKernel(
			final float sigma,
			final float offsetX,
			final float offsetY )
	{
    	final FloatProcessor kernel = createShiftedGaussianKernel( sigma, offsetX, offsetY );
    	normalize( ( float[] )kernel.getPixels() );
    	return kernel;
	}

	final public static FloatProcessor[] createGradients( final FloatProcessor array )
	{
		final int width = array.getWidth();
		final int height = array.getHeight();
		final FloatProcessor[] gradients = new FloatProcessor[ 2 ];
		gradients[ 0 ] = new FloatProcessor( width, height );
		gradients[ 1 ] = new FloatProcessor( width, height );

		final float[] data = ( float[] )array.getPixels();
		final float[] rData = ( float[] )gradients[ 0 ].getPixels();
		final float[] phiData = ( float[] )gradients[ 1 ].getPixels();

		for ( int y = 0; y < height; ++y )
		{
			final int[] ro = new int[ 3 ];
			ro[ 0 ] = width * Math.max( 0, y - 1 );
			ro[ 1 ] = width * y;
			ro[ 2 ] = width * Math.min( y + 1, height - 1 );
			for ( int x = 0; x < width; ++x )
			{
				/* (L(x+1, y) - L(x-1, y)) / 2 */
				final float der_x = ( data[ ro[ 1 ] + Math.min( x + 1, width - 1 ) ] - data[ ro[ 1 ] + Math.max( 0, x - 1 ) ] ) / 2;

				/* (L(x, y+1) - L(x, y-1)) / 2 */
				final float der_y = ( data[ ro[ 2 ] + x ] - data[ ro[ 0 ] + x ] ) / 2;

				/* r */
				rData[ ro[ 1 ] + x ] = ( float ) Math.sqrt( Math.pow( der_x, 2 ) + Math.pow( der_y, 2 ) );

				/* phi */
				phiData[ ro[ 1 ] + x ] = ( float ) Math.atan2( der_y, der_x );
			}
		}
		return gradients;
	}

    /**
	 * Create a convolved image with a horizontal and a vertical kernel
	 * simple straightforward, not optimized---replace this with a trusted better version soon
	 *
	 * @param input the input image
	 * @param h horizontal kernel
	 * @param v vertical kernel
	 *
	 * @return convolved image
	 */
	final static public FloatProcessor createConvolveSeparable(
			final FloatProcessor input,
			final float[] h,
			final float[] v )
	{
		final FloatProcessor output = ( FloatProcessor )input.duplicate();
		convolveSeparable( output, h, v );
		return output;
	}


	/**
	 * Convolve an image with a horizontal and a vertical kernel
	 * simple straightforward, not optimized---replace this with a trusted better version soon
	 *
	 * @param input the input image
	 * @param h horizontal kernel
	 * @param v vertical kernel
	 */
	final static public void convolveSeparable(
			final FloatProcessor input,
			final float[] h,
			final float[] v )
	{
		final int width = input.getWidth();
		final int height = input.getHeight();

		final FloatProcessor temp = new FloatProcessor( width, height );

		final float[] inputData = ( float[] )input.getPixels();
		final float[] tempData = ( float[] )temp.getPixels();

		final int hl = h.length / 2;
		final int vl = v.length / 2;

		int xl = width - h.length + 1;
		int yl = height - v.length + 1;

		// create lookup tables for coordinates outside the image range
		final int[] xb = new int[ h.length + hl - 1 ];
		final int[] xa = new int[ h.length + hl - 1 ];
		for ( int i = 0; i < xb.length; ++i )
		{
			xb[ i ] = Util.pingPong( i - hl, width );
			xa[ i ] = Util.pingPong( i + xl, width );
		}

		final int[] yb = new int[ v.length + vl - 1 ];
		final int[] ya = new int[ v.length + vl - 1 ];
		for ( int i = 0; i < yb.length; ++i )
		{
			yb[ i ] = width * Util.pingPong( i - vl, height );
			ya[ i ] = width * Util.pingPong( i + yl, height );
		}

		xl += hl;
		yl += vl;

		// horizontal convolution per row
		final int rl = height * width;
		for ( int r = 0; r < rl; r += width )
		{
			for ( int x = hl; x < xl; ++x )
			{
				final int c = x - hl;
				float val = 0;
				for ( int xk = 0; xk < h.length; ++xk )
				{
					val += h[ xk ] * inputData[ r + c + xk ];
				}
				tempData[ r + x ] = val;
			}
			for ( int x = 0; x < hl; ++x )
			{
				float valb = 0;
				float vala = 0;
				for ( int xk = 0; xk < h.length; ++xk )
				{
					valb += h[ xk ] * inputData[ r + xb[ x + xk ] ];
					vala += h[ xk ] * inputData[ r + xa[ x + xk ] ];
				}
				tempData[ r + x ] = valb;
				tempData[ r + x + xl ] = vala;
			}
		}

		// vertical convolution per column
		final int rm = yl * width;
		final int vlc = vl * width;
		for ( int x = 0; x < width; ++x )
		{
			for ( int r = vlc; r < rm; r += width )
			{
				float val = 0;
				final int c = r - vlc;
				int rk = 0;
				for ( int yk = 0; yk < v.length; ++yk )
				{
					val += v[ yk ] * tempData[ c + rk + x ];
					rk += width;
				}
				inputData[ r + x ] = val;
			}
			for ( int y = 0; y < vl; ++y )
			{
				final int r = y * width;
				float valb = 0;
				float vala = 0;
				for ( int yk = 0; yk < v.length; ++yk )
				{
					valb += h[ yk ] * tempData[ yb[ y + yk ] + x ];
					vala += h[ yk ] * tempData[ ya[ y + yk ] + x ];
				}
				inputData[ r + x ] = valb;
				inputData[ r + rm + x ] = vala;
			}
		}
	}


	/**
	 * Smooth with a Gaussian kernel that represents downsampling at a given
	 * scale factor and sourceSigma.
	 */
	final static public void smoothForScale(
			final FloatProcessor source,
			final double scale,
			final float sourceSigma,
			final float targetSigma )
	{
		assert scale <= 1.0f : "Downsampling requires a scale factor < 1.0";

		final double s = targetSigma / scale;
		final double v = s * s - sourceSigma * sourceSigma;
		if ( v <= 0 )
			return;
		final float sigma = ( float )Math.sqrt( v );
//		final float[] kernel = createNormalizedGaussianKernel( sigma );
//		convolveSeparable( source, kernel, kernel );
		new GaussianBlur().blurFloat( source, sigma, sigma, 0.01 );
	}


	/**
	 * Create a downsampled {@link FloatProcessor}.
	 *
	 * @param source the source image
	 * @param scale scaling factor
	 * @param sourceSigma the Gaussian at which the source was sampled (guess 0.5 if you do not know)
	 * @param targetSigma the Gaussian at which the target will be sampled
	 *
	 * @return a new {@link FloatProcessor}
	 */
	final static public FloatProcessor createDownsampled(
			final FloatProcessor source,
			final double scale,
			final float sourceSigma,
			final float targetSigma )
	{
		assert scale <= 1.0f : "Downsampling requires a scale factor < 1.0";

		final int ow = source.getWidth();
		final int oh = source.getHeight();
		final int w = ( int )Math.round( ow * scale );
		final int h = ( int )Math.round( oh * scale );

		// same sigma as smoothForScale
		final double s = targetSigma / scale;
		final double v = s * s - sourceSigma * sourceSigma;
		final float sigma = v > 0 ? (float)Math.sqrt(v) : 0;

		if (sigma > 2 * 4 + 0.5) {
			// ImageJ blurs large sigmas by downscaling, convolving and upscaling; that path is not replicated here
			final FloatProcessor temp = (FloatProcessor)source.duplicate();
			temp.setMinAndMax(source.getMin(), source.getMax());
			smoothForScale(temp, scale, sourceSigma, targetSigma);
			if (scale == 1.0f) return temp;
			return sampleRows((float[])temp.getPixels(), ow, oh, scale, w, h, null, null, source);
		}

		final float[] src = (float[])source.getPixels();
		if (sigma == 0) return sampleRows(src, ow, oh, scale, w, h, null, null, source);

		// ImageJ's GaussianBlur.blurFloat: convolve rows, then columns, with ImageJ's kernel (accuracy 0.01) and edge rule
		final GaussianBlur gb = new GaussianBlur();
		return sampleRows(src, ow, oh, scale, w, h, gb.makeGaussianKernel(sigma, 0.01, ow), gb.makeGaussianKernel(sigma, 0.01, oh), source);
	}

	/**
	 * Nearest-neighbor sample {@code src} into a new {@code w}x{@code h} processor. If the kernels are
	 * not null, the image is first blurred like ImageJ's {@code GaussianBlur.blurFloat} (rows, then
	 * columns): the horizontally blurred rows are kept in a ring that stays in cache instead of a
	 * full-size temporary image, and only the rows that are sampled are convolved vertically (rows that
	 * are never sampled do not change any sampled value).
	 */
	private static FloatProcessor sampleRows(
			final float[] src, final int ow, final int oh, final double scale, final int w, final int h,
			final float[][] kernelX, final float[][] kernelY, final FloatProcessor source
	) {
		final FloatProcessor target = new FloatProcessor( w, h );
		target.setMinAndMax( source.getMin(), source.getMax() );
		final float[] dst = (float[])target.getPixels();

		/* LUT for scaled pixel locations */
		final int ow1 = ow - 1;
		final int oh1 = oh - 1;
		final int[] lutx = new int[ w ];
		for ( int x = 0; x < w; ++x )
			lutx[ x ] = Math.min( ow1, Math.max( 0, ( int )Math.round( x / scale ) ) );
		final int[] luty = new int[ h ];
		for ( int y = 0; y < h; ++y )
			luty[ y ] = Math.min( oh1, Math.max( 0, ( int )Math.round( y / scale ) ) );

		final float[] row = new float[ow];
		final int r = kernelY == null ? 1 : kernelY[0].length;
		final float[][] ring = new float[2 * r - 1][kernelY == null ? 0 : ow]; // rows y - r + 1 to y + r - 1 contribute to row y
		final float[] rowA = new float[ow], rowB = new float[ow], acc = new float[ow];
		final float[] first = new float[ow], last = new float[ow]; // ImageJ's edge rule adds the first and last row of a column
		if (kernelY != null) {
			blurRow(src, 0, ow, kernelX, first, acc, rowA, rowB);
			blurRow(src, oh1 * ow, ow, kernelX, last, acc, rowA, rowB);
		}
		int next = 0; // the next row to be blurred horizontally into the ring
		for ( int y = 0; y < h; ++y )
		{
			final int yy = luty[y];
			if (y == 0 || yy != luty[y - 1]) {
				if (kernelY == null)
					System.arraycopy(src, yy * ow, row, 0, ow);
				else {
					for (final int lastRow = Math.min(oh1, yy + r - 1); next <= lastRow; ++next)
						blurRow(src, next * ow, ow, kernelX, ring[next % ring.length], acc, rowA, rowB);
					blurColumnsAt(ring, first, last, oh, kernelY, yy, row);
				}
			}
			final int p = y * w;
			for ( int x = 0; x < w; ++x )
				dst[p + x] = row[lutx[x]];
		}
		return target;
	}

	/**
	 * Convolve the row of {@code in} starting at {@code p} into {@code out} with the arithmetic of
	 * ImageJ's {@code GaussianBlur.convolveLine}: {@code in[i]*kern[0] + sum_k kern[k]*(in[i-k] + in[i+k])},
	 * taps in ascending order, out-of-line pixels replaced by the edge pixel via the running kernel sum.
	 * The interior is evaluated as one axpy per tap over contiguous scratch rows (C2 vectorizes it, see
	 * {@link mpicbg.imagefeatures.Filter#convolveSeparable}), the edges pixel by pixel.
	 */
	private static void blurRow(final float[] in, final int p, final int w, final float[][] kernel, final float[] out, final float[] acc, final float[] rowA, final float[] rowB) {
		final float[] kern = kernel[0];
		final int r = kern.length;
		final float kern0 = kern[0];
		final int firstPart = Math.min(r, w);
		final int n = w - 2 * r; // interior pixels [ r, w - r )
		if (n > 0) {
			System.arraycopy(in, p + r, rowA, 0, n);
			for (int x = 0; x < n; ++x)
				acc[x] = rowA[x] * kern0;
			for (int k = 1; k < r; ++k) {
				final float kk = kern[k];
				System.arraycopy(in, p + r - k, rowA, 0, n);
				System.arraycopy(in, p + r + k, rowB, 0, n);
				for (int x = 0; x < n; ++x)
					acc[x] += kk * (rowA[x] + rowB[x]);
			}
			System.arraycopy(acc, 0, out, r, n);
		}
		for (int i = 0; i < firstPart; ++i)
			out[i] = convolveEdgePixel(in, p, 1, i, w, kernel, true);
		for (int i = Math.max(firstPart, w - r); i < w; ++i)
			out[i] = convolveEdgePixel(in, p, 1, i, w, kernel, false);
	}

	/**
	 * Vertically convolve row {@code y} of the image whose rows {@code y - r + 1} to {@code y + r - 1} are
	 * in {@code ring} (row {@code i} at {@code i % ring.length}) and whose first and last rows are
	 * {@code first} and {@code last}, into {@code row}. Same arithmetic and order as {@link #blurRow}
	 * and {@link #convolveEdgePixel}, but as row operations over the ring rows (which C2 vectorizes).
	 */
	private static void blurColumnsAt(final float[][] ring, final float[] first, final float[] last, final int h, final float[][] kernel, final int y, final float[] row) {
		final float[] kern = kernel[0], kernSum = kernel[1];
		final int r = kern.length;
		final int w = row.length;
		final float kern0 = kern[0];
		final float[] c = ring[y % ring.length];
		for (int x = 0; x < w; ++x)
			row[x] = c[x] * kern0;
		final boolean edge = y < r || y >= h - r;
		if (edge) {
			final boolean firstPart = y < Math.min(r, h);
			if (firstPart || y < r) scaleAdd(row, kernSum[y], first);
			if (firstPart ? y + r > h : y + r >= h) scaleAdd(row, kernSum[h - y - 1], last);
		}
		for (int k = 1; k < r; ++k) {
			final float kk = kern[k];
			final boolean hasA = y - k >= 0, hasB = y + k < h;
			if (hasA && hasB) {
				final float[] a = ring[(y - k) % ring.length], b = ring[(y + k) % ring.length];
				for (int x = 0; x < w; ++x)
					row[x] += kk * (a[x] + b[x]);
			} else if (hasA) scaleAdd(row, kk, ring[(y - k) % ring.length]);
			else if (hasB) scaleAdd(row, kk, ring[(y + k) % ring.length]);
			else
				for (int x = 0; x < w; ++x)
					row[x] += kk * 0f; // ImageJ adds kern[ k ] * 0 here, which turns -0 into +0
		}
	}

	/** {@code row += k * a} */
	private static void scaleAdd(final float[] row, final float k, final float[] a) {
		for (int x = 0; x < row.length; ++x)
			row[x] += k * a[x];
	}

	/**
	 * Pixel {@code i} of a line (start {@code p0}, stride {@code inc}, {@code length} pixels) whose kernel
	 * support crosses a line end; literal port of the first ({@code firstPart}) and last loop of ImageJ's
	 * {@code GaussianBlur.convolveLine}.
	 */
	private static float convolveEdgePixel(
			final float[] in, final int p0, final int inc, final int i, final int length, final float[][] kernel, final boolean firstPart
	) {
		final float[] kern = kernel[0], kernSum = kernel[1];
		final int r = kern.length;
		final float first = in[p0], last = in[p0 + (length - 1) * inc];
		float result = in[p0 + i * inc] * kern[0];
		if (firstPart || i < r) result += kernSum[i] * first;
		if (firstPart ? i + r > length : i + r >= length) result += kernSum[length - i - 1] * last;
		for (int k = 1; k < r; ++k) {
			float v = 0;
			if (i - k >= 0) v += in[p0 + (i - k) * inc];
			if (i + k < length) v += in[p0 + (i + k) * inc];
			result += kern[k] * v;
		}
		return result;
	}

	/**
	 * Smooth with a Gaussian kernel that represents downsampling at a given
	 * scale factor and sourceSigma.
	 */
	final static public void smoothForScale(
		final ImageProcessor source,
		final double scale,
		final float sourceSigma,
		final float targetSigma )
	{
		final double s = targetSigma / scale;
		final double v = s * s - sourceSigma * sourceSigma;
		if ( v <= 0 )
			return;
		final double sigma = Math.sqrt( v );
		new GaussianBlur().blurGaussian( source, sigma, sigma, 0.01 );
	}


	/**
	 * Create a downsampled ImageProcessor.
	 *
	 * @param source the source image
	 * @param scale scaling factor
	 * @param sourceSigma the Gaussian at which the source was sampled (guess 0.5 if you do not know)
	 * @param targetSigma the Gaussian at which the target will be sampled
	 *
	 * @return a new {@link FloatProcessor}
	 */
	final static public ImageProcessor createDownsampled(
			final ImageProcessor source,
			final double scale,
			final float sourceSigma,
			final float targetSigma )
	{
		final int ow = source.getWidth();
		final int oh = source.getHeight();
		final int w = ( int )Math.round( ow * scale );
		final int h = ( int )Math.round( oh * scale );

		final ImageProcessor temp = source.duplicate();
		temp.setMinAndMax( source.getMin(), source.getMax() );

		smoothForScale( temp, scale, sourceSigma, targetSigma );
		if ( scale >= 1.0f ) return temp;

		final ImageProcessor target = temp.resize( w, h );
		target.setMinAndMax( source.getMin(), source.getMax() );
		return target;
	}

	/**
	 * Scale an image with good quality in both up and down direction
	 */
	final static public ImageProcessor scale(
			final ImageProcessor source,
			final float scale )
	{
		final ImageProcessor target;
		if ( scale == 1.0f ) target = source.duplicate();
		else if ( scale < 1.0f ) target = createDownsampled( source, scale, 0.5f, 0.5f );
		else
		{
			source.setInterpolationMethod( ImageProcessor.BILINEAR );
			target = source.resize( Math.round( scale * source.getWidth() ) );
		}
		target.setMinAndMax( source.getMin(), source.getMax() );
		return target;
	}
}
