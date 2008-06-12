package mpicbg.models;

import java.util.ArrayList;
import java.util.Collections;
import java.lang.IndexOutOfBoundsException;
import java.util.ListIterator;

public class ErrorStatistic
{
	final public ArrayList< Double > values = new ArrayList< Double >();
	final public ArrayList< Double > slope = new ArrayList< Double >();
	
	public double var0 = 0;		// variance relative to 0
	public double var = 0;		// variance relative to mean
	public double std0 = 0;		// standard-deviation relative to 0
	public double std = 0;		// standard-deviation
	public double mean = 0;
	public double median = 0;
	public double min = Double.MIN_VALUE;
	public double max = 0;
	
	final public void add( double new_value )
	{
		if (values.size() > 1 ) slope.add( new_value - values.get( values.size() - 1 ) );
		else slope.add( 0.0 );
		mean = ( mean * values.size() + new_value );
		values.add( new_value );
		mean /= values.size();
		var0 += new_value * new_value / ( double )( values.size() );
		std0 = Math.sqrt( var0 );
		double tmp = new_value - mean;
		var += tmp * tmp / ( double )( values.size() );
		std = Math.sqrt( var );
		Collections.sort( values );
		if ( values.size() % 2 == 0 )
		{
			int m = values.size() / 2;
			median = ( values.get( m - 1 ) + values.get( m ) ) / 2.0;
		}
		else
			median = values.get( values.size() / 2 );
		if ( new_value < min ) min = new_value;
		if ( new_value > max ) max = new_value;
	}
	
	final public double getWideSlope( int width ) throws IndexOutOfBoundsException
	{
		if ( width > slope.size() ) throw new IndexOutOfBoundsException( "Cannot estimate a wide slope for width larger than than the number of sample." );
		ListIterator< Double > li = slope.listIterator( slope.size() - 1 );
		int i = 0;
		double s = 0.0;
		while ( i < width && li.hasPrevious() )
		{
			s += li.previous();
		}
		s /= ( double )width;
		return s;
	}
}
