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
		final int w = source.getWidth() - 1;
		final int h = source.getHeight() - 1;
		for ( int y = 0; y < target.getHeight(); ++y )
		{
			for ( int x = 0; x < target.getWidth(); ++x )
			{
				t[ 0 ] = x;
				t[ 1 ] = y;
				transform.applyInPlace( t );
				if (
						t[ 0 ] >= 0 &&
						t[ 0 ] < w &&
						t[ 1 ] >= 0 &&
						t[ 1 ] < h )
					target.putPixel( x, y, source.getPixel( ( int )t[ 0 ], ( int )t[ 1 ] ) );
			}
		}
	}

	//@Override
	public void mapInverseInterpolated( ImageProcessor source, ImageProcessor target )
	{
		final float[] t = new float[ 2 ];
		final int w = source.getWidth() - 1;
		final int h = source.getHeight() - 1;
		for ( int y = 0; y < target.getHeight(); ++y )
		{
			for ( int x = 0; x < target.getWidth(); ++x )
			{
				t[ 0 ] = x;
				t[ 1 ] = y;
				transform.applyInPlace( t );
				if (
						t[ 0 ] >= 0 &&
						t[ 0 ] < w &&
						t[ 1 ] >= 0 &&
						t[ 1 ] < h )
					target.putPixel( x, y, source.getPixelInterpolated( t[ 0 ], t[ 1 ] ) );
			}
		}
	}
}
