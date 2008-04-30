package mpicbg.image;

import mpicbg.image.interpolation.Interpolator;

public abstract class DirectLongPixelPointer extends DirectPixelPointer 
{
	public DirectLongPixelPointer( PixelContainer pc, Interpolator ip )
	{
		super( pc, ip );
	}
	
	
	@Override
	public void add( PixelPointer p )
	{
		int n = container.getPixelType().getNumChannels();
		long[] b = this.getLongs();
		long[] a = p.getLongs();
		for ( int i = 0; i < n; ++i )
			b[ i ] += a[ i ];
		set( b );
	}

	@Override
	public void add( byte a ) { add( ( long )a ); }

	@Override
	public void add( short a ) { add( ( long )a ); }

	@Override
	public void add( int a ) {	add( ( long )a ); }

	@Override
	public void add( long a )
	{
		int n = container.getPixelType().getNumChannels();
		long[] b = this.getLongs();
		for ( int i = 0; i < n; ++i )
			b[ i ] += a;
		set( b );
	}

		@Override
	public void add( float a ) { add( ( long )a ); }

	@Override
	public void add( double a ) { add( ( long )a );	}

	
	@Override
	public void div( PixelPointer p )
	{
		int n = container.getPixelType().getNumChannels();
		long[] b = this.getLongs();
		long[] a = p.getLongs();
		for ( int i = 0; i < n; ++i )
			b[ i ] /= a[ i ];
		set( b );
	}

	@Override
	public void div( byte a ) { div( ( long )a ); }

	@Override
	public void div( short a ) { div( ( long )a ); }

	@Override
	public void div( int a ) {	div( ( long )a ); }

	@Override
	public void div( long a )
	{
		int n = container.getPixelType().getNumChannels();
		long[] b = this.getLongs();
		for ( int i = 0; i < n; ++i )
			b[ i ] /= a;
		set( b );
	}

		@Override
	public void div( float a ) { div( ( long )a ); }

	@Override
	public void div( double a ) { div( ( long )a );	}

	
	@Override
	public void mul( PixelPointer p )
	{
		int n = container.getPixelType().getNumChannels();
		long[] b = this.getLongs();
		long[] a = p.getLongs();
		for ( int i = 0; i < n; ++i )
			b[ i ] *= a[ i ];
		set( b );
	}

	@Override
	public void mul( byte a ) { mul( ( long )a ); }

	@Override
	public void mul( short a ) { mul( ( long )a ); }

	@Override
	public void mul( int a ) {	mul( ( long )a ); }

	@Override
	public void mul( long a )
	{
		int n = container.getPixelType().getNumChannels();
		long[] b = this.getLongs();
		for ( int i = 0; i < n; ++i )
			b[ i ] *= a;
		set( b );
	}

		@Override
	public void mul( float a ) { mul( ( long )a ); }

	@Override
	public void mul( double a ) { mul( ( long )a );	}

	
	@Override
	public void sub( PixelPointer p )
	{
		int n = container.getPixelType().getNumChannels();
		long[] b = this.getLongs();
		long[] a = p.getLongs();
		for ( int i = 0; i < n; ++i )
			b[ i ] -= a[ i ];
		set( b );
	}

	@Override
	public void sub( byte a ) { sub( ( long )a ); }

	@Override
	public void sub( short a ) { sub( ( long )a ); }

	@Override
	public void sub( int a ) {	sub( ( long )a ); }

	@Override
	public void sub( long a )
	{
		int n = container.getPixelType().getNumChannels();
		long[] b = this.getLongs();
		for ( int i = 0; i < n; ++i )
			b[ i ] -= a;
		set( b );
	}

		@Override
	public void sub( float a ) { sub( ( long )a ); }

	@Override
	public void sub( double a ) { sub( ( long )a );	}
}
