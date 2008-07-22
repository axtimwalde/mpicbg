/**
 * 
 */
package mpicbg.image;

/**
 * @author Preibisch and Saalfeld
 *
 */
public interface Iterator< I extends Container< ? extends PixelType, ? extends Cursor > >
{
	public void next();
	public void prev();
}
