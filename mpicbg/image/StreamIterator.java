package mpicbg.image;

public class StreamIterator
		extends ReadableAndWritableCursor
		implements StreamCursor, Iterator, Localizable, LocalizableFactory< StreamIterator >
{	
	/**
	 * A local pointer to the CoordinateInformationStream and the Stream to 
	 * prevent continious casting 
	 */
	final protected Stream stream;
	final protected int numChannels;
	protected int i = 0;
	
	StreamIterator( Stream stream )
	{
		super( stream, stream.createDirectAccessStrategy() );
		
		// Local pointers to prevent continious casting 
		this.stream = (Stream)container;
		numChannels = stream.getPixelType().getNumChannels();
	}
	
	final public boolean isInside(){ return i > -1 && i < stream.getNumPixels(); }
	
	public void next(){ i += numChannels; }
	public void prev(){ i -= numChannels; }
	
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

	public IteratorByDimension toIteratableByDimension( )
	{
		int[] l = new int[ container.getNumDim() ];
		localize( l );
		return new StreamIteratorByDimension( stream, l, accessStrategy.clone(null) );
	}

	public RandomAccess toRandomAccessible( )
	{
		int[] l = new int[ container.getNumDim() ];
		localize( l );
		return new StreamRandomAccess( stream, l, accessStrategy.clone(null) );
	}

	public int getStreamIndex() { return i; }
}
