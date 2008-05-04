package mpicbg.image;

import mpicbg.image.interpolation.*;

public abstract class PixelPointer
{
	/**
	 * A link to the container responsible for storing the data
	 */
	final Container container;
	final Interpolator interpolator;
	
	/**
	 * Returns the coordinates in PixelContainer
	 * @return coordinates in PixelContainer
	 */
	abstract double[] getCoordinates();
	abstract void getCoordinates( double[] coordinates );

	/**
	 * Returns the values.  Depending on the PixelType those are either byte[],
	 * short[], int[], long[], float[] or double[].
	 * 
	 * @return values
	 */
	abstract Object get();
	
	/**
	 * Computes the projection of the Raw Pixel intensity of type PixelType.getType() to ARGB
	 * To be implemented as a static method.
	 * 
	 * @return rgba integer
	 */
	public abstract int toRGBA();	

	/**
	 * Computes the projection of the Raw Pixel intensity of type PixelType.getType() to an 8-Bit Grayvalue
	 * To be implemented as a static method.
	 * 
	 * @return rgba integer
	 */
	public abstract int toByte();	

	PixelPointer( Container pc, Interpolator ip )
	{
		container = pc;
		interpolator = ip;
	}
}
