package mpicbg.image;

public class FloatStreamRandomAccess
		extends FloatStreamReadableAndWritable
		implements RandomAccessible, Localizable, LocalizableFactory< FloatStreamRandomAccess >
{
	final protected int[] step;
	final int[] iByDim;
	
	FloatStreamRandomAccess( FloatStream stream )
	{
		super( stream );
		int nd = stream.getNumDim();
		iByDim = new int[ nd ];
		step = new int[ nd ];
		step[ 0 ] = stream.getPixelType().getNumChannels();
		for ( int d = 1; d < nd; ++d )
			step[ d ] = step[ d - 1 ] * container.getDim( d - 1 );
	}

	FloatStreamRandomAccess( FloatStream stream, int[] l )
	{
		this( stream );
		to( l );
	}
	
	/**
	 * Constructor at the floor of a given initial location.
	 * 
	 * @param stream
	 * @param l initial location
	 */
	FloatStreamRandomAccess( FloatStream stream, float[] l )
	{
		this( stream );
		to( l );
	}

	final public boolean isInside(){ return i > -1 && i < data.length; }

	public void to( int[] l )
	{
		i = l[ 0 ] * step[ 0 ];
		iByDim[ 0 ] = l[ 0 ];
		for ( int d = 1; d < step.length; ++d )
		{
			iByDim[ d ] = l[ d ];
			i += l[ d ] * step[ d ];
		}
	}
	
	public void to( float[] l )
	{
		iByDim[ 0 ] = l[ 0 ] > 0 ? ( int )l[ 0 ] : ( int )l[ 0 ] - 1;
		i = iByDim[ 0 ] * step[ 0 ];
		for ( int d = 1; d < l.length; ++d )
		{
			iByDim[ d ] = l[ d ] > 0 ? ( int )l[ d ] : ( int )l[ d ] - 1;
			i += iByDim[ d ] * step[ d ];
		}
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

	@Override
	public IteratableByDimension toIteratableByDimension()
	{
		return new FloatStreamIteratorByDimension( ( FloatStream )container, iByDim );
	}

	@Override
	public RandomAccessible toRandomAccessible()
	{
		return new FloatStreamRandomAccess( ( FloatStream )container, iByDim );
	}
}
