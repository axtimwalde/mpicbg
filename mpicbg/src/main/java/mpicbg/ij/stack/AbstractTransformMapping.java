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
package mpicbg.ij.stack;

import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/**
 * Abstract base class {@link Mapping} from an {@linkplain ImageStack source}
 * into an {@linkplain ImageProcessor target}.
 *
 * Bilinear interpolation is supported.
 *
 */
abstract public class AbstractTransformMapping< T > implements Mapping< T >
{
	protected float z = 0;

	/* Here comes the ugly part as required by ImageJ for interpolation in z */
	abstract static public class Interpolator
	{
		abstract public int interpolate( final int a, final int b, final double da );

		final static protected float interpolate( final float a, final float b, final float da )
		{
			return da * ( b - a ) + a;
		}

		final static protected double interpolate( final double a, final double b, final double da )
		{
			return da * ( b - a ) + a;
		}
	}

	static public class ByteInterpolator extends Interpolator
	{
		@Override
		public int interpolate( final int a, final int b, final double da )
		{
			final double fa = a & 0xff;
			final double fb = b & 0xff;
			return ( int )Math.round( interpolate( fa, fb, da ) );
		}
	}

	static public class ShortInterpolator extends Interpolator
	{
		@Override
		public int interpolate( final int a, final int b, final double da )
		{
			final double fa = a & 0xffff;
			final double fb = b & 0xffff;
			return ( int )Math.round( interpolate( fa, fb, da ) );
		}
	}

	static public class FloatInterpolator extends Interpolator
	{
		@Override
		public int interpolate( final int a, final int b, final double da )
		{
			final double fa = Float.intBitsToFloat( a );
			final double fb = Float.intBitsToFloat( b );
			return Float.floatToIntBits( ( float )interpolate( fa, fb, da ) );
		}
	}

	static public class RGBInterpolator extends Interpolator
	{
		@Override
		public int interpolate( final int a, final int b, final double da )
		{
			final double fra = ( a >> 16 ) & 0xff ;
			final double frb = ( b >> 16 ) & 0xff ;
			final double fga = ( a >> 8 ) & 0xff ;
			final double fgb = ( b >> 8 ) & 0xff ;
			final double fba = a & 0xff ;
			final double fbb = b & 0xff ;
			final double fr = interpolate( fra, frb, da );
			final double fg = interpolate( fga, fgb, da );
			final double fb = interpolate( fba, fbb, da );

			return ( ( int )Math.round( fr ) << 16 ) | ( int )( Math.round( fg ) << 8 ) | ( int )Math.round( fb );
		}
	}


	final protected T transform;
	@Override
    final public T getTransform(){ return transform; }

	public AbstractTransformMapping( final T t )
	{
		this.transform = t;
	}

	static protected Interpolator pickInterpolator( final ImageProcessor slice )
	{
	    final Interpolator interpolator;
        if ( ByteProcessor.class.isInstance( slice ) ) interpolator = new ByteInterpolator();
        else if ( ShortProcessor.class.isInstance( slice ) ) interpolator = new ShortInterpolator();
        else if ( FloatProcessor.class.isInstance( slice ) ) interpolator = new FloatInterpolator();
        else if ( ColorProcessor.class.isInstance( slice ) ) interpolator = new RGBInterpolator();
        else interpolator = null;

        return interpolator;
	}

	//@Override
	@Override
    public void setSlice( final float z ){ this.z = z; }
}
