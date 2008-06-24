package mpicbg.image;

public class FloatStreamIteratorByDimension extends FloatStreamReadableAndWritable implements IteratableByDimension, Localizable
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
	
	final public boolean hasNext( int d ){ return iByDim[ d ] < container.getDim( d ) - 1; }
	final public boolean hasPrev( int d ){ return iByDim[ d ] > 0; }
	
	public void next( int d ) throws OutOfBoundsException
	{
		++iByDim[ d ];
		i += step[ d ];
	}
	public void prev( int d ) throws OutOfBoundsException
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
}
