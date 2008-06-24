package mpicbg.image;

/**
 * Implements Readable, Writable and Operator for float without the actual
 * write and operation methods for float.  This is necessary to overcome the
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
public abstract class FloatReadableAndWritable extends Cursor implements Readable, Writable, Operator
{
	final protected float[] a;
	
	public FloatReadableAndWritable( Container c )
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
	
	public void add( byte c ){ add( ( float )c ); }
	public void add( short c ) { add( ( float )c ); }
	public void add( int c ) { add( ( float )c ); }
	public void add( long c ) {	add( ( float )c ); }
	public void add( double c ) { add( ( float )c );	}

	public void sub( byte c ){ sub( ( float )c ); }
	public void sub( short c ) { sub( ( float )c ); }
	public void sub( int c ) { sub( ( float )c ); }
	public void sub( long c ) {	sub( ( float )c ); }
	public void sub( double c ) { sub( ( float )c );	}

	public void mul( byte c ){ mul( ( float )c ); }
	public void mul( short c ) { mul( ( float )c ); }
	public void mul( int c ) { mul( ( float )c ); }
	public void mul( long c ) {	mul( ( float )c ); }
	public void mul( double c ) { mul( ( float )c );	}

	public void div( byte c ){ div( ( float )c ); }
	public void div( short c ) { div( ( float )c ); }
	public void div( int c ) { div( ( float )c ); }
	public void div( long c ) {	div( ( float )c ); }
	public void div( double c ) { div( ( float )c );	}
	
	public void set( Object[] a )
	{
		for ( int i = 0; i < a.length; ++i )
			setChannel( ( Float )a[ i ], i );
	}
	public void set( byte[] a )
	{
		for ( int i = 0; i < a.length; ++i )
			setChannel( ( float )a[ i ], i );
	}
	public void set( short[] a )
	{
		for ( int i = 0; i < a.length; ++i )
			setChannel( ( float )a[ i ], i );
	}
	public void set( int[] a )
	{
		for ( int i = 0; i < a.length; ++i )
			setChannel( ( float )a[ i ], i );
	}
	public void set( long[] a )
	{
		for ( int i = 0; i < a.length; ++i )
			setChannel( ( float )a[ i ], i );
	}
	public void set( double[] a )
	{
		for ( int i = 0; i < a.length; ++i )
			setChannel( ( float )a[ i ], i );
	}
	
	/**
	 * Set one channel
	 * @param v value
	 * @param c channel
	 */
	public void setChannel( Object v, int c ){ setChannel( ( Float )v, c ); }
	public void setChannel( byte v, int c ){ setChannel( ( float )v, c ); }
	public void setChannel( short v, int c ){ setChannel( ( float )v, c ); }
	public void setChannel( int v, int c ){ setChannel( ( float )v, c ); }
	public void setChannel( long v, int c ){ setChannel( ( float )v, c ); }
	public void setChannel( double v, int c ){ setChannel( ( float )v, c ); }

}
