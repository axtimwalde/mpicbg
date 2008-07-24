package mpicbg.image;

import mpicbg.image.interpolation.Interpolator;

public abstract class StreamCursor
		extends Cursor
{
	final Stream container;

	/**
	 * Constructs the cursor.
	 *
	 * @param c - The Container to work on
	 * @param ip - The Interpolator - still unused
	 * @param as - A prototype for the AccessStrategy which is cloned in order to give this cursor as
	 * final instance to the AccessStrategy to be as fast as possible.
	 */
	public StreamCursor( Stream container, Interpolator ip, final Access as )
	{
		super( container, ip, as );
		this.container = container;
	}

	abstract public int getStreamIndex();

	final byte getByteChannelDirect( final int c) { return container.getReader().getByteChannel( this, c ); }
	final short getShortChannelDirect( final int c) { return container.getReader().getShortChannel( this, c ); }
	final int getIntChannelDirect( final int c) { return container.getReader().getIntChannel( this, c); }
	final long getLongChannelDirect( final int c) { return container.getReader().getLongChannel( this, c); }
	final float getFloatChannelDirect( final int c) { return container.getReader().getFloatChannel( this, c); }
	final double getDoubleChannelDirect( final int c) { return container.getReader().getDoubleChannel( this, c); }
	final Object getChannelDirect( final int c) { return container.getReader().getChannel( this, c); }

	final void readDirect( final Object[] a) { container.getReader().read( this, a); }
	final void readDirect( final byte[] a) { container.getReader().read( this, a); }
	final void readDirect( final short[] a) { container.getReader().read( this, a); }
	final void readDirect( final int[] a) { container.getReader().read( this, a); }
	final void readDirect( final long[] a) { container.getReader().read( this, a); }
	final void readDirect( final float[] a ) { container.getReader().read( this, a ); }
	final void readDirect( final double[] a) { container.getReader().read( this, a); }

	final void setDirect( final Object[] f ){ container.getWriter().set( this, f ); }
	final void setDirect( final byte[] f ){ container.getWriter().set( this, f ); }
	final void setDirect( final short[] f ){ container.getWriter().set( this, f ); }
	final void setDirect( final int[] f ){ container.getWriter().set( this, f ); }
	final void setDirect( final long[] f ){ container.getWriter().set( this, f ); }
	final void setDirect( final float[] f ){ container.getWriter().set( this, f ); }
	final void setDirect( final double[] f ){ container.getWriter().set( this, f ); }

	final void setChannelDirect( final int i, final Object f ){ container.getWriter().setChannel( this, i, f ); }
	final void setChannelDirect( final int i, final byte f ){ container.getWriter().setChannel( this, i, f ); }
	final void setChannelDirect( final int i, final short f ){ container.getWriter().setChannel( this, i, f ); }
	final void setChannelDirect( final int i, final int f ){ container.getWriter().setChannel( this, i, f ); }
	final void setChannelDirect( final int i, final long f ){ container.getWriter().setChannel( this, i, f ); }
	final void setChannelDirect( final int i, final float f ){ container.getWriter().setChannel( this, i, f ); }
	final void setChannelDirect( final int i, final double f ){ container.getWriter().setChannel( this, i, f ); }
}
