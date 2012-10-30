package mpicbg.ij.util;

import ij.plugin.filter.GaussianBlur;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import mpicbg.util.Util;

/**
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
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
	 * 
	 * @return convolved image
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
			final float scale,
			final float sourceSigma,
			final float targetSigma )
	{
		assert scale <= 1.0f : "Downsampling requires a scale factor < 1.0";
		
		final float s = targetSigma / scale;
		final float v = s * s - sourceSigma * sourceSigma;
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
			final float scale,
			final float sourceSigma,
			final float targetSigma )
	{
		assert scale <= 1.0f : "Downsampling requires a scale factor < 1.0";
		
		final int ow = source.getWidth();
		final int oh = source.getHeight();
		final int w = Math.round( ow * scale );
		final int h = Math.round( oh * scale );
		final int l = Math.max( w, h );
		
		final FloatProcessor temp = ( FloatProcessor )source.duplicate();
		temp.setMinAndMax( source.getMin(), source.getMax() );
		
		smoothForScale( temp, scale, sourceSigma, targetSigma );
		if ( scale == 1.0f ) return temp;
		
		final float[] tempPixels = ( float[] )temp.getPixels();
		
		final FloatProcessor target = new FloatProcessor( w, h );
		target.setMinAndMax( source.getMin(), source.getMax() );
		final float[] targetPixels = ( float[] )target.getPixels();
		
		/* LUT for scaled pixel locations */
		final int ow1 = ow - 1;
		final int oh1 = oh - 1;
		final int[] lutx = new int[ l ];
		for ( int x = 0; x < w; ++x )
			lutx[ x ] = Math.min( ow1, Math.max( 0, Math.round( x / scale ) ) );
		final int[] luty = new int[ l ];
		for ( int y = 0; y < h; ++y )
			luty[ y ] = Math.min( oh1, Math.max( 0, Math.round( y / scale ) ) );
		
		for ( int y = 0; y < h; ++y )
		{
			final int p = y * w;
			final int q = luty[ y ] * ow;
			for ( int x = 0; x < w; ++x )
				targetPixels[ p + x ] = tempPixels[ q + lutx[ x ] ];
		}
		
		return target;
	}
	
	/**
	 * Smooth with a Gaussian kernel that represents downsampling at a given
	 * scale factor and sourceSigma.
	 */
	final static public void smoothForScale(
		final ImageProcessor source,
		final float scale,
		final float sourceSigma,
		final float targetSigma )
	{
		final float s = targetSigma / scale;
		final float v = s * s - sourceSigma * sourceSigma;
		if ( v <= 0 )
			return;
		final float sigma = ( float )Math.sqrt( v );
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
			final float scale,
			final float sourceSigma,
			final float targetSigma )
	{
		final int ow = source.getWidth();
		final int oh = source.getHeight();
		final int w = Math.round( ow * scale );
		final int h = Math.round( oh * scale );
		
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
