package mpicbg.image;

/**
 * @author Preibisch and Saalfeld
 */
public interface IteratableByDimension
{
	public void next( int dimension ) throws OutOfBoundsException;
	public void prev( int dimension ) throws OutOfBoundsException;
	public boolean hasNext( int dimension );
	public boolean hasPrev( int dimension );
}
