package mpicbg.ij;

import ij.process.ImageProcessor;
import mpicbg.models.InvertibleCoordinateTransform;
import mpicbg.models.NoninvertibleModelException;

/**
 * Use an {@link InvertibleCoordinateTransform} to map
 * {@linkplain ImageProcessor source} into {@linkplain ImageProcessor target}
 * which is a {@link Mapping}.
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 */
public class InvertibleTransformMapping< T extends InvertibleCoordinateTransform > implements InvertibleMapping< T >
{
	final protected T transform;
	@Override
	final public T getTransform(){ return transform; }

	public InvertibleTransformMapping( final T t )
	{
		this.transform = t;
	}

	//@Override
	@Override
	public void map( final ImageProcessor source, final ImageProcessor target )
	{
		final double[] t = new double[ 2 ];
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
				try
				{
					transform.applyInverseInPlace( t );
					final int tx = ( int )( t[ 0 ] + 0.5f );
					final int ty = ( int )( t[ 1 ] + 0.5f );
					if (
							t[ 0 ] >= 0 &&
							t[ 0 ] <= sw &&
							t[ 1 ] >= 0 &&
							t[ 1 ] <= sh )
						target.putPixel( x, y, source.getPixel( tx, ty ) );
				}
				catch ( final NoninvertibleModelException e ){}
			}
		}
	}

	//@Override
	@Override
	public void mapInterpolated( final ImageProcessor source, final ImageProcessor target )
	{
		final double[] t = new double[ 2 ];
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
				try
				{
					transform.applyInverseInPlace( t );
					if (
							t[ 0 ] >= 0 &&
							t[ 0 ] <= sw &&
							t[ 1 ] >= 0 &&
							t[ 1 ] <= sh )
						target.putPixel( x, y, source.getPixelInterpolated( t[ 0 ], t[ 1 ] ) );
				}
				catch ( final NoninvertibleModelException e ){}
			}
		}
	}

	//@Override
	@Override
	public void mapInverse( final ImageProcessor source, final ImageProcessor target )
	{
		final double[] t = new double[ 2 ];
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
	@Override
	public void mapInverseInterpolated( final ImageProcessor source, final ImageProcessor target )
	{
		final double[] t = new double[ 2 ];
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
						t[ 0 ] < sw &&
						t[ 1 ] >= 0 &&
						t[ 1 ] < sh )
					target.putPixel( x, y, source.getPixelInterpolated( t[ 0 ], t[ 1 ] ) );
			}
		}
	}
}
