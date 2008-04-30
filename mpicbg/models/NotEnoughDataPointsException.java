package mpicbg.models;

import java.lang.Exception;

/**
 * Signalizes that there were not enough data points available to estimate the
 * model.
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
public class NotEnoughDataPointsException extends Exception
{
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
