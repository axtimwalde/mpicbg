package mpicbg.image;

public abstract class FloatWritable extends Cursor implements Operator, Writable
{
	final protected float[] a;
	
	public FloatWritable( Container c )
	{
		super( c, null );
		a = new float[ container.getPixelType().getNumChannels() ];
	}
	
	public void add( byte c ){ add( ( float )c ); }
	public void add( short c ) { add( ( float )c ); }
	public void add( int c ) { add( ( float )c ); }
	public void add( long c ) {	add( ( float )c ); }
	public void add( double c ) { add( ( float )c );	}

	public void sub( byte c ){ sub( ( byte )c ); }
	public void sub( short c ) { sub( ( byte )c ); }
	public void sub( int c ) { sub( ( byte )c ); }
	public void sub( long c ) {	sub( ( byte )c ); }
	public void sub( double c ) { sub( ( byte )c );	}

	public void mul( byte c ){ mul( ( byte )c ); }
	public void mul( short c ) { mul( ( byte )c ); }
	public void mul( int c ) { mul( ( byte )c ); }
	public void mul( long c ) {	mul( ( byte )c ); }
	public void mul( double c ) { mul( ( byte )c );	}

	public void div( byte c ){ div( ( byte )c ); }
	public void div( short c ) { div( ( byte )c ); }
	public void div( int c ) { div( ( byte )c ); }
	public void div( long c ) {	div( ( byte )c ); }
	public void div( double c ) { div( ( byte )c );	}
	
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
