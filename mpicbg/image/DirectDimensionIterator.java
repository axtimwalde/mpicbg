/**
 * 
 */
package mpicbg.image;

import mpicbg.image.interpolation.Interpolator;

/**
 * @author Preibisch and Saalfeld
 *
 */
public abstract class DirectDimensionIterator extends DirectPixelPointer implements DirectDimensionIteratable
{
	DirectDimensionIterator( Container pc, Interpolator ip )
	{
		super( pc, ip );
	}
}
