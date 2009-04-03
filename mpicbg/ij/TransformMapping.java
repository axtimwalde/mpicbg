/**
 * 
 */
package mpicbg.ij;

import mpicbg.models.CoordinateTransform;
import ij.process.ImageProcessor;

/**
 * @author saalfeld
 *
 */
public class TransformMapping implements InverseMapping
{
	final protected CoordinateTransform transform;
	
	public TransformMapping( final CoordinateTransform t )
	{
		this.transform = t;
	}
	
	//@Override
	public void mapInverse( ImageProcessor source, ImageProcessor target )
	{
		final float[] t = new float[ 2 ];
		for ( int y = 0; y < target.getHeight(); ++y )
		{
			for ( int x = 0; x < target.getWidth(); ++x )
			{
				t[ 0 ] = x;
				t[ 1 ] = y;
				transform.applyInPlace( t );
				target.putPixel( x, y, source.getPixel( ( int )t[ 0 ], ( int )t[ 1 ] ) );
			}
		}
	}

	//@Override
	public void mapInverseInterpolated( ImageProcessor source, ImageProcessor target )
	{
		final float[] t = new float[ 2 ];
		for ( int y = 0; y < target.getHeight(); ++y )
		{
			for ( int x = 0; x < target.getWidth(); ++x )
			{
				t[ 0 ] = x;
				t[ 1 ] = y;
				transform.applyInPlace( t );
				target.putPixel( x, y, source.getPixelInterpolated( t[ 0 ], t[ 1 ] ) );
			}
		}

	}

}
