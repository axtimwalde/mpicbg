package mpicbg.ij;

import ij.process.ImageProcessor;
import mpicbg.models.InverseCoordinateTransform;
import mpicbg.models.NoninvertibleModelException;

/**
 * Use an {@link InverseCoordinateTransform} to map
 * {@linkplain ImageProcessor source} into {@linkplain ImageProcessor target}
 * which is a {@link Mapping}.
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 */
public class InverseTransformMapping< T extends InverseCoordinateTransform > implements Mapping< T >
{
	final protected T transform;
	final public T getTransform(){ return transform; }	
	
	public InverseTransformMapping( final T t )
	{
		this.transform = t;
	}
	
	//@Override
	public void map(
			final ImageProcessor source,
			final ImageProcessor target )
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
				try
				{
					transform.applyInverseInPlace( t );
					if (
							t[ 0 ] >= 0 &&
							t[ 0 ] < w &&
							t[ 1 ] >= 0 &&
							t[ 1 ] < h )
						target.putPixel( x, y, source.getPixel( ( int )t[ 0 ], ( int )t[ 1 ] ) );
				}
				catch ( NoninvertibleModelException e ){}
			}
		}
	}
	
	//@Override
	public void mapInterpolated(
			final ImageProcessor source,
			final ImageProcessor target )
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
				try
				{
					transform.applyInverseInPlace( t );
					if (
							t[ 0 ] >= 0 &&
							t[ 0 ] < w &&
							t[ 1 ] >= 0 &&
							t[ 1 ] < h )
						target.putPixel( x, y, source.getPixelInterpolated( t[ 0 ], t[ 1 ] ) );
				}
				catch ( NoninvertibleModelException e ){}
			}
		}
	}
}
