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
    
    public static ImagePlus FloatArrayToImagePlus( FloatArray2D image, String name, float min, float max )
	{
		ImagePlus imp = IJ.createImage( name, "32-Bit Black", image.width, image.height, 1 );
		FloatProcessor ip = ( FloatProcessor ) imp.getProcessor();
		FloatArrayToFloatProcessor( ip, image );

		if ( min == max )
			ip.resetMinAndMax();
		else
			ip.setMinAndMax( min, max );

		imp.updateAndDraw();

		return imp;
	}

    
    public static FloatArray2D ImageToFloatArray2D( ImageProcessor ip )
	{
		FloatArray2D image;
		Object pixelArray = ip.getPixels();
		int count = 0;

		if ( ip instanceof ByteProcessor )
		{
			image = new FloatArray2D( ip.getWidth(), ip.getHeight() );
			byte[] pixels = ( byte[] ) pixelArray;

			for ( int y = 0; y < ip.getHeight(); y++ )
				for ( int x = 0; x < ip.getWidth(); x++ )
					image.data[ count ] = pixels[ count++ ] & 0xff;
		}
		else if ( ip instanceof ShortProcessor )
		{
			image = new FloatArray2D( ip.getWidth(), ip.getHeight() );
			short[] pixels = ( short[] ) pixelArray;

			for ( int y = 0; y < ip.getHeight(); y++ )
				for ( int x = 0; x < ip.getWidth(); x++ )
					image.data[ count ] = pixels[ count++ ] & 0xffff;
		}
		else if ( ip instanceof FloatProcessor )
		{
			image = new FloatArray2D( ip.getWidth(), ip.getHeight() );
			float[] pixels = ( float[] ) pixelArray;

			for ( int y = 0; y < ip.getHeight(); y++ )
				for ( int x = 0; x < ip.getWidth(); x++ )
					image.data[ count ] = pixels[ count++ ];
		}
		else // if (ip instanceof ColorProcessor )
		{
			image = new FloatArray2D( ip.getWidth(), ip.getHeight() );
			int[] pixels = ( int[] ) pixelArray;

			for ( int y = 0; y < ip.getHeight(); y++ )
			{
				for ( int x = 0; x < ip.getWidth(); x++ )
				{
					int rgb = pixels[ count++ ];
					int b = rgb & 0xff;
					rgb = rgb >> 8;
					int g = rgb & 0xff;
					rgb = rgb >> 8;
					int r = rgb & 0xff;
					image.data[ count ] = 0.3f * r + 0.6f * g + 0.1f * b;
				}
			}
		}
		return image;
	}

    public static void FloatArrayToFloatProcessor( ImageProcessor ip, FloatArray2D pixels )
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
