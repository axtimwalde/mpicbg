package mpicbg.image;

public class FloatStreamIterator
		extends FloatStreamReadableAndWritable
		implements Iteratable, Localizable, LocalizableFactory< FloatStreamIterator >
{
	FloatStreamIterator( FloatStream stream )
	{
		super( stream );
	}

	final public boolean isInside(){ return i > -1 && i < data.length; }
	
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

	public IteratableByDimension toIteratableByDimension()
	{
		int[] l = new int[ container.getNumDim() ];
		localize( l );
		return new FloatStreamIteratorByDimension( ( FloatStream )container, l );
	}

	public RandomAccessible toRandomAccessible()
	{
		int[] l = new int[ container.getNumDim() ];
		localize( l );
		return new FloatStreamRandomAccess( ( FloatStream )container, l );
	}
}
