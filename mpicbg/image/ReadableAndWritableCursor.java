package mpicbg.image;

public abstract class ReadableAndWritableCursor extends Cursor implements Readable, Writable, Operator
{
	/**
	 * Constructs a Float Cursor that can read
	 * 
	 * @param c - The image container
	 * @param as - The access strategy
	 * @param ci - The coordinate information
	 */
	ReadableAndWritableCursor( final Container c, final AccessStrategy as )
	{
		super( c, null, as );
	}
	
	//
	// Readable methods
	//
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
	
	//
	// Writable Methods
	//
	final public void set(final Object[] a) { accessStrategy.set(a); }
	final public void set(final byte[] a) { accessStrategy.set(a); }
	final public void set(final short[] a) { accessStrategy.set(a); }
	final public void set(final int[] a) { accessStrategy.set(a); }
	final public void set(final long[] a) { accessStrategy.set(a); }
	final public void set(final float[] a) { accessStrategy.set(a); }
	final public void set(final double[] a) { accessStrategy.set(a); }

	final public void setChannel(final Object v, final int c) { accessStrategy.setChannel(v, c); }
	final public void setChannel(final byte v, final int c) { accessStrategy.setChannel(v, c); }
	final public void setChannel(final short v, final int c) { accessStrategy.setChannel(v, c); }
	final public void setChannel(final int v, final int c) { accessStrategy.setChannel(v, c); }
	final public void setChannel(final long v, final int c) { accessStrategy.setChannel(v, c); }
	final public void setChannel(final float v, final int c) { accessStrategy.setChannel(v, c); }
	final public void setChannel(final double v, final int c) { accessStrategy.setChannel(v, c); }

	//
	// Operator Methods
	//
	final public void add(final Readable c) { accessStrategy.add(c); }
	final public void add(final byte c) { accessStrategy.add(c); }
	final public void add(final short c) { accessStrategy.add(c); }
	final public void add(final int c) { accessStrategy.add(c); }
	final public void add(final long c) { accessStrategy.add(c); }
	final public void add(final float c) { accessStrategy.add(c); }
	final public void add(final double c) { accessStrategy.add(c); }

	final public void div(final Readable c) { accessStrategy.div(c); }
	final public void div(final byte c) { accessStrategy.div(c); }
	final public void div(final short c) { accessStrategy.div(c); }
	final public void div(final int c) { accessStrategy.div(c); }
	final public void div(final long c) { accessStrategy.div(c); }
	final public void div(final float c) { accessStrategy.div(c); }
	final public void div(final double c) { accessStrategy.div(c); }
	
	final public void mul(final Readable c) { accessStrategy.mul(c); }
	final public void mul(final byte c) { accessStrategy.mul(c); }
	final public void mul(final short c) { accessStrategy.mul(c); }
	final public void mul(final int c) { accessStrategy.mul(c); }
	final public void mul(final long c) { accessStrategy.mul(c); }
	final public void mul(final float c) { accessStrategy.mul(c); }
	final public void mul(final double c) { accessStrategy.mul(c); }

	final public void sub(final Readable c) { accessStrategy.sub(c); }
	final public void sub(final byte c) { accessStrategy.sub(c); }
	final public void sub(final short c) { accessStrategy.sub(c); }
	final public void sub(final int c) { accessStrategy.sub(c); }
	final public void sub(final long c) { accessStrategy.sub(c); }
	final public void sub(final float c) { accessStrategy.sub(c); }
	final public void sub(final double c) { accessStrategy.sub(c); }
}
