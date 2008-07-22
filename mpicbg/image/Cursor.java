package mpicbg.image;

import mpicbg.image.interpolation.Interpolator;

/**
 * A Cursor that can read and write through its AccessStrategy
 * 
 * @author Saalfeld and Preibisch
 *
 */
public abstract class Cursor< I extends Container< ? extends PixelType, ? extends Cursor > >
	implements Readable, Writable
{
	final I container;
	final Interpolator interpolator;
	final Access  accessStrategy;
	
	/**
	 * Constructs the cursor.
	 * 
	 * @param c - The Container to work on
	 * @param ip - The Interpolator - still unused
	 * @param as - A prototype for the AccessStrategy which is cloned in order to give this cursor as 
	 * final instance to the AccessStrategy to be as fast as possible.
	 */
	public Cursor( I c, Interpolator ip, final Access as )
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
	
	abstract byte getByteChannelDirect( final int c);
	abstract short getShortChannelDirect( final int c);
	abstract int getIntChannelDirect( final int c);
	abstract long getLongChannelDirect( final int c);
	abstract float getFloatChannelDirect( final int c);
	abstract double getDoubleChannelDirect( final int c);
	abstract Object getChannelDirect( final int c);

	abstract void readDirect( final Object[] a);
	abstract void readDirect( final byte[] a);
	abstract void readDirect( final short[] a);
	abstract void readDirect( final int[] a);
	abstract void readDirect( final long[] a);
	abstract void readDirect( final float[] a );
	abstract void readDirect( final double[] a);

	abstract void setDirect( final Object[] f );
	abstract void setDirect( final byte[] f );
	abstract void setDirect( final short[] f );
	abstract void setDirect( final int[] f );
	abstract void setDirect( final long[] f );
	abstract void setDirect( final float[] f );
	abstract void setDirect( final double[] f );

	abstract void setChannelDirect( final int i, final Object f ); 
	abstract void setChannelDirect( final int i, final byte f );
	abstract void setChannelDirect( final int i, final short f );
	abstract void setChannelDirect( final int i, final int f );
	abstract void setChannelDirect( final int i, final long f );
	abstract void setChannelDirect( final int i, final float f );
	abstract void setChannelDirect( final int i, final double f );
	
	final public void read( final Object[] f ){ accessStrategy.read( this, f ); }
	final public void read( final byte[] f ){ accessStrategy.read( this, f ); }
	final public void read( final short[] f ){ accessStrategy.read( this, f ); }
	final public void read( final int[] f ){ accessStrategy.read( this, f ); }
	final public void read( final long[] f ){ accessStrategy.read( this, f ); }
	final public void read( final float[] f ){ accessStrategy.read( this, f ); }
	final public void read( final double[] f ){ accessStrategy.read( this, f ); }
	
	final public Object getChannel( final int i ){ return accessStrategy.getChannel( this, i ); }
	final public byte getByteChannel( final int i ){ return accessStrategy.getByteChannel( this, i ); }
	final public short getShortChannel( final int i ){ return accessStrategy.getShortChannel( this, i ); }
	final public int getIntChannel( final int i ){ return accessStrategy.getIntChannel( this, i ); }
	final public long getLongChannel( final int i ){ return accessStrategy.getLongChannel( this, i ); }
	final public float getFloatChannel( final int i ){ return accessStrategy.getFloatChannel( this, i ); }
	final public double getDoubleChannel( final int i ){ return accessStrategy.getDoubleChannel( this, i ); }
	
	final public void set( final Object[] f ){ accessStrategy.set( this, f ); }
	final public void set( final byte[] f ){ accessStrategy.set( this, f ); }
	final public void set( final short[] f ){ accessStrategy.set( this, f ); }
	final public void set( final int[] f ){ accessStrategy.set( this, f ); }
	final public void set( final long[] f ){ accessStrategy.set( this, f ); }
	final public void set( final float[] f ){ accessStrategy.set( this, f ); }
	final public void set( final double[] f ){ accessStrategy.set( this, f ); }
	
	final public void setChannel( final int i, final Object f ){ accessStrategy.setChannel( this, i, f ); }
	final public void setChannel( final int i, final byte f ){ accessStrategy.setChannel( this, i, f ); }
	final public void setChannel( final int i, final short f ){ accessStrategy.setChannel( this, i, f ); }
	final public void setChannel( final int i, final int f ){ accessStrategy.setChannel( this, i, f ); }
	final public void setChannel( final int i, final long f ){ accessStrategy.setChannel( this, i, f ); }
	final public void setChannel( final int i, final float f ){ accessStrategy.setChannel( this, i, f ); }
	final public void setChannel( final int i, final double f ){ accessStrategy.setChannel( this, i, f ); }
	
	final public Object[] get()
	{
		final Object[] a = new Object[ container.getPixelType().getNumChannels() ];
		read( a );
		return a;
	}
	final public byte[] getBytes()
	{
		final byte[] a = new byte[ container.getPixelType().getNumChannels() ];
		read( a );
		return a;
	}
	final public short[] getShorts()
	{
		final short[] a = new short[ container.getPixelType().getNumChannels() ];
		read( a );
		return a;
	}
	final public int[] getInts()
	{
		final int[] a = new int[ container.getPixelType().getNumChannels() ];
		read( a );
		return a;
	}
	final public long[] getLongs()
	{
		final long[] a = new long[ container.getPixelType().getNumChannels() ];
		read( a );
		return a;
	}
	final public float[] getFloats()
	{
		final float[] a = new float[ container.getPixelType().getNumChannels() ];
		read( a );
		return a;
	}
	final public double[] getDoubles()
	{
		final double[] a = new double[ container.getPixelType().getNumChannels() ];
		read( a );
		return a;
	}
}
