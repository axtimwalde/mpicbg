package mpicbg.image;

import mpicbg.image.interpolation.Interpolator;

public abstract class DirectDoublePixelPointer extends DirectPixelPointer 
{
	public DirectDoublePixelPointer( Container pc, Interpolator ip )
	{
		super( pc, ip );
	}
	
	
	@Override
	public void add( PixelPointer p )
	{
		int n = container.getPixelType().getNumChannels();
		double[] b = this.getDoubles();
		double[] a = p.getDoubles();
		for ( int i = 0; i < n; ++i )
			b[ i ] += a[ i ];
		set( b );
	}

	@Override
	public void add( byte a ) { add( ( double )a ); }

	@Override
	public void add( short a ) { add( ( double )a ); }

	@Override
	public void add( int a ) {	add( ( double )a ); }

	@Override
	public void add( long a ) { add( ( double )a ); }

	@Override
	public void add( float a )
	{
		int n = container.getPixelType().getNumChannels();
		double[] b = this.getDoubles();
		for ( int i = 0; i < n; ++i )
			b[ i ] += a;
		set( b );
	}

	@Override
	public void add( double a ) { add( ( double )a );	}

	
	@Override
	public void div( PixelPointer p )
	{
		int n = container.getPixelType().getNumChannels();
		double[] b = this.getDoubles();
		double[] a = p.getDoubles();
		for ( int i = 0; i < n; ++i )
			b[ i ] /= a[ i ];
		set( b );
	}

	@Override
	public void div( byte a ) { div( ( double )a ); }

	@Override
	public void div( short a ) { div( ( double )a ); }

	@Override
	public void div( int a ) {	div( ( double )a ); }

	@Override
	public void div( long a ) { div( ( double )a ); }

	@Override
	public void div( float a )
	{
		int n = container.getPixelType().getNumChannels();
		double[] b = this.getDoubles();
		for ( int i = 0; i < n; ++i )
			b[ i ] /= a;
		set( b );
	}

	@Override
	public void div( double a ) { div( ( double )a );	}

	
	@Override
	public void mul( PixelPointer p )
	{
		int n = container.getPixelType().getNumChannels();
		double[] b = this.getDoubles();
		double[] a = p.getDoubles();
		for ( int i = 0; i < n; ++i )
			b[ i ] *= a[ i ];
		set( b );
	}

	@Override
	public void mul( byte a ) { mul( ( double )a ); }

	@Override
	public void mul( short a ) { mul( ( double )a ); }

	@Override
	public void mul( int a ) {	mul( ( double )a ); }

	@Override
	public void mul( long a ) { mul( ( double )a ); }

	@Override
	public void mul( float a )
	{
		int n = container.getPixelType().getNumChannels();
		double[] b = this.getDoubles();
		for ( int i = 0; i < n; ++i )
			b[ i ] *= a;
		set( b );
	}

	@Override
	public void mul( double a ) { mul( ( double )a );	}

	
	@Override
	public void sub( PixelPointer p )
	{
		int n = container.getPixelType().getNumChannels();
		double[] b = this.getDoubles();
		double[] a = p.getDoubles();
		for ( int i = 0; i < n; ++i )
			b[ i ] -= a[ i ];
		set( b );
	}

	@Override
	public void sub( byte a ) { sub( ( double )a ); }

	@Override
	public void sub( short a ) { sub( ( double )a ); }

	@Override
	public void sub( int a ) {	sub( ( double )a ); }

	@Override
	public void sub( long a ) { sub( ( double )a ); }

	@Override
	public void sub( float a )
	{
		int n = container.getPixelType().getNumChannels();
		double[] b = this.getDoubles();
		for ( int i = 0; i < n; ++i )
			b[ i ] -= a;
		set( b );
	}

	@Override
	public void sub( double a ) { sub( ( double )a );	}
}
