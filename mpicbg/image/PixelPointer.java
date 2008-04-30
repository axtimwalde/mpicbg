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
	abstract void getCoordinates( double[] coordinates );

	/**
	 * Returns the values.  Depending on the PixelType those are either byte[],
	 * short[], int[], long[], float[] or double[].
	 * 
	 * @return values
	 */
	abstract Object get();
	
	/**
	 * Writes the values of .  Depending on the PixelType those are either byte[],
	 * short[], int[], long[], float[] or double[].
	 * 
	 * @return values
	 */
	abstract void get( Object a );
	abstract void get( byte[] a );
	abstract void get( short[] a );
	abstract void get( int[] a );
	abstract void get( long[] a );
	abstract void get( float[] a );
	abstract void get( double[] a );
	
	/**
	 * Typed version of {@link #get()}
	 * 
	 * @return values
	 */
	public abstract byte[] getBytes();
	
	/**
	 * Typed version of {@link #get()}
	 * 
	 * @return values
	 */
	public abstract short[] getShorts();
	
	/**
	 * Typed version of {@link #get()}
	 * 
	 * @return values
	 */
	public abstract int[] getInts();
	
	/**
	 * Typed version of {@link #get()}
	 * 
	 * @return values
	 */
	public abstract long[] getLongs();
	
	/**
	 * Typed version of {@link #get()}
	 * 
	 * @return values
	 */
	public abstract float[] getFloats();
	
	/**
	 * Typed version of {@link #get()}
	 * 
	 * @return values
	 */
	public abstract double[] getDoubles();

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

	PixelPointer( PixelContainer pc, Interpolator ip )
	{
		container = pc;
		interpolator = ip;
	}
}
