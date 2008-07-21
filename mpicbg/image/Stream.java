/**
 * 
 */
package mpicbg.image;

/**
 * @author Stephan
 *
 */
public abstract class Stream< P extends PixelType, R extends ContainerRead, W extends ContainerWrite >
		extends Container< P, R, W >
		implements Iteratable, IteratableByDimension, RandomAccessible
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
	
	public Iterator createIterator() { return new StreamIterator(this); }
	public IteratorByDimension createIteratorByDimension() { return new StreamIteratorByDimension(this); }
	public RandomAccess createRandomAccess() { return new StreamRandomAccess(this); }	
	
}
