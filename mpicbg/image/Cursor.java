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
	
	public Cursor( Container c, Interpolator ip )
	{
		container = c;
		interpolator = ip;
	}
	
	/**
	 * Read (and cast if necessary) the pixel values into and array of the the
	 * required type.  Depending on the PixelType those are either byte[],
	 * short[], int[], long[], float[], double[] or Object[].
	 * 
	 */
	abstract void read( Object[] a );
	abstract void read( byte[] a );
	abstract void read( short[] a );
	abstract void read( int[] a );
	abstract void read( long[] a );
	abstract void read( float[] a );
	abstract void read( double[] a );
	
	/**
	 * Get the pixel values.
	 */
	abstract public Object[] get();
	
	/**
	 * Typed version of {@link #get()}
	 */
	abstract public byte[] getBytes();
	
	/**
	 * Typed version of {@link #get()}
	 */
	abstract public short[] getShorts();
	
	/**
	 * Typed version of {@link #get()}
	 */
	abstract public int[] getInts();
	
	/**
	 * Typed version of {@link #get()}
	 */
	abstract public long[] getLongs();
	
	/**
	 * Typed version of {@link #get()}
	 */
	abstract public float[] getFloats();
	
	/**
	 * Typed version of {@link #get()}
	 */
	abstract public double[] getDoubles();

	
	/**
	 * Get the pixel channel value.
	 * @param c channel
	 */
	abstract public Object getChannel( int c );
	
	/**
	 * Basic type version of {@link #getChannel( int c )}
	 * @param c channel
	 */
	abstract public byte[] getByteChannel( int c );
	
	/**
	 * Basic type version of {@link #getChannel( int c )}
	 * @param c channel
	 */
	abstract public short[] getShortChannel( int c );
	
	/**
	 * Basic type version of {@link #getChannel( int c )}
	 * @param c channel
	 */
	abstract public int[] getIntChannel( int c );
	
	/**
	 * Basic type version of {@link #getChannel( int c )}
	 * @param c channel
	 */
	abstract public long[] getLongChannel( int c );
	
	/**
	 * Basic type version of {@link #getChannel( int c )}
	 * @param c channel
	 */
	abstract public float[] getFloatChannel( int c );
	
	/**
	 * Basic type version of {@link #getChannel( int c )}
	 * @param c channel
	 */
	abstract public double[] getDoubleChannel( int c );

}
