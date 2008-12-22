package mpicbg.imagefeatures;

/**
 * 
 * @author Stephan Preibisch
 * @version 1.0
 */

import ij.IJ;
import ij.ImagePlus;
import ij.process.*;

public class ImageArrayConverter
{
    public static boolean CUTOFF_VALUES = true;
    public static boolean NORM_VALUES = false;
    
    final public static ImagePlus FloatArrayToImagePlus(
    		final FloatArray2D image,
    		final String name,
    		final float min,
    		final float max )
	{
		final ImagePlus imp = IJ.createImage( name, "32-Bit Black", image.width, image.height, 1 );
		final FloatProcessor ip = ( FloatProcessor )imp.getProcessor();
		floatArray2DToFloatProcessor( image, ip );

		if ( min == max )
			ip.resetMinAndMax();
		else
			ip.setMinAndMax( min, max );

		imp.updateAndDraw();

		return imp;
	}

    
    final public static void imageProcessorToFloatArray2D( final ImageProcessor ip, final FloatArray2D fa )
	{
		if ( ip instanceof ByteProcessor )
		{
			final byte[] pixels = ( byte[] )ip.getPixels();
			for ( int i = 0; i < pixels.length; ++i )
				fa.data[ i ] = pixels[ i ] & 0xff;
		}
		else if ( ip instanceof ShortProcessor )
		{
			final short[] pixels = ( short[] )ip.getPixels();
			for ( int i = 0; i < pixels.length; ++i )
				fa.data[ i ] = pixels[ i ] & 0xffff;
		}
		else if ( ip instanceof FloatProcessor )
		{
			final float[] pixels = ( float[] )ip.getPixels();
			for ( int i = 0; i < pixels.length; ++i )
				fa.data[ i ] = pixels[ i ];
		}
		else if ( ip instanceof ColorProcessor )
		{
			final int[] pixels = ( int[] )ip.getPixels();
			for ( int i = 0; i < pixels.length; ++i )
			{
				final int rgb = pixels[ i ];
				final int b = rgb & 0xff;
				final int g = ( rgb >> 8 ) & 0xff;
				final int r = ( rgb >> 16 ) & 0xff;
				fa.data[ i ] = 0.3f * r + 0.6f * g + 0.1f * b;
			}
		}
	}

    public static void floatArray2DToFloatProcessor( final FloatArray2D pixels, final FloatProcessor ip )
	{
		float[] data = new float[ pixels.width * pixels.height ];

		int count = 0;
		for ( int y = 0; y < pixels.height; y++ )
			for ( int x = 0; x < pixels.width; x++ )
				data[ count ] = pixels.data[ count++ ];

		ip.setPixels( data );
		ip.resetMinAndMax();
	}
}
