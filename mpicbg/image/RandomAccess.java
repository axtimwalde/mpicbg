/**
 * 
 */
package mpicbg.image;

/**
 * @author Preibisch and Saalfeld
 *
 */
public interface RandomAccess
{
	/**
	 * Go to a random location.
	 * 
	 * @param l coordinates of the location
	 */
	public void to( int[] l );
	
	/**
	 * Go to the floor of a random location.
	 * 
	 * @param l coordinates of the location
	 */
	public void to( float[] l );
}
