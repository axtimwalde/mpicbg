package mpicbg.image;

/**
 * @author Preibisch and Saalfeld
 */
public interface IteratableByDimension
{
	public void next( int dimension );
	public void prev( int dimension );
	public boolean isInside( int dimension );
}
