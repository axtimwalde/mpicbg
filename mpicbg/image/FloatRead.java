package mpicbg.image;

/**
 * @author Preibisch & Saalfeld
 *
 */
public abstract class FloatRead<C extends Cursor> implements ContainerRead<C>
{
	final protected float[] a;
	final protected Container container;

	/**
	 * Create a {@link FloatRead FloatReader} for a specific {@link Container}.
	 *
	 * @param container
	 */
	public FloatRead( final Container container )
	{
		this.container = container;
		a = new float[ container.getPixelType().getNumChannels() ];
	}

	abstract public void read( final C c, final float[] f );
	abstract public float getFloatChannel( final C c, final int i );

	final public void read( final C c, final Object[] f )
	{
		read( c, a );
		for ( int i = 0; i < f.length; ++i )
			f[ i ] = a[ i ];
	}

	final public void read( final C c, final byte[] f )
	{
		read( c, a );
		for ( int i = 0; i < f.length; ++i )
			f[ i ] = ( byte )a[ i ];
	}
	final public void read( final C c, final short[] f )
	{
		read( c, a );
		for ( int i = 0; i < f.length; ++i )
			f[ i ] = ( short )a[ i ];
	}
	final public void read( final C c, final int[] f )
	{
		read( c, a );
		for ( int i = 0; i < f.length; ++i )
			f[ i ] = ( int )a[ i ];
	}
	final public void read( final C c, final long[] f )
	{
		read( c, a );
		for ( int i = 0; i < f.length; ++i )
			f[ i ] = ( long )a[ i ];
	}
	final public void read( final C c, final double[] f )
	{
		read( c, a );
		for ( int i = 0; i < f.length; ++i )
			f[ i ] = ( double )a[ i ];
	}

	final public Object getChannel( final C c, final int i )
	{
		return getFloatChannel( c, i );
	}
	final public byte getByteChannel( final C c, final int i )
	{
		return ( byte )getFloatChannel( c, i );
	}
	final public short getShortChannel( final C c, final int i )
	{
		return ( short )getFloatChannel( c, i );
	}
	final public int getIntChannel( final C c, final int i )
	{
		return ( byte )getFloatChannel( c, i );
	}
	final public long getLongChannel( final C c, final int i )
	{
		return ( long )getFloatChannel( c, i );
	}
	final public double getDoubleChannel( final C c, final int i )
	{
		return getFloatChannel( c, i );
	}
}
