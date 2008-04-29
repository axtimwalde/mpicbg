/**
 * 
 */
package mpicbg.image;

/**
 * @author Preibisch and Saalfeld
 *
 */
public interface DirectIteratable
{
	public void next( int dimension );
	public void previous( int dimension );
	public boolean outOfBounds( int dimension );
}
