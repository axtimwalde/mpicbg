/**
 * 
 */
package mpicbg.image;

import mpicbg.image.interpolation.Interpolator;

/**
 * @author Preibisch and Saalfeld
 *
 */
public abstract class DirectLinearStreamIterator extends DirectPixelPointer implements DirectIteratable
{
	int i = 0;
	boolean hasNext = false;
	boolean hasPrev = false;
	
	DirectLinearStreamIterator( LinearStreamContainer pc, Interpolator ip )
	{
		super ( pc, ip );
		if ( ( ( LinearStreamContainer )container ).getStreamLength() > 1 ) hasNext = true;
	}
	
	public void next() throws OutOfBoundsException
	{
		switch ( ( ( LinearStreamContainer )container ).getStreamLength() - ++i )
		{
		case 1:
			hasNext = false;
		case 0:
			throw new OutOfBoundsException( "Iterator exceeded the stream's end." );
		default:
			hasPrev = true;
		}	
	}
	
	public void prev() throws OutOfBoundsException
	{
		switch ( --i )
		{
		case 0:
			hasPrev = false;
		case -1:
			throw new OutOfBoundsException( "Iterator exceeded the stream's begin." );
		default:
			hasNext = true;
		}	
	}
	
	public boolean hasNext(){ return hasNext; }
	public boolean hasPrev(){ return hasPrev; }
}
