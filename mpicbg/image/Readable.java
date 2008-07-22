package mpicbg.image;

/**
 * A Cursor which can read.
 * 
 * @author Saalfeld and Preibisch
 *
 */
interface Readable
{
	/**
	 * Read (and cast if necessary) the pixel values into and array of the the
	 * required type.  Depending on the PixelType those are either byte[],
	 * short[], int[], long[], float[], double[] or Object[].
	 * 
	 */
	void read( Object[] f );
	void read( byte[] f );
	void read( short[] f );
	void read( int[] f );
	void read( long[] f );
	void read( float[] f );
	void read( double[] f );
	
	/**
	 * Get the pixel values.
	 */
	Object[] get();
	
	/**
	 * Typed version of {@link #get()}
	 */
	byte[] getBytes();
	
	/**
	 * Typed version of {@link #get()}
	 */
	short[] getShorts();
	
	/**
	 * Typed version of {@link #get()}
	 */
	int[] getInts();
	
	/**
	 * Typed version of {@link #get()}
	 */
	long[] getLongs();
	
	/**
	 * Typed version of {@link #get()}
	 */
	float[] getFloats();
	
	/**
	 * Typed version of {@link #get()}
	 */
	double[] getDoubles();

	
	/**
	 * Get the pixel channel value.
	 * @param c channel
	 */
	Object getChannel( int i );
	
	/**
	 * Basic type version of {@link #getChannel( int i )}
	 * @param c channel
	 */
	byte getByteChannel( int i );
	
	/**
	 * Basic type version of {@link #getChannel( int i )}
	 * @param c channel
	 */
	short getShortChannel( int i );
	
	/**
	 * Basic type version of {@link #getChannel( int i )}
	 * @param c channel
	 */
	int getIntChannel( int i );
	
	/**
	 * Basic type version of {@link #getChannel( int i )}
	 * @param c channel
	 */
	long getLongChannel( int i );
	
	/**
	 * Basic type version of {@link #getChannel( int i )}
	 * @param c channel
	 */
	float getFloatChannel( int i );
	
	/**
	 * Basic type version of {@link #getChannel( int i )}
	 * @param c channel
	 */
	double getDoubleChannel( int i );
}
