package mpicbg.image;

import mpicbg.image.interpolation.Interpolator;

public abstract class StreamCursor
		extends Cursor
{
	final Stream stream;
	final ContainerRead<StreamCursor> reader;
	final ContainerWrite<StreamCursor> writer;

	/**
	 * Constructs the cursor.
	 *
	 * @param c - The Container to work on
	 * @param ip - The Interpolator - still unused
	 * @param as - A prototype for the AccessStrategy which is cloned in order to give this cursor as
	 * final instance to the AccessStrategy to be as fast as possible.
	 */
	public StreamCursor( Stream stream, Interpolator ip, final Access as )
	{
		super( stream, ip, as );
		this.stream = stream;
		this.reader = stream.getReader();
		this.writer = stream.getWriter();
	}

	abstract public int getStreamIndex();

	final byte getByteChannelDirect( final int c) { return stream.getReader().getByteChannel( this, c ); }
	final short getShortChannelDirect( final int c) { return reader.getShortChannel( this, c ); }
	final int getIntChannelDirect( final int c) { return reader.getIntChannel( this, c); }
	final long getLongChannelDirect( final int c) { return reader.getLongChannel( this, c); }
	final float getFloatChannelDirect( final int c) { return reader.getFloatChannel( this, c); }
	final double getDoubleChannelDirect( final int c) { return reader.getDoubleChannel( this, c); }
	final Object getChannelDirect( final int c) { return reader.getChannel( this, c); }

	final void readDirect( final Object[] a) { reader.read( this, a); }
	final void readDirect( final byte[] a) { reader.read( this, a); }
	final void readDirect( final short[] a) { reader.read( this, a); }
	final void readDirect( final int[] a) { reader.read( this, a); }
	final void readDirect( final long[] a) { reader.read( this, a); }
	final void readDirect( final float[] a ) { reader.read( this, a ); }
	final void readDirect( final double[] a) { reader.read( this, a); }

	final void setDirect( final Object[] f ){ writer.set( this, f ); }
	final void setDirect( final byte[] f ){ writer.set( this, f ); }
	final void setDirect( final short[] f ){ writer.set( this, f ); }
	final void setDirect( final int[] f ){ writer.set( this, f ); }
	final void setDirect( final long[] f ){ writer.set( this, f ); }
	final void setDirect( final float[] f ){ writer.set( this, f ); }
	final void setDirect( final double[] f ){ writer.set( this, f ); }

	final void setChannelDirect( final int i, final Object f ){ writer.setChannel( this, i, f ); }
	final void setChannelDirect( final int i, final byte f ){ writer.setChannel( this, i, f ); }
	final void setChannelDirect( final int i, final short f ){ writer.setChannel( this, i, f ); }
	final void setChannelDirect( final int i, final int f ){ writer.setChannel( this, i, f ); }
	final void setChannelDirect( final int i, final long f ){ writer.setChannel( this, i, f ); }
	final void setChannelDirect( final int i, final float f ){ writer.setChannel( this, i, f ); }
	final void setChannelDirect( final int i, final double f ){ writer.setChannel( this, i, f ); }
}
