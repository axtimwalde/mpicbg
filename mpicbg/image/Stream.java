/**
 *
 */
package mpicbg.image;

/**
 * @author Stephan
 *
 */
public abstract class Stream
		extends Container
		implements Iteratable, IteratableByDimension, RandomAccessible
{
	final int numPixels;

	Stream(final PixelType type, final int[] dim )
	{
		super(type, dim);

		int l = type.getNumChannels();
		for ( int i = 0; i < dim.length; ++i )
			l *= dim[ i ];

		numPixels = l;
	}

	final public int getNumPixels()
	{
		return numPixels;
	}

	abstract public ContainerRead<StreamCursor> getReader();
	abstract public ContainerWrite<StreamCursor> getWriter();

	public StreamIterator createIterator() { return new StreamIterator( this ); }
	public StreamIteratorByDimension createIteratorByDimension() { return new StreamIteratorByDimension( this ); }
	public StreamRandomAccess createRandomAccess() { return new StreamRandomAccess( this ); }

}
