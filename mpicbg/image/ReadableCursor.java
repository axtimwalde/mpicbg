package mpicbg.image;

/**
 * A Cursor which can read.
 * 
 * @author Saalfeld and Preibisch
 *
 */
public abstract class ReadableCursor extends Cursor implements Readable
{
	/**
	 * Constructs a Float Cursor that can read
	 * 
	 * @param c - The image container
	 * @param as - The access strategy
	 * @param ci - The coordinate information
	 */
	ReadableCursor( final Container c, final AccessStrategy as )
	{
		super( c, null, as );
	}

	final public byte[] getBytes() { return accessStrategy.getBytes(); }
	final public short[] getShorts() { return accessStrategy.getShorts(); }
	final public int[] getInts() { return accessStrategy.getInts(); }
	final public long[] getLongs() { return accessStrategy.getLongs(); }
	final public float[] getFloats() { return accessStrategy.getFloats(); }
	final public double[] getDoubles() { return accessStrategy.getDoubles(); }
	final public Object[] get() { return accessStrategy.get(); }

	final public byte getByteChannel(final int c) { return accessStrategy.getByteChannel(c); }
	final public short getShortChannel(final int c) { return accessStrategy.getShortChannel(c); }
	final public int getIntChannel(final int c) { return accessStrategy.getIntChannel(c); }
	final public long getLongChannel(final int c) { return accessStrategy.getLongChannel(c); }
	final public float getFloatChannel(final int c) { return accessStrategy.getFloatChannel(c); }
	final public double getDoubleChannel(final int c) { return accessStrategy.getDoubleChannel(c); }
	final public Object getChannel(final int c) { return accessStrategy.getChannel(c); }

	final public void read(final Object[] a) { accessStrategy.read(a); }
	final public void read(final byte[] a) { accessStrategy.read(a); }
	final public void read(final short[] a) { accessStrategy.read(a); }
	final public void read(final int[] a) { accessStrategy.read(a); }
	final public void read(final long[] a) { accessStrategy.read(a); }
	final public void read(final float[] a) { accessStrategy.read(a); }
	final public void read(final double[] a) { accessStrategy.read(a); }
}
