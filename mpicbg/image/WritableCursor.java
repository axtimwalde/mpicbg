package mpicbg.image;

public abstract class WritableCursor extends Cursor implements Writable, Operator
{
	/**
	 * Constructs a Float Cursor that can read
	 * 
	 * @param c - The image container
	 * @param as - The access strategy
	 */
	WritableCursor( final Container c, final AccessStrategy as )
	{
		super( c, null, as );
	}

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
