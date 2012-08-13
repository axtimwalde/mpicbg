package mpicbg.ij;

import mpicbg.models.CoordinateTransform;
import ij.process.ImageProcessor;

/**
 * Use a {@link CoordinateTransform} to map {@linkplain ImageProcessor source}
 * into {@linkplain ImageProcessor target} which is an {@link InverseMapping}.
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 */
public class TransformMapping< T extends CoordinateTransform > implements InverseMapping< T >
{
	final protected T transform;
	final public T getTransform(){ return transform; }	
	
	public TransformMapping( final T t )
	{
		this.transform = t;
	}
	
	//@Override
	public void mapInverse( ImageProcessor source, ImageProcessor target )
	{
		final float[] t = new float[ 2 ];
		final int sw = source.getWidth() - 1;
		final int sh = source.getHeight() - 1;
		final int tw = target.getWidth();
		final int th = target.getHeight();
		for ( int y = 0; y < th; ++y )
		{
			for ( int x = 0; x < tw; ++x )
			{
				t[ 0 ] = x;
				t[ 1 ] = y;
				transform.applyInPlace( t );
				final int tx = ( int )( t[ 0 ] + 0.5f );
				final int ty = ( int )( t[ 1 ] + 0.5f );
				if (
						t[ 0 ] >= 0 &&
						t[ 0 ] <= sw &&
						t[ 1 ] >= 0 &&
						t[ 1 ] <= sh )
					target.putPixel( x, y, source.getPixel( tx, ty ) );
			}
		}
	}

	//@Override
	public void mapInverseInterpolated( ImageProcessor source, ImageProcessor target )
	{
		final float[] t = new float[ 2 ];
		final int sw = source.getWidth() - 1;
		final int sh = source.getHeight() - 1;
		final int tw = target.getWidth();
		final int th = target.getHeight();
		for ( int y = 0; y < th; ++y )
		{
			for ( int x = 0; x < tw; ++x )
			{
				t[ 0 ] = x;
				t[ 1 ] = y;
				transform.applyInPlace( t );
				if (
						t[ 0 ] >= 0 &&
						t[ 0 ] <= sw &&
						t[ 1 ] >= 0 &&
						t[ 1 ] <= sh )
					target.putPixel( x, y, source.getPixelInterpolated( t[ 0 ], t[ 1 ] ) );
			}
		}
	}
}
