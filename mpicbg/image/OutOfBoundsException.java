package mpicbg.image;

/**
 * Signalizes that a PixelPointer is outside the PixelContainer	
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
public class OutOfBoundsException extends RuntimeException
{
	public OutOfBoundsException()
	{
		super( "Cursor is outside the PixelContainer." );
	}
	
	public OutOfBoundsException( String message )
	{
		super( message );
	}
	
	public OutOfBoundsException( Throwable cause )
	{
		super( cause );
	}
	
	public OutOfBoundsException( String message, Throwable cause )
	{
		super( message, cause );
	}
}
