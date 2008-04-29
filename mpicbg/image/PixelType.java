package mpicbg.image;

/**
 * @author Preibisch and Saalfeld
 *
 */
public abstract class PixelType
{
	public Class c = float.class;
	/**
	 * Returns the number of channels used in this image. 
	 * @return int - Number of channels
	 */
	abstract int getNumberOfChannels();
	
	/**
	 * Returns the type of each channel in this image.  
	 * @return Class - Type of all individual channels 
	 */
	abstract Class getType();
	
	/**
	 * Returns the number of Bytes required to store one pixel in memory (all Channels!)   
	 * @return int - Number of Bytes required to store one pixel 
	 */
	abstract int sizeOf();
	
}
