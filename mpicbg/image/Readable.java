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
	void read( Object[] a );
	void read( byte[] a );
	void read( short[] a );
	void read( int[] a );
	void read( long[] a );
	void read( float[] a );
	void read( double[] a );
	
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
	Object getChannel( int c );
	
	/**
	 * Basic type version of {@link #getChannel( int c )}
	 * @param c channel
	 */
	byte getByteChannel( int c );
	
	/**
	 * Basic type version of {@link #getChannel( int c )}
	 * @param c channel
	 */
	short getShortChannel( int c );
	
	/**
	 * Basic type version of {@link #getChannel( int c )}
	 * @param c channel
	 */
	int getIntChannel( int c );
	
	/**
	 * Basic type version of {@link #getChannel( int c )}
	 * @param c channel
	 */
	long getLongChannel( int c );
	
	/**
	 * Basic type version of {@link #getChannel( int c )}
	 * @param c channel
	 */
	float getFloatChannel( int c );
	
	/**
	 * Basic type version of {@link #getChannel( int c )}
	 * @param c channel
	 */
	double getDoubleChannel( int c );
}
