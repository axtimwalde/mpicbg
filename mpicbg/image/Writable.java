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
	void set( Object[] a );
	void set( byte[] a );
	void set( short[] a );
	void set( int[] a );
	void set( long[] a );
	void set( float[] a );
	void set( double[] a );
	
	/**
	 * Set one channel
	 * @param v value
	 * @param c channel
	 */
	void setChannel( Object v, int c );
	void setChannel( byte v, int c );
	void setChannel( short v, int c );
	void setChannel( int v, int c );
	void setChannel( long v, int c );
	void setChannel( float v, int c );
	void setChannel( double v, int c );
}
