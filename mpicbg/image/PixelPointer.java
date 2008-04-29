package mpicbg.image;

public abstract class PixelPointer
{
	/**
	 * A link to the container responsible for storing the data
	 */
	final PixelContainer container;
	
	/**
	 * Returns the current integer position in the PixelContainer
	 * @return The integer location of this PixelPointer in the PixelContainer
	 */
	abstract int[] getGridLocation();
	
	/**
	 * Returns the current floating point position in the PixelContainer
	 * @return The floating point location of this PixelPointer in the PixelContainer
	 */
	abstract double[] getLocation();

	/**
	 * Returns the intensity for the current position. It might be interpolated if the location is not integer.
	 * @return The Raw Pixel intensity of type PixelType.getType()
	 */
	abstract Object getRawPixel();

	/**
	 * Computes the projection of the Raw Pixel intensity of type PixelType.getType() to ARGB
	 * To be implemented as a static method.
	 * 
	 * @param pixel raw pixel data
	 * @return rgba integer
	 */
	abstract int toRGBA( Object pixel );	

	/**
	 * Computes the projection of the Raw Pixel intensity of type PixelType.getType() to an 8-Bit Grayvalue
	 * To be implemented as a static method.
	 * 
	 * @param pixel raw pixel data
	 * @return rgba integer
	 */
	abstract int toByte( Object pixel );	

	PixelPointer( PixelContainer pc )
	{
		container = pc;
	}	
}
