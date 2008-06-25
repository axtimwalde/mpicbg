package mpicbg.image;

/**
 * 
 * @author Preibisch and Saalfeld
 *
 */
interface PixelType
{
	/**
	 * Get the number of channels.
	 *  
	 * @return int number of channels
	 */
	public int getNumChannels();
	
	/**
	 * 
	 * @param min
	 * @param max
	 */
	public void setVisibleRange( double min, double max );
	public void setVisibleRange( double[] min, double[] max );
	
	/**
	 * Computes the projection of a pixel instance to ARGB.
	 * 
	 * @param cursor pointer to the pixel instance
	 * @return integer
	 */
	int toRGBA( Readable cursor );

	/**
	 * Computes the projection of a pixel instance to an 8bit grey value.
	 * 
	 * @return byte
	 */
	byte toByte( Readable cursor );
	
	/**
	 * Computes the projection of a pixel instance to an 8bit grey value.
	 * 
	 * @return byte
	 */
	float toFloat( Readable cursor );
}
