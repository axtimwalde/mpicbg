package mpicbg.image;

public abstract class ConstantCursor< P extends PixelType > extends Cursor< Container< P, ConstantCursor > >
	implements Readable, Writable, Operator
{
	final P type;

	ConstantCursor( P type )
	{
		super( null, null, null );
		this.type = type;
	}
	
	@Override
	final public boolean isInside() { return true; }
	
	final byte getByteChannelDirect( final int c) { return getByteChannel( c ); }
	final short getShortChannelDirect( final int c) { return getShortChannel( c ); }
	final int getIntChannelDirect( final int c) { return getIntChannel( c); }
	final long getLongChannelDirect( final int c) { return getLongChannel( c); }
	final float getFloatChannelDirect( final int c) { return getFloatChannel( c); }
	final double getDoubleChannelDirect( final int c) { return getDoubleChannel( c); }
	final Object getChannelDirect( final int c) { return getChannel( c); }

	final void readDirect( final Object[] a) { read( a ); }
	final void readDirect( final byte[] a) { read( a ); }
	final void readDirect( final short[] a) { read( a ); }
	final void readDirect( final int[] a) { read( a ); }
	final void readDirect( final long[] a) { read( a ); }
	final void readDirect( final float[] a ) { read( a ); }
	final void readDirect( final double[] a) { read( a ); }
	
	final void setDirect( final Object[] f ){ set( f ); }
	final void setDirect( final byte[] f ){ set( f ); }
	final void setDirect( final short[] f ){ set( f ); }
	final void setDirect( final int[] f ){ set( f ); }
	final void setDirect( final long[] f ){ set( f ); }
	final void setDirect( final float[] f ){ set( f ); }
	final void setDirect( final double[] f ){ set( f ); }
	
	final void setChannelDirect( final int i, final Object f ){ setChannel( i, f ); } 
	final void setChannelDirect( final int i, final byte f ){ setChannel( i, f ); }
	final void setChannelDirect( final int i, final short f ){ setChannel( i, f ); }
	final void setChannelDirect( final int i, final int f ){ setChannel( i, f ); }
	final void setChannelDirect( final int i, final long f ){ setChannel( i, f ); }
	final void setChannelDirect( final int i, final float f ){ setChannel( i, f ); }
	final void setChannelDirect( final int i, final double f ){ setChannel( i, f ); }
}
