package mpicbg.image;

/**
 * @author Preibisch and Saalfeld
 */
public interface IteratorByDimension< I extends Container< ? extends PixelType, ? extends Cursor > >
{
	public void next( int dimension );
	public void prev( int dimension );
	public boolean isInside( int dimension );
}
