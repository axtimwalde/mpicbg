package mpicbg.image;

interface ContainerRead< C extends Cursor >
{	
	public void read( final C c, final Object[] f );
	public void read( final C c, final byte[] f );
	public void read( final C c, final short[] f );
	public void read( final C c, final int[] f );
	public void read( final C c, final long[] f );
	public void read( final C c, final float[] f );
	public void read( final C c, final double[] f );
	
	public Object getChannel( final C c, final int i );
	public byte getByteChannel( final C c, final int i );
	public short getShortChannel( final C c, final int i );
	public int getIntChannel( final C c, final int i );
	public long getLongChannel( final C c, final int i );
	public float getFloatChannel( final C c, final int i );
	public double getDoubleChannel( final C c, final int i );
}
