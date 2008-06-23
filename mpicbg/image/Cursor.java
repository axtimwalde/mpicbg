/**
 * 
 */
package mpicbg.image;

import mpicbg.image.interpolation.Interpolator;

/**
 * @author Saalfeld
 *
 */
abstract public class Cursor
{
	final Container container;
	final Interpolator interpolator;
	
	public Cursor( Container c, Interpolator ip )
	{
		container = c;
		interpolator = ip;
	}
}
