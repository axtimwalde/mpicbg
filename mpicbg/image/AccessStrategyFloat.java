package mpicbg.image;

/**
 * @author Preibisch & Saalfeld
 *
 */
public abstract class AccessStrategyFloat extends AccessStrategy 
{	
	final protected float[] a;
	
	/**
	 * Constructs a AccessStrategy for Float
	 * 
	 * @param c - The image container
	 */
	
	public AccessStrategyFloat( final Container container, final Cursor cursor )
	{
		super( (Container) container, cursor );
		a = new float[ this.container.getNumDim() ];
	}

	public AccessStrategyFloat( final Container container )
	{
		this( container, null );
	}
	
	//
	// "native" get method
	// 
	final public float[] getFloats()
	{
		final float[] f = new float[ container.getNumDim() ];
		read( f );
		return f;
	}
	
	//
	// "native" operator methods
	// 
	final public void add( final Readable c )
	{
		for ( int j = 0; j < a.length; ++j )
			setChannel(getFloatChannel(j) + c.getFloatChannel(j), j);
	}
	final public void add( final float c )
	{
		for ( int j = 0; j < a.length; ++j )
			setChannel(getFloatChannel(j) + c, j);
	}
	
	final public void sub( final Readable c )
	{
		for ( int j = 0; j < a.length; ++j )
			setChannel(getFloatChannel(j) - c.getFloatChannel(j), j);
	}
	final public void sub( final float c )
	{
		for ( int j = 0; j < a.length; ++j )
			setChannel(getFloatChannel(j) - c, j);
	}
	
	final public void mul( final Readable c )
	{
		for ( int j = 0; j < a.length; ++j )
			setChannel(getFloatChannel(j) * c.getFloatChannel(j), j);
	}
	final public void mul( final float c )
	{
		for ( int j = 0; j < a.length; ++j )
			setChannel(getFloatChannel(j) * c, j);
	}
	
	final public void div( final Readable c )
	{
		for ( int j = 0; j < a.length; ++j )
			setChannel(getFloatChannel(j) / c.getFloatChannel(j), j);
	}	
	final public void div( final float c )
	{
		for ( int j = 0; j < a.length; ++j )
			setChannel(getFloatChannel(j) / c, j);
	}	
	
	//
	// Writable Methods
	//
	final public void add( final byte c ){ add( ( float )c ); }
	final public void add( final short c ) { add( ( float )c ); }
	final public void add( final int c ) { add( ( float )c ); }
	final public void add( final long c ) {	add( ( float )c ); }
	final public void add( final double c ) { add( ( float )c );	}

	final public void sub( final byte c ){ sub( ( float )c ); }
	final public void sub( final short c ) { sub( ( float )c ); }
	final public void sub( final int c ) { sub( ( float )c ); }
	final public void sub( final long c ) {	sub( ( float )c ); }
	final public void sub( final double c ) { sub( ( float )c );	}

	final public void mul( final byte c ){ mul( ( float )c ); }
	final public void mul( final short c ) { mul( ( float )c ); }
	final public void mul( final int c ) { mul( ( float )c ); }
	final public void mul( final long c ) {	mul( ( float )c ); }
	final public void mul( final double c ) { mul( ( float )c );	}

	final public void div( final byte c ){ div( ( float )c ); }
	final public void div( final short c ) { div( ( float )c ); }
	final public void div( final int c ) { div( ( float )c ); }
	final public void div( final long c ) {	div( ( float )c ); }
	final public void div( final double c ) { div( ( float )c );	}
	
	final public void set( final Object[] a )
	{
		for ( int i = 0; i < a.length; ++i )
			setChannel( ( Float )a[ i ], i );
	}
	final public void set( final byte[] a )
	{
		for ( int i = 0; i < a.length; ++i )
			setChannel( ( float )a[ i ], i );
	}
	final public void set( final short[] a )
	{
		for ( int i = 0; i < a.length; ++i )
			setChannel( ( float )a[ i ], i );
	}
	final public void set( final int[] a )
	{
		for ( int i = 0; i < a.length; ++i )
			setChannel( ( float )a[ i ], i );
	}
	final public void set( final long[] a )
	{
		for ( int i = 0; i < a.length; ++i )
			setChannel( ( float )a[ i ], i );
	}
	final public void set( final double[] a )
	{
		for ( int i = 0; i < a.length; ++i )
			setChannel( ( float )a[ i ], i );
	}
	
	/**
	 * Set one channel
	 * @param v value
	 * @param c channel
	 */
	final public void setChannel( final Object v, final int c ){ setChannel( ( Float )v, c ); }
	final public void setChannel( final byte v, final int c ){ setChannel( ( float )v, c ); }
	final public void setChannel( final short v, final int c ){ setChannel( ( float )v, c ); }
	final public void setChannel( final int v, final int c ){ setChannel( ( float )v, c ); }
	final public void setChannel( final long v, final int c ){ setChannel( ( float )v, c ); }
	final public void setChannel( final double v, final int c ){ setChannel( ( float )v, c ); }
	//
	// Readable Methods
	//
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
