/**
 * 
 */
package mpicbg.image;

/**
 * @author Preibisch and Saalfeld
 *
 */
public interface RandomAccessible
{
	/**
	 * Go to a random location.
	 * 
	 * @param location coordinates of the location
	 */
	public void to( int[] location );
}
