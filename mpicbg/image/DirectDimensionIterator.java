/**
 * 
 */
package mpicbg.image;

import mpicbg.image.interpolation.Interpolator;

/**
 * @author Preibisch and Saalfeld
 *
 */
public abstract class DirectDimensionIterator extends DirectPixelPointer implements IteratableByDimension
{
	DirectDimensionIterator( Container pc, Interpolator ip )
	{
		super( pc, ip );
	}
}
