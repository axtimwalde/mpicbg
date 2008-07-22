package mpicbg.image;

interface ContainerWrite< C extends Cursor >
{	
	public void set( final C c, final Object[] f );
	public void set( final C c, final byte[] f );
	public void set( final C c, final short[] f );
	public void set( final C c, final int[] f );
	public void set( final C c, final long[] f );
	public void set( final C c, final float[] f );
	public void set( final C c, final double[] f );
	
	public void setChannel( final C c, final int i, final Object f );
	public void setChannel( final C c, final int i, final byte f );
	public void setChannel( final C c, final int i, final short f );
	public void setChannel( final C c, final int i, final int f );
	public void setChannel( final C c, final int i, final long f );
	public void setChannel( final C c, final int i, final float f );
	public void setChannel( final C c, final int i, final double f );
}
