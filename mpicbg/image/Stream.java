/**
 * 
 */
package mpicbg.image;

/**
 * @author Stephan
 *
 */
public abstract class Stream< P extends PixelType >
		extends Container< P, StreamCursor >
		implements Iteratable< Stream< P > >, IteratableByDimension< Stream< P > >, RandomAccessible< Stream< P > >
{
	final int numPixels;
	
	Stream(final P type, final int[] dim )
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
	
	public StreamIterator< Stream< P > > createIterator() { return new StreamIterator< Stream< P > >( this ); }
	public StreamIteratorByDimension< Stream< P > > createIteratorByDimension() { return new StreamIteratorByDimension< Stream< P > >( this ); }
	public StreamRandomAccess< Stream< P > > createRandomAccess() { return new StreamRandomAccess< Stream< P > >( this ); }	
	
}
