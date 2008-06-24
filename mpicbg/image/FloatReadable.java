package mpicbg.image;

/**
 * Implements Readable for float without the actual write and
 * operation methods for float.  This is necessary to overcome the
 * limitations of the Java programming language like multiple inheritance
 * and generics for basic types which otherwise require to write the exact
 * same code ever and ever again.
 * 
 * All these methods tend to be slow because each value has to be casted to
 * float.  Take care to use compatible types.
 * 
 * @author Saalfeld and Preibisch
 *
 */
public abstract class FloatReadable extends Cursor implements Readable
{
	final protected float[] a;
	
	public FloatReadable( Container c )
	{
		super( c, null );
		a = new float[ container.getPixelType().getNumChannels() ];
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
