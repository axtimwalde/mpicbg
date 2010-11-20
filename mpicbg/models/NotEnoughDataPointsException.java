package mpicbg.models;

import java.lang.Exception;

/**
 * Signalizes that there were not enough data points available to estimate the
 * {@link AbstractModel}.
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 */
public class NotEnoughDataPointsException extends Exception
{
	private static final long serialVersionUID = 492656623783477968L;

	public NotEnoughDataPointsException()
	{
		super( "Not enough data points to solve the Model." );
	}
	
	
	public NotEnoughDataPointsException( String message )
	{
		super( message );
	}
	
	
	public NotEnoughDataPointsException( Throwable cause )
	{
		super( cause );
	}
	
	
	public NotEnoughDataPointsException( String message, Throwable cause )
	{
		super( message, cause );
	}
}
