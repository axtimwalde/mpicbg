package mpicbg.image;

/**
 * Signalizes that a PixelPointer is outside the PixelContainer	
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
public class OutOfBoundsException extends RuntimeException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -4888331272088044246L;

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
