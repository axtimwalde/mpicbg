package mpicbg.ij.stack;

import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import mpicbg.models.InverseCoordinateTransform;
import mpicbg.models.NoninvertibleModelException;

/**
 * Use an {@link InverseCoordinateTransform} to map
 * {@linkplain ImageStack source} into {@linkplain ImageProcessor target}
 * which is a projective {@link Mapping}.
 * 
 * Bilinear interpolation is supported.
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 */
public class InverseTransformMapping< T extends InverseCoordinateTransform > implements Mapping< T >
{
	/* Here comes the ugly part as required by ImageJ for interpolation in z */
	abstract static public class Interpolator
	{
		abstract public int interpolate( final int a, final int b, final float da );
		
		final static protected float interpolate( final float a, final float b, final float da )
		{
			return da * ( b - a ) + a;
		}
		
		final static protected double interpolate( final double a, final double b, final float da )
		{
			return da * ( b - a ) + a;
		}
	}
	
	static public class ByteInterpolator extends Interpolator
	{
		@Override
		public int interpolate( final int a, final int b, final float da )
		{
			final float fa = a & 0xff;
			final float fb = b & 0xff;
			return Math.round( interpolate( fa, fb, da ) );
		}
	}
	
	static public class ShortInterpolator extends Interpolator
	{
		@Override
		public int interpolate( final int a, final int b, final float da )
		{
			final float fa = a & 0xffff;
			final float fb = b & 0xffff;
			return Math.round( interpolate( fa, fb, da ) );
		}
	}
	
	static public class FloatInterpolator extends Interpolator
	{
		@Override
		public int interpolate( final int a, final int b, final float da )
		{
			final double fa = Float.intBitsToFloat( a );
			final double fb = Float.intBitsToFloat( b );
			return Float.floatToIntBits( ( float )interpolate( fa, fb, da ) );
		}
	}
	
	static public class RGBInterpolator extends Interpolator
	{
		@Override
		public int interpolate( final int a, final int b, final float da )
		{
			final float fra = ( a >> 16 ) & 0xff ;
			final float frb = ( b >> 16 ) & 0xff ;
			final float fga = ( a >> 8 ) & 0xff ;
			final float fgb = ( b >> 8 ) & 0xff ;
			final float fba = a & 0xff ;
			final float fbb = b & 0xff ;
			final float fr = interpolate( fra, frb, da );
			final float fg = interpolate( fga, fgb, da );
			final float fb = interpolate( fba, fbb, da );
			
			return ( Math.round( fr ) << 16 ) | ( Math.round( fg ) << 8 ) | Math.round( fb );
		}
	}
	
	
	final protected T transform;
	final public T getTransform(){ return transform; }	
	
	public InverseTransformMapping( final T t )
	{
		this.transform = t;
	}
	
	//@Override
	public void map(
			final ImageStack source,
			final ImageProcessor target )
	{
		final float[] t = new float[ 3 ];
		final int sw = source.getWidth() - 1;
		final int sh = source.getHeight() - 1;
		final int sd = source.getSize();
		final int tw = target.getWidth();
		final int th = target.getHeight();
		
		/* ImageJ creates a !NEW! ImageProcessor for each call to getProcessor() */
		final ImageProcessor slice = source.getProcessor( 1 );
		
		for ( int y = 0; y < th; ++y )
		{
			for ( int x = 0; x < tw; ++x )
			{
				t[ 0 ] = x;
				t[ 1 ] = y;
				t[ 2 ] = 0;
				try
				{
					transform.applyInverseInPlace( t );
					final int tx = ( int )( t[ 0 ] + 0.5f );
					final int ty = ( int )( t[ 1 ] + 0.5f );
					final int tz = ( int )( t[ 2 ] + 1.5f );
					if (
							tx >= 0 &&
							tx <= sw &&
							ty >= 0 &&
							ty <= sh &&
							tz >= 1 &&
							tz <= sd )
					{
						slice.setPixels( source.getPixels( tz ) );
						target.putPixel( x, y, slice.getPixel( tx, ty ) );
					}
				}
				catch ( NoninvertibleModelException e ){}
			}
		}
	}
	
	//@Override
	public void mapInterpolated(
			final ImageStack source,
			final ImageProcessor target )
	{
		final float[] t = new float[ 3 ];
		final int sw = source.getWidth() - 1;
		final int sh = source.getHeight() - 1;
		final int sd = source.getSize();
		final int tw = target.getWidth();
		final int th = target.getHeight();
		
		/* ImageJ creates a !NEW! ImageProcessor for each call to getProcessor() */
		final ImageProcessor slice = source.getProcessor( 1 );
		slice.setInterpolationMethod( ImageProcessor.BILINEAR );
		
		final Interpolator interpolator;
		if ( ByteProcessor.class.isInstance( slice ) ) interpolator = new ByteInterpolator();
		else if ( ShortProcessor.class.isInstance( slice ) ) interpolator = new ShortInterpolator();
		else if ( FloatProcessor.class.isInstance( slice ) ) interpolator = new FloatInterpolator();
		else if ( ColorProcessor.class.isInstance( slice ) ) interpolator = new RGBInterpolator();
		else interpolator = null;
		
		for ( int y = 0; y < th; ++y )
		{
			for ( int x = 0; x < tw; ++x )
			{
				t[ 0 ] = x;
				t[ 1 ] = y;
				t[ 2 ] = 0;
				try
				{
					transform.applyInverseInPlace( t );
					final int tza = ( int )( t[ 2 ] + 1.0f );
					final int tzb = ( int )( t[ 2 ] + 2.0f );
					if (
							t[ 0 ] >= 0 &&
							t[ 0 ] <= sw &&
							t[ 1 ] >= 0 &&
							t[ 1 ] <= sh &&
							tza >= 1 &&
							tzb <= sd )
					{
						slice.setPixels( source.getPixels( tza ) );
						final int a = slice.getPixelInterpolated( t[ 0 ], t[ 1 ] );
						slice.setPixels( source.getPixels( tzb ) );
						final int b = slice.getPixelInterpolated( t[ 0 ], t[ 1 ] );
						
						target.putPixel( x, y, interpolator.interpolate( a, b, t[ 2 ] - tza + 1.0f ) );
					}
				}
				catch ( NoninvertibleModelException e ){}
			}
		}
	}
}
