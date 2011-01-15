package mpicbg.models;

import java.lang.Exception;

/**
 * Signalizes that a {@link AbstractModel} is not invertible.
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 */
public class NoninvertibleModelException extends Exception
{
	private static final long serialVersionUID = 8790171367047404173L;


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
