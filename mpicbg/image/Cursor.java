/**
 * 
 */
package mpicbg.image;

import mpicbg.image.interpolation.Interpolator;

/**
 * @author Saalfeld
 *
 */
abstract public class Cursor
{
	final Container container;
	final Interpolator interpolator;
	final AccessStrategy accessStrategy;
	
	/**
	 * Constructs the cursor.
	 * 
	 * @param c - The Container to work on
	 * @param ip - The Interpolator - still unused
	 * @param as - A prototype for the AccessStrategy which is cloned in order to give this cursor as 
	 * final instance to the AccessStrategy to be as fast as possible.
	 */
	public Cursor( Container c, Interpolator ip, AccessStrategy as )
	{
		container = c;
		interpolator = ip;
		
		if ( as != null)
			accessStrategy = as.clone(this);
		else // for the ConstantCursor's
			accessStrategy = null;
	}
	
	/**
	 * Returns the interpolator used by this Cursor
	 * @return Interpolator
	 */
	public Interpolator getInterpolator()
	{
		return interpolator;
	}
		
	/**
	 * Returns if the cursor is inside the image
	 * @return boolean
	 */
	public abstract boolean isInside();
	
	/**
	 * Computes the projection of a pixel instance to ARGB.
	 * 
	 * This method risks a class cast exception for non readable cursors.
	 * Otherwise it would require to be defined in the interface Readable and
	 * thus implemented ever and ever again.
	 * 
	 * @param cursor pointer to the pixel instance
	 * @return integer
	 */
	public int toRGBA(){ return container.getPixelType().toRGBA( ( Readable )this ); }

	/**
	 * Computes the projection of a pixel instance to an 8bit grey value.
	 * 
	 * This method risks a class cast exception for non readable cursors.
	 * Otherwise it would require to be defined in the interface Readable and
	 * thus implemented ever and ever again.
	 * 
	 * @return byte
	 */
	byte toByte( Readable cursor ){ return container.getPixelType().toByte( ( Readable )this ); }
	
	/**
	 * Computes the projection of a pixel instance to an 8bit grey value.
	 * 
	 * This method risks a class cast exception for non readable cursors.
	 * Otherwise it would require to be defined in the interface Readable and
	 * thus implemented ever and ever again.
	 * 
	 * @return byte
	 */
	float toFloat( Readable cursor ){ return container.getPixelType().toFloat( ( Readable )this ); }
}
