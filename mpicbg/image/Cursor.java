package mpicbg.image;

import mpicbg.image.interpolation.Interpolator;

/**
 * A Cursor that can read and write through its AccessStrategy
 * 
 * @author Saalfeld and Preibisch
 *
 */
public abstract class Cursor< I extends Container< ? extends PixelType, ? extends ContainerRead, ? extends ContainerWrite > >
{
	final I container;
	final Interpolator interpolator;
	final AccessStrategy< I, ? extends Cursor< ? > >  accessStrategy;
	
	/**
	 * Constructs the cursor.
	 * 
	 * @param c - The Container to work on
	 * @param ip - The Interpolator - still unused
	 * @param as - A prototype for the AccessStrategy which is cloned in order to give this cursor as 
	 * final instance to the AccessStrategy to be as fast as possible.
	 */
	public Cursor( I c, Interpolator ip, final AccessStrategy< I, ? extends Cursor< ? > > as )
	{
		container = c;
		interpolator = ip;
		accessStrategy = as;
	}
	
	/**
	 * Returns the interpolator used by this Cursor
	 * @return Interpolator
	 */
	public Interpolator getInterpolator()
	{
		return interpolator;
	}
		
	/**
	 * Returns if the cursor is inside the image
	 * @return boolean
	 */
	public abstract boolean isInside();
	
	/**
	 * Computes the projection of a pixel instance to ARGB.
	 * 
	 * This method risks a class cast exception for non readable cursors.
	 * Otherwise it would require to be defined in the interface Readable and
	 * thus implemented ever and ever again.
	 * 
	 * @param cursor pointer to the pixel instance
	 * @return integer
	 */
	public int toRGBA(){ return container.getPixelType().toRGBA( ( Readable )this ); }

	/**
	 * Computes the projection of a pixel instance to an 8bit grey value.
	 * 
	 * This method risks a class cast exception for non readable cursors.
	 * Otherwise it would require to be defined in the interface Readable and
	 * thus implemented ever and ever again.
	 * 
	 * @return byte
	 */
	byte toByte( Readable cursor ){ return container.getPixelType().toByte( ( Readable )this ); }
	
	/**
	 * Computes the projection of a pixel instance to an 8bit grey value.
	 * 
	 * This method risks a class cast exception for non readable cursors.
	 * Otherwise it would require to be defined in the interface Readable and
	 * thus implemented ever and ever again.
	 * 
	 * @return byte
	 */
	float toFloat( Readable cursor ){ return container.getPixelType().toFloat( ( Readable )this ); }
	
	

//	final public byte[] getBytes() { return accessStrategy.getBytes(); }
//	final public short[] getShorts() { return accessStrategy.getShorts(); }
//	final public int[] getInts() { return accessStrategy.getInts(); }
//	final public long[] getLongs() { return accessStrategy.getLongs(); }
//	final public float[] getFloats() { return accessStrategy.getFloats(); }
//	final public double[] getDoubles() { return accessStrategy.getDoubles(); }
//	final public Object[] get() { return accessStrategy.get(); }

	final public byte getByteChannel(final int c) { return accessStrategy.getByteChannel( this, c ); }
	final public short getShortChannel(final int c) { return accessStrategy.getShortChannel( this, c ); }
	final public int getIntChannel(final int c) { return accessStrategy.getIntChannel( this, c); }
	final public long getLongChannel(final int c) { return accessStrategy.getLongChannel( this, c); }
	final public float getFloatChannel(final int c) { return accessStrategy.getFloatChannel( this, c); }
	final public double getDoubleChannel(final int c) { return accessStrategy.getDoubleChannel( this, c); }
	final public Object getChannel(final int c) { return accessStrategy.getChannel( this, c); }

	final public void read(final Object[] a) { accessStrategy.read( this, a); }
	final public void read(final byte[] a) { accessStrategy.read( this, a); }
	final public void read(final short[] a) { accessStrategy.read( this, a); }
	final public void read(final int[] a) { accessStrategy.read( this, a); }
	final public void read(final long[] a) { accessStrategy.read( this, a); }
	final public void read(final float[] a ) { accessStrategy.read( this, a ); }
	final public void read(final double[] a) { accessStrategy.read( this, a); }
}
