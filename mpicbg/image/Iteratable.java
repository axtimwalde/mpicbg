/**
 * 
 */
package mpicbg.image;

/**
 * @author Preibisch and Saalfeld
 *
 */
public interface Iteratable
{
	public void next() throws OutOfBoundsException;
	public void prev() throws OutOfBoundsException;
	public boolean hasNext();
	public boolean hasPrev();
}
