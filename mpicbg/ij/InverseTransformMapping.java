package mpicbg.ij;

import ij.process.ImageProcessor;
import mpicbg.models.InverseCoordinateTransform;
import mpicbg.models.NoninvertibleModelException;

/**
 * 
 * 
 *
 */
public class InverseTransformMapping implements Mapping
{
	final protected InverseCoordinateTransform transform;
	
	public InverseTransformMapping( final InverseCoordinateTransform t )
	{
		this.transform = t;
	}
	
	//@Override
	final public void map(
			final ImageProcessor source,
			final ImageProcessor target )
	{
		final float[] t = new float[ 2 ];
		for ( int y = 0; y < target.getHeight(); ++y )
		{
			for ( int x = 0; x < target.getWidth(); ++x )
			{
				t[ 0 ] = x;
				t[ 1 ] = y;
				try
				{
					transform.applyInverseInPlace( t );
					target.putPixel( x, y, source.getPixel( ( int )t[ 0 ], ( int )t[ 1 ] ) );
				}
				catch ( NoninvertibleModelException e ){ e.printStackTrace(); }
			}
		}
	}
	
	//@Override
	final public void mapInterpolated(
			final ImageProcessor source,
			final ImageProcessor target )
	{
		final float[] t = new float[ 2 ];
		for ( int y = 0; y < target.getHeight(); ++y )
		{
			for ( int x = 0; x < target.getWidth(); ++x )
			{
				t[ 0 ] = x;
				t[ 1 ] = y;
				try
				{
					transform.applyInverseInPlace( t );
					target.putPixel( x, y, source.getPixelInterpolated( t[ 0 ], t[ 1 ] ) );
				}
				catch ( NoninvertibleModelException e ){ e.printStackTrace(); }
			}
		}
	}
}
