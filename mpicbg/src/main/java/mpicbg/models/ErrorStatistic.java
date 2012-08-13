package mpicbg.models;

import java.util.Collections;
import java.lang.IndexOutOfBoundsException;

import mpicbg.util.RingBuffer;

public class ErrorStatistic
{
	final static IndexOutOfBoundsException tooWide = new IndexOutOfBoundsException( "Cannot estimate a wide slope for width larger than than the number of sample." );
	
	final public RingBuffer< Double > values;
	final public RingBuffer< Double > slope;
	final public RingBuffer< Double > sortedValues;
	
	public ErrorStatistic( final int capacity )
	{
		values = new RingBuffer< Double >( capacity );
		slope = new RingBuffer< Double >( capacity );
		sortedValues = new RingBuffer< Double >( capacity );
	}
	
	public double var0 = 0;		// variance relative to 0
	public double var = 0;		// variance relative to mean
	public double std0 = 0;		// standard-deviation relative to 0
	public double std = 0;		// standard-deviation
	public double mean = 0;
	private double median = 0;
	public double getMedian()
	{
		Collections.sort( sortedValues );
		if ( sortedValues.size() % 2 == 0 )
		{
			int m = sortedValues.size() / 2;
			median = ( sortedValues.get( m - 1 ) + sortedValues.get( m ) ) / 2.0;
		}
		else
			median = sortedValues.get( sortedValues.size() / 2 );
		
		return median;
	}
	
	public double min = Double.MAX_VALUE;
	public double max = 0;
	
	final public void add( final double new_value )
	{
		if ( values.nextIndex() >= 1 ) slope.add( new_value - values.get( values.lastIndex() ) );
		else slope.add( 0.0 );
		mean = ( mean * values.nextIndex() + new_value );
		values.add( new_value );
		mean /= ( values.nextIndex() );
		
		var0 += new_value * new_value / ( double )( values.lastIndex() );
		std0 = Math.sqrt( var0 );
		
		double tmp = new_value - mean;
		var += tmp * tmp / ( double )( values.lastIndex() );
		std = Math.sqrt( var );
		
		sortedValues.add( new_value );
		
		if ( new_value < min ) min = new_value;
		if ( new_value > max ) max = new_value;
	}
	
	final public double getWideSlope( final int width ) throws IndexOutOfBoundsException
	{
		return ( values.get( values.lastIndex() ) - values.get( values.lastIndex() - width ) ) / width;
	}
	
	public int n() { return values.nextIndex(); }
	
	public void clear()
	{
		values.clear();
		slope.clear();
		sortedValues.clear();
		
		var0 = 0;		// variance relative to 0
		var = 0;		// variance relative to mean
		std0 = 0;		// standard-deviation relative to 0
		std = 0;		// standard-deviation
		mean = 0;
		median = 0;
		
	}
}
