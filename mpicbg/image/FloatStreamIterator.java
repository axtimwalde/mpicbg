package mpicbg.image;

public class FloatStreamIterator extends FloatStreamReadableAndWritable implements Iteratable, Localizable
{
	FloatStreamIterator( FloatStream stream )
	{
		super( stream );
	}

	final public boolean hasNext(){ return i < data.length - numChannels; }
	final public boolean hasPrev(){ return i > 0; }
	
	public void next() throws OutOfBoundsException{ i += numChannels; }
	public void prev() throws OutOfBoundsException{ i -= numChannels; }
	
	public float[] localize()
	{
		float[] l = new float[ container.getNumDim() ];
		localize( l );
		return l;
	}
	public void localize( float[] l )
	{
		int r = i / numChannels;
		for ( int d = 0; d < l.length; ++d )
		{
			l[ d ] = r % container.getDim( d );
			r /= container.getDim( d );
		}		
	}
	public void localize( int[] l )
	{
		int r = i / numChannels;
		
		for ( int d = 0; d < l.length; ++d )
		{
			l[ d ] = r % container.getDim( d );
			r /= container.getDim( d );
		}		
	}
}
