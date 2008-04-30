package mpicbg.models;

import java.lang.Exception;

/**
 * Signalizes that the Model is not invertible.
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
public class NoninvertibleModelException extends Exception
{
	public NoninvertibleModelException()
	{
		super( "Non invertible Model." );
	}
	

	public NoninvertibleModelException( String message )
	{
		super( message );
	}

	
	public NoninvertibleModelException( Throwable cause )
	{
		super( cause );
	}

	
	public NoninvertibleModelException( String message, Throwable cause )
	{
		super( message, cause );
	}
}
