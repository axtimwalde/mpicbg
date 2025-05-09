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
package mpicbg.imagefeatures;

import ij.IJ;
import ij.ImagePlus;
import ij.process.*;

/**
 * 
 * @author Stephan Preibisch and Stephan Saalfeld &lt;saalfeld@mpi-cbg.de&gt;
 * @version 1.4b
 */
final public class ImageArrayConverter
{
    final static public ImagePlus FloatArrayToImagePlus(
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

    
    final static public void imageProcessorToFloatArray2D( final ImageProcessor ip, final FloatArray2D fa )
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
    
    
    /**
     * Convert an arbitrary {@link ImageProcessor} into a
     * {@link FloatArray2D} mapping {@link ImageProcessor#getMin() min} and
     * {@link ImageProcessor#getMax() max} into [0.0,1.0] cropping larger and
     * smaller values.
     * 
     * @param ip
     * @param fa
     */
    final static public void imageProcessorToFloatArray2DCropAndNormalize( final ImageProcessor ip, final FloatArray2D fa )
	{
    	final float min = ( float )ip.getMin();
		final float scale = 1.0f / ( ( float )ip.getMax() - min );
    	if ( ip instanceof ByteProcessor )
		{
			final byte[] pixels = ( byte[] )ip.getPixels();
			for ( int i = 0; i < pixels.length; ++i )
				fa.data[ i ] = ( ( pixels[ i ] & 0xff ) - min ) * scale;
		}
		else if ( ip instanceof ShortProcessor )
		{
			final short[] pixels = ( short[] )ip.getPixels();
			for ( int i = 0; i < pixels.length; ++i )
				fa.data[ i ] = ( ( pixels[ i ] & 0xffff ) - min ) * scale;
		}
		else if ( ip instanceof FloatProcessor )
		{
			final float[] pixels = ( float[] )ip.getPixels();
			for ( int i = 0; i < pixels.length; ++i )
				fa.data[ i ] = ( pixels[ i ] - min ) * scale;
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
				fa.data[ i ] = ( 0.3f * r + 0.6f * g + 0.1f * b - min ) * scale;
			}
		}
	}

    final static public void floatArray2DToFloatProcessor( final FloatArray2D pixels, final FloatProcessor ip )
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
