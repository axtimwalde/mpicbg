package mpicbg.image;

interface AccessRead
{	
	public void read( final Cursor c, final Object[] f );
	public void read( final Cursor c, final byte[] f );
	public void read( final Cursor c, final short[] f );
	public void read( final Cursor c, final int[] f );
	public void read( final Cursor c, final long[] f );
	public void read( final Cursor c, final float[] f );
	public void read( final Cursor c, final double[] f );
	
	public Object getChannel( final Cursor c, final int i );
	public byte getByteChannel( final Cursor c, final int i );
	public short getShortChannel( final Cursor c, final int i );
	public int getIntChannel( final Cursor c, final int i );
	public long getLongChannel( final Cursor c, final int i );
	public float getFloatChannel( final Cursor c, final int i );
	public double getDoubleChannel( final Cursor c, final int i );
}
