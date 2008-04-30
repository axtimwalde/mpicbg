package mpicbg.image;

import java.lang.Exception;

/**
 * Signalizes that a PixelPointer is outside the PixelContainer	
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
public class OutOfBoundsException extends Exception
{
	public OutOfBoundsException()
	{
		super( "Iterator is outside the PixelContainer." );
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
