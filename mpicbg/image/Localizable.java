package mpicbg.image;

/**
 * @author Preibisch and Saalfeld
 *
 */
public interface Localizable
{
	/**
	 * Get the cursor's location.
	 */
	public float[] localize();
	
	/**
	 * Write the cursor's location into a given array
	 * @param location coordinates of the location
	 */
	public void localize( int[] l );
	public void localize( float[] l );
}
