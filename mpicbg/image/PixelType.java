package mpicbg.image;

/**
 * @author Preibisch and Saalfeld
 *
 */
public class PixelType
{
	final Class type;
	final int numChannels;
	PixelType( Class type, int numChannels )
	{
		this.type = type;
		this.numChannels = numChannels;
	}
	/**
	 * Returns the number of channels used in this image. 
	 * @return int - Number of channels
	 */
	final public int getNumChannels(){ return numChannels; }
	
	/**
	 * Returns the type of each channel in this image.  
	 * @return Class - Type of all individual channels 
	 */
	final public Class getType(){ return type; }
}
