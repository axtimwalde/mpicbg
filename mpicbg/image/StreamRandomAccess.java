package mpicbg.image;

public class StreamRandomAccess
		extends ReadableAndWritableCursor
		implements StreamCursor, RandomAccess, Localizable, LocalizableFactory< StreamRandomAccess >
{
	/**
	 * A local pointer to the CoordinateInformationStream and the Stream to 
	 * prevent continious casting 
	 */
	final Stream stream;	
	
	final protected int[] step;
	final protected int[] iByDim;
	protected int i = 0;
	
	StreamRandomAccess( Stream stream, AccessStrategy accessStrategy )
	{
		super( stream, accessStrategy );
		int nd = stream.getNumDim();
		iByDim = new int[nd];		
		step = new int[ nd ];
		step[ 0 ] = stream.getPixelType().getNumChannels();
		for ( int d = 1; d < nd; ++d )
			step[ d ] = step[ d - 1 ] * container.getDim( d - 1 );
		
		// Local pointers to prevent continious casting 
		this.stream = (Stream)container;		
	}

	StreamRandomAccess( Stream stream )
	{
		this(stream, stream.createDirectAccessStrategy());
	}


	StreamRandomAccess( Stream stream, int[] l, AccessStrategy accessStrategy )
	{
		this( stream, accessStrategy );
		to( l );
	}

	StreamRandomAccess( Stream stream, int[] l )
	{
		this(stream, l, stream.createDirectAccessStrategy());
	}
	
	/**
	 * Constructor at the floor of a given initial location.
	 * 
	 * @param stream
	 * @param l initial location
	 */
	StreamRandomAccess( Stream stream, float[] l, AccessStrategy accessStrategy )
	{
		this( stream, accessStrategy );
		to( l );
	}

	StreamRandomAccess( Stream stream, float[] l )
	{
		this(stream, l, stream.createDirectAccessStrategy());
	}

	final public boolean isInside(){ return i > -1 && i < stream.getNumPixels(); }

	final public void to( final int[] l )
	{
		i = l[ 0 ] * step[ 0 ];
		iByDim[ 0 ] = l[ 0 ];
		for ( int d = 1; d < step.length; ++d )
		{
			iByDim[ d ] = l[ d ];
			i += l[ d ] * step[ d ];
		}
	}
	
	final public void to( final float[] l )
	{
		iByDim[ 0 ] = l[ 0 ] > 0 ? ( int )l[ 0 ] : ( int )l[ 0 ] - 1;
		i = iByDim[ 0 ] * step[ 0 ];
		for ( int d = 1; d < l.length; ++d )
		{
			iByDim[ d ] = l[ d ] > 0 ? ( int )l[ d ] : ( int )l[ d ] - 1;
			i += iByDim[ d ] * step[ d ];
		}
	}
	
	final public float[] localize()
	{
		float[] l = new float[ iByDim.length ];
		localize( l );
		return l;
	}
	final public void localize( final float[] l )
	{
		for ( int d = 0; d < l.length; ++d )
			l[ d ] = iByDim[ d ];
	}
	final public void localize( final int[] l )
	{
		for ( int d = 0; d < l.length; ++d )
			l[ d ] = iByDim[ d ];
	}

	final public int getStreamIndex() { return i; }

	final public IteratorByDimension toIteratableByDimension( )
	{
		return new StreamIteratorByDimension( ( Stream )container, iByDim, accessStrategy.clone(null) );
	}

	final public RandomAccess toRandomAccessible( )
	{
		return new StreamRandomAccess( ( Stream )container, iByDim, accessStrategy.clone(null) );
	}
}
