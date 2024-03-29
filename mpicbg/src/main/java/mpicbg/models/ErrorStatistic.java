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

	private double squareDifferences = 0;
	private double squares = 0;

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
		values.add(new_value);
		sortedValues.add( new_value );

		if (values.lastIndex() == 0) {
			slope.add(0.0);
			mean = new_value;
			var = 0.0;
			var0 = 0.0;
		} else {
			slope.add(new_value - values.get(values.lastIndex()));

			final double delta = new_value - mean;
			mean += delta / values.nextIndex();

			squareDifferences += delta * (new_value - mean);
			var = squareDifferences / values.lastIndex();

			squares += new_value * new_value;
			var0 = squares / values.lastIndex();
		}

		std0 = Math.sqrt(var0);
		std = Math.sqrt(var);

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
