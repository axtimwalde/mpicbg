package mpicbg.image;

import mpicbg.image.interpolation.Interpolator;

public abstract class StreamCursor< I extends Container< ? extends PixelType, ? extends ContainerRead, ? extends ContainerWrite > >
		extends Cursor< I >
{
	/**
	 * Constructs the cursor.
	 * 
	 * @param c - The Container to work on
	 * @param ip - The Interpolator - still unused
	 * @param as - A prototype for the AccessStrategy which is cloned in order to give this cursor as 
	 * final instance to the AccessStrategy to be as fast as possible.
	 */
	public StreamCursor( I c, Interpolator ip, final AccessStrategy< I > as )
	{
		super( c, ip, as );
	}
	
	abstract public int getStreamIndex();
}
