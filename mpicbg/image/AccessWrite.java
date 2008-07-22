package mpicbg.image;

interface AccessWrite
{	
	public void set( final Cursor c, final Object[] f );
	public void set( final Cursor c, final byte[] f );
	public void set( final Cursor c, final short[] f );
	public void set( final Cursor c, final int[] f );
	public void set( final Cursor c, final long[] f );
	public void set( final Cursor c, final float[] f );
	public void set( final Cursor c, final double[] f );
	
	public void setChannel( final Cursor c, final int i, final Object f );
	public void setChannel( final Cursor c, final int i, final byte f );
	public void setChannel( final Cursor c, final int i, final short f );
	public void setChannel( final Cursor c, final int i, final int f );
	public void setChannel( final Cursor c, final int i, final long f );
	public void setChannel( final Cursor c, final int i, final float f );
	public void setChannel( final Cursor c, final int i, final double f );
}
