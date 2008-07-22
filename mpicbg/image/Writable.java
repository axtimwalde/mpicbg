package mpicbg.image;

/**
 * A Cursor that can write.
 * 
 * @author Saalfeld and Preibisch
 *
 */
public interface Writable
{
	/**
	 * Read (and cast if necessary) the pixel values into and array of the the
	 * required type.  Depending on the PixelType those are either byte[],
	 * short[], int[], long[], float[], double[] or Object[].
	 * 
	 */
	void set( Object[] f );
	void set( byte[] f );
	void set( short[] f );
	void set( int[] f );
	void set( long[] f );
	void set( float[] f );
	void set( double[] f );
	
	/**
	 * Set one channel
	 * @param v value
	 * @param c channel
	 */
	void setChannel( int i, Object f );
	void setChannel( int i, byte f );
	void setChannel( int i, short f );
	void setChannel( int i, int f );
	void setChannel( int i, long f );
	void setChannel( int i, float f );
	void setChannel( int i, double f );
	
}
