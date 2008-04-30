package mpicbg.image;

import mpicbg.image.interpolation.*;

public abstract class PixelPointer
{
	/**
	 * A link to the container responsible for storing the data
	 */
	final PixelContainer container;
	final Interpolator interpolator;
	
	/**
	 * Returns the coordinates in PixelContainer
	 * @return coordinates in PixelContainer
	 */
	abstract double[] getCoordinates();

	/**
	 * Returns the values.  Depending on the PixelType those are either byte[],
	 * short[], int[], long[], float[] or double[].
	 * 
	 * @return values
	 */
	abstract Object get();
	
	/**
	 * Typed version of {@link #get()}
	 * 
	 * @return values
	 */
	abstract byte[] getBytes();
	
	/**
	 * Typed version of {@link #get()}
	 * 
	 * @return values
	 */
	abstract short[] getShorts();
	
	/**
	 * Typed version of {@link #get()}
	 * 
	 * @return values
	 */
	abstract int[] getInts();
	
	/**
	 * Typed version of {@link #get()}
	 * 
	 * @return values
	 */
	abstract long[] getLongs();
	
	/**
	 * Typed version of {@link #get()}
	 * 
	 * @return values
	 */
	abstract float[] getFloats();
	
	/**
	 * Typed version of {@link #get()}
	 * 
	 * @return values
	 */
	abstract double[] getDoubles();

	/**
	 * Computes the projection of the Raw Pixel intensity of type PixelType.getType() to ARGB
	 * To be implemented as a static method.
	 * 
	 * @return rgba integer
	 */
	abstract int toRGBA();	

	/**
	 * Computes the projection of the Raw Pixel intensity of type PixelType.getType() to an 8-Bit Grayvalue
	 * To be implemented as a static method.
	 * 
	 * @return rgba integer
	 */
	abstract int toByte();	

	PixelPointer( PixelContainer pc, Interpolator ip )
	{
		container = pc;
		interpolator = ip;
	}
}
