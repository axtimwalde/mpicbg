package mpicbg.image;

/**
 * 
 * @author Preibisch and Saalfeld
 *
 */
public abstract class PixelType
{
	final Class type;
	final int numChannels;
	PixelType( Class type, int numChannels )
	{
		this.type = type;
		this.numChannels = numChannels;
	}
	/**
	 * Get the number of channels.
	 *  
	 * @return int number of channels
	 */
	final public int getNumChannels(){ return numChannels; }
	
	/**
	 * Get the type of each channel.
	 * 
	 * @return Class type of all individual channels 
	 */
	final public Class getType(){ return type; }
	
	/**
	 * Computes the projection of a pixel instance to ARGB.
	 * 
	 * @param cursor pointer to the pixel instance
	 * @return integer
	 */
	public abstract int getRGBA( Cursor cursor );	

	/**
	 * Computes the projection of a pixel instance to an 8bit grey value.
	 * 
	 * @return byte
	 */
	public abstract byte toByte( Cursor cursor );
	
}
