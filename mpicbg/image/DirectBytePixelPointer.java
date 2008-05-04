package mpicbg.image;

import mpicbg.image.interpolation.Interpolator;

public abstract class DirectBytePixelPointer extends DirectPixelPointer 
{
	final byte[] register;
	
	public DirectBytePixelPointer( Container pc, Interpolator ip )
	{
		super( pc, ip );
		register = new byte[ container.getPixelType().getNumChannels() ];
	}
	
	
	@Override
	public void add( PixelPointer p )
	{
		int n = container.getPixelType().getNumChannels();
		byte[] b = this.getBytes();
		byte[] a = p.getBytes();
		for ( int i = 0; i < n; ++i )
			b[ i ] += a[ i ];
		set( b );
	}

	@Override
	public void add( byte a )
	{
		int n = container.getPixelType().getNumChannels();
		byte[] b = this.getBytes();
		for ( int i = 0; i < n; ++i )
			b[ i ] += a;
		set( b );
	}

	@Override
	public void add( short a ) { add( ( byte )a ); }

	@Override
	public void add( int a ) { add( ( byte )a ); }

	@Override
	public void add( long a ) {	add( ( byte )a ); }

	@Override
	public void add( float a ) { add( ( byte )a ); }

	@Override
	public void add( double a ) { add( ( byte )a );	}

	
	@Override
	public void div( PixelPointer p )
	{
		int n = container.getPixelType().getNumChannels();
		byte[] b = this.getBytes();
		byte[] a = p.getBytes();
		for ( int i = 0; i < n; ++i )
			b[ i ] /= a[ i ];
		set( b );
	}

	@Override
	public void div( byte a )
	{
		int n = container.getPixelType().getNumChannels();
		byte[] b = this.getBytes();
		for ( int i = 0; i < n; ++i )
			b[ i ] /= a;
		set( b );
	}

	@Override
	public void div( short a ) { div( ( byte )a ); }
	
	@Override
	public void div( int a ) { div( ( byte )a ); }
	
	@Override
	public void div( long a ) { div( ( byte )a ); }
	
	@Override
	public void div( float a ) { div( ( byte )a ); }
	
	@Override
	public void div( double a ) { div( ( byte )a ); }

	
	@Override
	public void mul( PixelPointer p )
	{
		int n = container.getPixelType().getNumChannels();
		byte[] b = this.getBytes();
		byte[] a = p.getBytes();
		for ( int i = 0; i < n; ++i )
			b[ i ] *= a[ i ];
		set( b );
	}

	@Override
	public void mul( byte a )
	{
		int n = container.getPixelType().getNumChannels();
		byte[] b = this.getBytes();
		for ( int i = 0; i < n; ++i )
			b[ i ] *= a;
		set( b );
	}
	
	@Override
	public void mul( short a ) { mul( ( byte )a ); }
	
	@Override
	public void mul( int a ) { mul( ( byte )a ); }
	
	@Override
	public void mul( long a ) { mul( ( byte )a ); }
	
	@Override
	public void mul( float a ) { mul( ( byte )a ); }
	
	@Override
	public void mul( double a ) { mul( ( byte )a ); }

	
	@Override
	public void sub( PixelPointer p )
	{
		int n = container.getPixelType().getNumChannels();
		byte[] b = this.getBytes();
		byte[] a = p.getBytes();
		for ( int i = 0; i < n; ++i )
			b[ i ] -= a[ i ];
		set( b );
	}

	@Override
	public void sub( byte a )
	{
		int n = container.getPixelType().getNumChannels();
		byte[] b = this.getBytes();
		for ( int i = 0; i < n; ++i )
			b[ i ] -= a;
		set( b );
	}

	@Override
	public void sub( short a ) { mul( ( byte )a ); }

	@Override
	public void sub( int a ) { mul( ( byte )a ); }

	@Override
	public void sub( long a ) { mul( ( byte )a ); }

	@Override
	public void sub( float a ) { mul( ( byte )a ); }

	@Override
	public void sub( double a ) { mul( ( byte )a ); }
}
