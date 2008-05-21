package mpicbg.image;

/**
 * Provides a reasonable set of basic operations on top of a Pixel.
 * The pixel itself is identified by a Cursor.
 * 
 * @author Saalfeld
 *
 */
public interface Operator
{
	abstract public void add( Cursor a );
	abstract public void sub( Cursor a );
	abstract public void mul( Cursor a );
	abstract public void div( Cursor a );
}
