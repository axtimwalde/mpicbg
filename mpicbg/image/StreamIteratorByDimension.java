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
public class StreamIteratorByDimension< I extends Stream< ? extends PixelType > >
		extends StreamCursor < I >
		implements IteratorByDimension< I >, Localizable, LocalizableFactory< StreamIteratorByDimension >
{
	final protected int[] step;
	final protected int[] iByDim;
	protected int i = 0;

	StreamIteratorByDimension( final I stream, final Access accessStrategy )
	{
		super( stream, null, accessStrategy );
		int nd = stream.getNumDim();
		iByDim = new int[nd];
		step = new int[ nd ];
		step[ 0 ] = stream.getPixelType().getNumChannels();
		for ( int d = 1; d < nd; ++d )
			step[ d ] = step[ d - 1 ] * container.getDim( d - 1 );
	}

	StreamIteratorByDimension( final I stream )
	{
		this( stream, new AccessDirect() );
	}

	/**
	 * Constructor at a given initial location.
	 * 
	 * @param stream
	 * @param l initial location
	 */
	StreamIteratorByDimension( final I stream, int[] l, final Access accessStrategy )
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

	StreamIteratorByDimension( final I stream, final float[] l )
	{
		this( stream, l, new AccessDirect() );
	}
	
	/**
	 * Constructor at the floor of a given initial location.
	 * 
	 * @param stream
	 * @param l initial location
	 */
	StreamIteratorByDimension( final I stream, final float[] l, final Access accessStrategy )
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
	
	final public boolean isInside()
	{
		boolean a = true;
		for ( int i = 0; a && i < iByDim.length; ++i )
		{
			a &= iByDim[ i ] >= 0;
			a &= iByDim[ i ] < container.getDim( i );
		}
		return a;
	}
	
	final public boolean isInside( int d ){ return iByDim[ d ] >= 0 && iByDim[ d ] < container.getDim( d ); }
	
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
		return new StreamIteratorByDimension< I >( container, iByDim, accessStrategy );
	}

	final public RandomAccess toRandomAccessible( )
	{
		return new StreamRandomAccess< I >( container, iByDim, accessStrategy );
	}
}
