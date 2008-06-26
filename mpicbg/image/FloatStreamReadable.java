package mpicbg.image;

abstract public class FloatStreamReadable extends FloatReadable
{
	protected int i = 0;
	final protected float[] a;
	final protected float[] data;
	final protected int numChannels;
	
	FloatStreamReadable( FloatStream stream )
	{
		super( stream );
		data = stream.data;
		a = new float[ container.getPixelType().getNumChannels() ];
		numChannels = stream.getPixelType().getNumChannels();
	}
	
	final public void read( final float[] c )
	{
		System.arraycopy( ( ( FloatStream )container ).data, i, c, 0, c.length );
	}	
	final public float getFloatChannel( final int c )
	{
		return ( ( FloatStream )container ).data[ i + c ];
	}
	final public float[] getFloats()
	{
		final float[] f = new float[ container.getPixelType().getNumChannels() ];
		read( f );
		return f;
	}
}
