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
	public void next() throws OutOfBoundsException;
	public void prev() throws OutOfBoundsException;
	public boolean hasNext();
	public boolean hasPrev();
}
