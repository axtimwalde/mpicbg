package mpicbg.image;

import mpicbg.image.interpolation.Interpolator;

public abstract class DirectShortPixelPointer extends DirectPixelPointer implements DirectRandomAccessible
{
	public DirectShortPixelPointer( Container pc, Interpolator ip )
	{
		super( pc, ip );
	}
	
	
	@Override
	public void add( PixelPointer p )
	{
		int n = container.getPixelType().getNumChannels();
		short[] b = this.getShorts();
		short[] a = p.getShorts();
		for ( int i = 0; i < n; ++i )
			b[ i ] += a[ i ];
		set( b );
	}

	@Override
	public void add( byte a ) { add( ( short )a ); }

	@Override
	public void add( short a )
	{
		int n = container.getPixelType().getNumChannels();
		short[] b = this.getShorts();
		for ( int i = 0; i < n; ++i )
			b[ i ] += a;
		set( b );
	}

	@Override
	public void add( int a ) { add( ( short )a ); }

	@Override
	public void add( long a ) {	add( ( short )a ); }

	@Override
	public void add( float a ) { add( ( short )a ); }

	@Override
	public void add( double a ) { add( ( short )a );	}

	
	@Override
	public void div( PixelPointer p )
	{
		int n = container.getPixelType().getNumChannels();
		short[] b = this.getShorts();
		short[] a = p.getShorts();
		for ( int i = 0; i < n; ++i )
			b[ i ] /= a[ i ];
		set( b );
	}

	@Override
	public void div( byte a ) { div( ( short )a ); }

	@Override
	public void div( short a )
	{
		int n = container.getPixelType().getNumChannels();
		short[] b = this.getShorts();
		for ( int i = 0; i < n; ++i )
			b[ i ] /= a;
		set( b );
	}

	@Override
	public void div( int a ) { div( ( short )a ); }

	@Override
	public void div( long a ) {	div( ( short )a ); }

	@Override
	public void div( float a ) { div( ( short )a ); }

	@Override
	public void div( double a ) { div( ( short )a );	}

	
	@Override
	public void mul( PixelPointer p )
	{
		int n = container.getPixelType().getNumChannels();
		short[] b = this.getShorts();
		short[] a = p.getShorts();
		for ( int i = 0; i < n; ++i )
			b[ i ] *= a[ i ];
		set( b );
	}

	@Override
	public void mul( byte a ) { mul( ( short )a ); }

	@Override
	public void mul( short a )
	{
		int n = container.getPixelType().getNumChannels();
		short[] b = this.getShorts();
		for ( int i = 0; i < n; ++i )
			b[ i ] *= a;
		set( b );
	}

	@Override
	public void mul( int a ) { mul( ( short )a ); }

	@Override
	public void mul( long a ) {	mul( ( short )a ); }

	@Override
	public void mul( float a ) { mul( ( short )a ); }

	@Override
	public void mul( double a ) { mul( ( short )a );	}

	
	@Override
	public void sub( PixelPointer p )
	{
		int n = container.getPixelType().getNumChannels();
		short[] b = this.getShorts();
		short[] a = p.getShorts();
		for ( int i = 0; i < n; ++i )
			b[ i ] -= a[ i ];
		set( b );
	}

	@Override
	public void sub( byte a ) { sub( ( short )a ); }

	@Override
	public void sub( short a )
	{
		int n = container.getPixelType().getNumChannels();
		short[] b = this.getShorts();
		for ( int i = 0; i < n; ++i )
			b[ i ] -= a;
		set( b );
	}

	@Override
	public void sub( int a ) { sub( ( short )a ); }

	@Override
	public void sub( long a ) {	sub( ( short )a ); }

	@Override
	public void sub( float a ) { sub( ( short )a ); }

	@Override
	public void sub( double a ) { sub( ( short )a );	}
}
