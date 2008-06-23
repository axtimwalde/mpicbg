package mpicbg.image;

import java.util.NoSuchElementException;

public class FloatStreamIterator extends FloatWritable implements Iteratable
{
	protected int i = 0;
	final int step;
	
	private final float[] data;
	
	FloatStreamIterator( FloatStream stream )
	{
		super( stream );
		step = stream.getPixelType().getNumChannels();
		data = stream.data;
	}

	final public boolean hasNext(){ return i >= data.length - step; }
	final public boolean hasPrev(){ return i >= step; }
	
	public void set( float[] c )
	{
		synchronized ( data ){ System.arraycopy( c, 0, data, i, c.length ); }
	}
	public void setChannel( float v, int c )
	{
		synchronized ( data ){ data[ i + c ] = v; }
	}
	
	
	public void add( Readable c )
	{
		c.read( a );
		synchronized ( data )
		{
			for ( int j = 0; j < a.length; ++j )
				data[ i + j ] += a[ j ];
		}
	}
	public void add( float c )
	{
		synchronized ( data )
		{
			for ( int j = 0; j < step; ++j )
				data[ i + j ] += c;
		}
	}
	
	public void sub( Readable c )
	{
		c.read( a );
		synchronized ( data )
		{
			for ( int j = 0; j < a.length; ++j )
				data[ i + j ] -= a[ j ];
		}
	}
	public void sub( float c )
	{
		synchronized ( data )
		{
			for ( int j = 0; j < step; ++j )
				data[ i + j ] -= c;
		}
	}
	
	public void mul( Readable c )
	{
		c.read( a );
		synchronized ( data )
		{
			for ( int j = 0; j < a.length; ++j )
				data[ i + j ] *= a[ j ];
		}
	}
	public void mul( float c )
	{
		synchronized ( data )
		{
			for ( int j = 0; j < step; ++j )
				data[ i + j ] *= c;
		}
	}
	
	public void div( Readable c )
	{
		c.read( a );
		synchronized ( data )
		{
			for ( int j = 0; j < a.length; ++j )
				data[ i + j ] /= a[ j ];
		}
	}
	public void div( float c )
	{
		synchronized ( data )
		{
			for ( int j = 0; j < step; ++j )
				data[ i + j ] /= c;
		}
	}
	
	public void next() throws NoSuchElementException{ i += step; }
	public void prev() throws NoSuchElementException{ i -= step; }

}
