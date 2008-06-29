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
public class FloatStreamIteratorByDimension extends FloatStreamReadableAndWritable implements IteratableByDimension, Localizable, LocalizableFactory< FloatStreamIteratorByDimension >
{
	final protected int[] step;	
	final int[] iByDim;
	
	FloatStreamIteratorByDimension( FloatStream stream )
	{
		super( stream );
		int nd = stream.getNumDim();
		iByDim = new int[ nd ];
		step = new int[ nd ];
		step[ 0 ] = stream.getPixelType().getNumChannels();
		for ( int d = 1; d < nd; ++d )
			step[ d ] = step[ d - 1 ] * container.getDim( d - 1 );
	}

	/**
	 * Constructor at a given initial location.
	 * 
	 * @param stream
	 * @param l initial location
	 */
	FloatStreamIteratorByDimension( FloatStream stream, int[] l )
	{
		this( stream );
		System.arraycopy( l, 0, iByDim, 0, iByDim.length );
		i = l[ 0 ] * step[ 0 ];
		iByDim[ 0 ] = l[ 0 ];
		for ( int d = 1; d < step.length; ++d )
		{
			iByDim[ d ] = l[ d ];
			i += l[ d ] * step[ d ];
		}
	}

	/**
	 * Constructor at the floor of a given initial location.
	 * 
	 * @param stream
	 * @param l initial location
	 */
	FloatStreamIteratorByDimension( FloatStream stream, float[] l )
	{
		this( stream );
		iByDim[ 0 ] = l[ 0 ] > 0 ? ( int )l[ 0 ] : ( int )l[ 0 ] - 1;
		i = iByDim[ 0 ] * step[ 0 ];
		for ( int d = 1; d < l.length; ++d )
		{
			iByDim[ d ] = l[ d ] > 0 ? ( int )l[ d ] : ( int )l[ d ] - 1;
			i += iByDim[ d ] * step[ d ];
		}
	}

	final public boolean isInside(){ return i > -1 && i < data.length; }
	final public boolean isInside( int d ){ return iByDim[ d ] > -1 && iByDim[ d ] < container.getDim( d ); }
	
	public void next( int d )
	{
		++iByDim[ d ];
		i += step[ d ];
	}
	public void prev( int d )
	{
		--iByDim[ d ];
		i -= step[ d ];
	}
	
	public float[] localize()
	{
		float[] l = new float[ iByDim.length ];
		localize( l );
		return l;
	}
	public void localize( float[] l )
	{
		for ( int d = 0; d < l.length; ++d )
			l[ d ] = iByDim[ d ];
	}
	public void localize( int[] l )
	{
		System.arraycopy( iByDim, 0, l, 0, l.length );
	}

	public IteratableByDimension toIteratableByDimension()
	{
		return new FloatStreamIteratorByDimension( ( FloatStream )container, iByDim );
	}

	public RandomAccessible toRandomAccessible()
	{
		return new FloatStreamRandomAccess( ( FloatStream )container, iByDim );
	}
}
