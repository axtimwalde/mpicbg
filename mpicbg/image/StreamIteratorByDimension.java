package mpicbg.image;

/**
 * 
 * @author Saalfeld and Preibisch
 * 
 * TODO Implement the handling of out of bounds locations.  For such locations
 *   hasNext and hasPrev will return false, but next and prev will actually
 *   move the cursor.  We are still not clear how to specify the strategy for
 *   out of bounds bouncing.
 */
public class StreamIteratorByDimension 
		extends ReadableAndWritableCursor 
		implements StreamCursor, IteratorByDimension, Localizable, LocalizableFactory< StreamIteratorByDimension >
{
	/**
	 * A local pointer to the CoordinateInformationStream and the Stream to 
	 * prevent continious casting 
	 */
	final Stream stream;

	final protected int[] step;
	final protected int[] iByDim;
	protected int i = 0;

	StreamIteratorByDimension( final Stream stream, final AccessStrategy accessStrategy )
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

	StreamIteratorByDimension( final Stream stream )
	{
		this( stream, stream.createDirectAccessStrategy() );
	}

	/**
	 * Constructor at a given initial location.
	 * 
	 * @param stream
	 * @param l initial location
	 */
	StreamIteratorByDimension( final Stream stream, int[] l, final AccessStrategy accessStrategy )
	{
		this( stream, accessStrategy );
		System.arraycopy( l, 0, iByDim, 0, iByDim.length );
		i = l[ 0 ] * step[ 0 ];
		iByDim[ 0 ] = l[ 0 ];
		for ( int d = 1; d < step.length; ++d )
		{
			iByDim[ d ] = l[ d ];
			i += l[ d ] * step[ d ];
		}
	}

	StreamIteratorByDimension( final FloatStream stream, final float[] l )
	{
		this(stream, l, stream.createDirectAccessStrategy());
	}
	
	/**
	 * Constructor at the floor of a given initial location.
	 * 
	 * @param stream
	 * @param l initial location
	 */
	StreamIteratorByDimension( final Stream stream, final float[] l, final AccessStrategy accessStrategy )
	{
		this( stream, accessStrategy );
		iByDim[ 0 ] = l[ 0 ] > 0 ? ( int )l[ 0 ] : ( int )l[ 0 ] - 1;
		i = iByDim[ 0 ] * step[ 0 ];
		for ( int d = 1; d < l.length; ++d )
		{
			iByDim[ d ] = l[ d ] > 0 ? ( int )l[ d ] : ( int )l[ d ] - 1;
			i += iByDim[ d ] * step[ d ];
		}
	}

	final public boolean isInside(){ return i > -1 && i < stream.getNumPixels(); }
	final public boolean isInside( int d ){ return iByDim[ d ] > -1 && iByDim[ d ] < container.getDim( d ); }
	
	final public void next( final int d )
	{
		++iByDim[ d ];
		i += step[ d ];
	}
	final public void prev( final int d )
	{
		--iByDim[ d ];
		i -= step[ d ];
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
