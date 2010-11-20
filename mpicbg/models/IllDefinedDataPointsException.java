package mpicbg.models;

import java.lang.Exception;

/**
 * Signalizes that there were not enough data points available to estimate the
 * {@link AbstractModel}.
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 */
public class IllDefinedDataPointsException extends Exception
{
	private static final long serialVersionUID = -8384893194524443449L;

	public IllDefinedDataPointsException()
	{
		super( "The set of data points is ill defined.  No Model could be solved." );
	}
	
	
	public IllDefinedDataPointsException( String message )
	{
		super( message );
	}
	
	
	public IllDefinedDataPointsException( Throwable cause )
	{
		super( cause );
	}
	
	
	public IllDefinedDataPointsException( String message, Throwable cause )
	{
		super( message, cause );
	}
}
