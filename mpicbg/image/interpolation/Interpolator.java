package mpicbg.image.interpolation;

import mpicbg.image.PixelPointer;

/**
 * @author Preibisch and Saalfeld
 *
 */

public interface Interpolator
{
	Object get( PixelPointer p );
}
