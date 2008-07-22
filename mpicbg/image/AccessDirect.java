/**
 * 
 */
package mpicbg.image;

/**
 * @author Stephan
 *
 */
public class AccessDirect
		extends Access
{
	public void read( final Cursor c, final Object[] f ){ c.readDirect( f ); }
	public void read( final Cursor c, final byte[] f ){ c.readDirect( f ); }
	public void read( final Cursor c, final short[] f ){ c.readDirect( f ); }
	public void read( final Cursor c, final int[] f ){ c.readDirect( f ); }
	public void read( final Cursor c, final long[] f ){ c.readDirect( f ); }
	public void read( final Cursor c, final float[] f ){ c.readDirect( f ); }
	public void read( final Cursor c, final double[] f ){ c.readDirect( f ); }
	
	public Object getChannel( final Cursor c, final int i ){ return c.getChannelDirect( i ); }
	public byte getByteChannel( final Cursor c, final int i ){ return c.getByteChannelDirect( i ); }
	public short getShortChannel( final Cursor c, final int i ){ return c.getShortChannelDirect( i ); }
	public int getIntChannel( final Cursor c, final int i ){ return c.getIntChannelDirect( i ); }
	public long getLongChannel( final Cursor c, final int i ){ return c.getLongChannelDirect( i ); }
	public float getFloatChannel( final Cursor c, final int i ){ return c.getFloatChannelDirect( i ); }
	public double getDoubleChannel( final Cursor c, final int i ){ return c.getDoubleChannelDirect( i ); }
	
	public void set( final Cursor c, final Object[] f ){ c.setDirect( f ); }
	public void set( final Cursor c, final byte[] f ){ c.setDirect( f ); }
	public void set( final Cursor c, final short[] f ){ c.setDirect( f ); }
	public void set( final Cursor c, final int[] f ){ c.setDirect( f ); }
	public void set( final Cursor c, final long[] f ){ c.setDirect( f ); }
	public void set( final Cursor c, final float[] f ){ c.setDirect( f ); }
	public void set( final Cursor c, final double[] f ){ c.setDirect( f ); }
	
	public void setChannel( final Cursor c, final int i, final Object f ){ c.setChannelDirect( i, f ); }
	public void setChannel( final Cursor c, final int i, final byte f ){ c.setChannelDirect( i, f ); }
	public void setChannel( final Cursor c, final int i, final short f ){ c.setChannelDirect( i, f ); }
	public void setChannel( final Cursor c, final int i, final int f ){ c.setChannelDirect( i, f ); }
	public void setChannel( final Cursor c, final int i, final long f ){ c.setChannelDirect( i, f ); }
	public void setChannel( final Cursor c, final int i, final float f ){ c.setChannelDirect( i, f ); }
	public void setChannel( final Cursor c, final int i, final double f ){ c.setChannelDirect( i, f ); }
}
