/**
 * 
 */
package mpicbg.image;

import mpicbg.image.interpolation.Interpolator;

/**
 * @author Preibisch and Saalfeld
 *
 */
public abstract class DirectIterator extends DirectPixelPointer implements DirectIteratable
{
	DirectIterator( Container pc, Interpolator ip )
	{
		super( pc, ip );
	}
}
