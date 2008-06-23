package mpicbg.image;

/**
 * Provides a reasonable set of basic operations on top of a Pixel.
 * The pixel itself is identified by a Cursor.
 * 
 * @author Saalfeld and Preibisch
 *
 */
public interface Operator
{
	public void add( Readable c );
	public void sub( Readable c );
	public void mul( Readable c );
	public void div( Readable c );
	
	public void add( byte c );
	public void add( short c );
	public void add( int c );
	public void add( long c );
	public void add( float c );
	public void add( double c );
	
	public void sub( byte c );
	public void sub( short c );
	public void sub( int c );
	public void sub( long c );
	public void sub( float c );
	public void sub( double c );
	
	public void mul( byte c );
	public void mul( short c );
	public void mul( int c );
	public void mul( long c );
	public void mul( float c );
	public void mul( double c );
	
	public void div( byte c );
	public void div( short c );
	public void div( int c );
	public void div( long c );
	public void div( float c );
	public void div( double c );
}
