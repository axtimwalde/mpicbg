package mpicbg.image;

public class StreamIterator
		extends StreamCursor
		implements Iterator, Localizable, LocalizableFactory< StreamIterator >
{	
	protected int i = 0;
	
	StreamIterator( Stream stream )
	{
		super( stream, null, new AccessDirect() );
	}
	
	final public boolean isInside(){ return i > -1 && i < stream.getNumPixels(); }
	
	public void next(){ i += stream.getPixelType().getNumChannels(); }
	public void prev(){ i -= stream.getPixelType().getNumChannels(); }
	
	public float[] localize()
	{
		float[] l = new float[ stream.getNumDim() ];
		localize( l );
		return l;
	}
	public void localize( float[] l )
	{
		int r = i / stream.getPixelType().getNumChannels();
		for ( int d = 0; d < l.length; ++d )
		{
			l[ d ] = r % stream.getDim( d );
			r /= stream.getDim( d );
		}		
	}
	public void localize( int[] l )
	{
		int r = i / stream.getPixelType().getNumChannels();
		
		for ( int d = 0; d < l.length; ++d )
		{
			l[ d ] = r % stream.getDim( d );
			r /= stream.getDim( d );
		}		
	}

	public IteratorByDimension toIteratableByDimension( )
	{
		int[] l = new int[ stream.getNumDim() ];
		localize( l );
		return new StreamIteratorByDimension( stream, l, accessStrategy );
	}

	public RandomAccess toRandomAccessible( )
	{
		int[] l = new int[ stream.getNumDim() ];
		localize( l );
		return new StreamRandomAccess( stream, l, accessStrategy );
	}

	public int getStreamIndex() { return i; }
}
