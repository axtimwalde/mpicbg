/**
 * 
 */
package mpicbg.image;

/**
 * @author Preibisch and Saalfeld
 *
 */
public interface Interpolator
{
	Object get( double[] location );
	Object get( PixelPointer p );
}
