package mpicbg.image;

import mpicbg.image.interpolation.Interpolator;

public class FloatStreamCursor extends Cursor implements Readable
{
	protected int i = -1;
	final protected float[] a;
	
	FloatStreamCursor( FloatStream image )
	{
		this( image, null );
	}
	
	FloatStreamCursor( FloatStream image, Interpolator ip )
	{
		super( image, ip );
		a = new float[ container.getPixelType().getNumChannels() ];
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

	
	final public void read( final Object[] c )
	{
		read( a );
		for ( int i = 0; i < c.length; ++i )
			c[ i ] = a[ i ];
	}
	final public void read( final byte[] c )
	{
		read( a );
		for ( int i = 0; i < c.length; ++i )
			c[ i ] = ( byte )a[ i ];
	}
	final public void read( final short[] c )
	{
		read( a );
		for ( int i = 0; i < c.length; ++i )
			c[ i ] = ( short )a[ i ];
	}
	final public void read( final int[] c )
	{
		read( a );
		for ( int i = 0; i < c.length; ++i )
			c[ i ] = ( int )a[ i ];
	}
	final public void read( final long[] c )
	{
		read( a );
		for ( int i = 0; i < c.length; ++i )
			c[ i ] = ( long )a[ i ];
	}
	final public void read( final double[] c )
	{
		read( a );
		for ( int i = 0; i < c.length; ++i )
			c[ i ] = ( double )a[ i ];
	}
	
	
	final public Object[] get()
	{
		Object[] c = new Object[ container.getPixelType().getNumChannels() ];
		read( c );
		return c;
	}
	final public byte[] getBytes()
	{
		byte[] c = new byte[ container.getPixelType().getNumChannels() ];
		read( c );
		return c;
	}
	final public short[] getShorts()
	{
		short[] c = new short[ container.getPixelType().getNumChannels() ];
		read( c );
		return c;
	}
	final public int[] getInts()
	{
		int[] c = new int[ container.getPixelType().getNumChannels() ];
		read( c );
		return c;
	}
	final public long[] getLongs()
	{
		long[] c = new long[ container.getPixelType().getNumChannels() ];
		read( c );
		return c;
	}
	final public double[] getDoubles()
	{
		double[] c = new double[ container.getPixelType().getNumChannels() ];
		read( c );
		return c;
	}
	
	final public Object getChannel( final int c )
	{
		return getFloatChannel( c );
	}
	final public byte getByteChannel( final int c )
	{
		return ( byte )getFloatChannel( c );
	}
	final public short getShortChannel( final int c )
	{
		return ( short )getFloatChannel( c );
	}
	final public int getIntChannel( final int c )
	{
		return ( byte )getFloatChannel( c );
	}
	final public long getLongChannel( final int c )
	{
		return ( long )getFloatChannel( c );
	}
	final public double getDoubleChannel( final int c )
	{
		return getFloatChannel( c );
	}
}
