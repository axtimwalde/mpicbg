package mpicbg.image;

import mpicbg.image.interpolation.Interpolator;

public abstract class DirectIntPixelPointer extends DirectPixelPointer 
{
	public DirectIntPixelPointer( PixelContainer pc, Interpolator ip )
	{
		super( pc, ip );
	}
	
	
	@Override
	public void add( PixelPointer p )
	{
		int n = container.getPixelType().getNumChannels();
		int[] b = this.getInts();
		int[] a = p.getInts();
		for ( int i = 0; i < n; ++i )
			b[ i ] += a[ i ];
		set( b );
	}

	@Override
	public void add( byte a ) { add( ( int )a ); }

	@Override
	public void add( short a ) { add( ( int )a ); }

	@Override
	public void add( int a )
	{
		int n = container.getPixelType().getNumChannels();
		int[] b = this.getInts();
		for ( int i = 0; i < n; ++i )
			b[ i ] += a;
		set( b );
	}

	@Override
	public void add( long a ) {	add( ( int )a ); }

	@Override
	public void add( float a ) { add( ( int )a ); }

	@Override
	public void add( double a ) { add( ( int )a );	}

	
	@Override
	public void div( PixelPointer p )
	{
		int n = container.getPixelType().getNumChannels();
		int[] b = this.getInts();
		int[] a = p.getInts();
		for ( int i = 0; i < n; ++i )
			b[ i ] /= a[ i ];
		set( b );
	}

	@Override
	public void div( byte a ) { div( ( int )a ); }

	@Override
	public void div( short a ) { div( ( int )a ); }

	@Override
	public void div( int a )
	{
		int n = container.getPixelType().getNumChannels();
		int[] b = this.getInts();
		for ( int i = 0; i < n; ++i )
			b[ i ] /= a;
		set( b );
	}

	@Override
	public void div( long a ) {	div( ( int )a ); }

	@Override
	public void div( float a ) { div( ( int )a ); }

	@Override
	public void div( double a ) { div( ( int )a );	}

	
	@Override
	public void mul( PixelPointer p )
	{
		int n = container.getPixelType().getNumChannels();
		int[] b = this.getInts();
		int[] a = p.getInts();
		for ( int i = 0; i < n; ++i )
			b[ i ] *= a[ i ];
		set( b );
	}

	@Override
	public void mul( byte a ) { mul( ( int )a ); }

	@Override
	public void mul( short a ) { mul( ( int )a ); }

	@Override
	public void mul( int a )
	{
		int n = container.getPixelType().getNumChannels();
		int[] b = this.getInts();
		for ( int i = 0; i < n; ++i )
			b[ i ] *= a;
		set( b );
	}

	@Override
	public void mul( long a ) {	mul( ( int )a ); }

	@Override
	public void mul( float a ) { mul( ( int )a ); }

	@Override
	public void mul( double a ) { mul( ( int )a );	}

	
	@Override
	public void sub( PixelPointer p )
	{
		int n = container.getPixelType().getNumChannels();
		int[] b = this.getInts();
		int[] a = p.getInts();
		for ( int i = 0; i < n; ++i )
			b[ i ] -= a[ i ];
		set( b );
	}

	@Override
	public void sub( byte a ) { sub( ( int )a ); }

	@Override
	public void sub( short a ) { sub( ( int )a ); }

	@Override
	public void sub( int a )
	{
		int n = container.getPixelType().getNumChannels();
		int[] b = this.getInts();
		for ( int i = 0; i < n; ++i )
			b[ i ] -= a;
		set( b );
	}

	@Override
	public void sub( long a ) {	sub( ( int )a ); }

	@Override
	public void sub( float a ) { sub( ( int )a ); }

	@Override
	public void sub( double a ) { sub( ( int )a );	}
}
