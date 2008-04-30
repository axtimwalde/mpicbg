package mpicbg.image;

import mpicbg.image.interpolation.Interpolator;

public abstract class DirectFloatPixelPointer extends DirectPixelPointer 
{
	public DirectFloatPixelPointer( PixelContainer pc, Interpolator ip )
	{
		super( pc, ip );
	}
	
	
	@Override
	public void add( PixelPointer p )
	{
		int n = container.getPixelType().getNumberOfChannels();
		float[] b = this.getFloats();
		float[] a = p.getFloats();
		for ( int i = 0; i < n; ++i )
			b[ i ] += a[ i ];
		setFloats( b );
	}

	@Override
	public void add( byte a ) { add( ( float )a ); }

	@Override
	public void add( short a ) { add( ( float )a ); }

	@Override
	public void add( int a ) {	add( ( float )a ); }

	@Override
	public void add( long a ) { add( ( float )a ); }

	@Override
	public void add( float a )
	{
		int n = container.getPixelType().getNumberOfChannels();
		float[] b = this.getFloats();
		for ( int i = 0; i < n; ++i )
			b[ i ] += a;
		setFloats( b );
	}

	@Override
	public void add( double a ) { add( ( float )a );	}

	
	@Override
	public void div( PixelPointer p )
	{
		int n = container.getPixelType().getNumberOfChannels();
		float[] b = this.getFloats();
		float[] a = p.getFloats();
		for ( int i = 0; i < n; ++i )
			b[ i ] /= a[ i ];
		setFloats( b );
	}

	@Override
	public void div( byte a ) { div( ( float )a ); }

	@Override
	public void div( short a ) { div( ( float )a ); }

	@Override
	public void div( int a ) {	div( ( float )a ); }

	@Override
	public void div( long a ) { div( ( float )a ); }

	@Override
	public void div( float a )
	{
		int n = container.getPixelType().getNumberOfChannels();
		float[] b = this.getFloats();
		for ( int i = 0; i < n; ++i )
			b[ i ] /= a;
		setFloats( b );
	}

	@Override
	public void div( double a ) { div( ( float )a );	}

	
	@Override
	public void mul( PixelPointer p )
	{
		int n = container.getPixelType().getNumberOfChannels();
		float[] b = this.getFloats();
		float[] a = p.getFloats();
		for ( int i = 0; i < n; ++i )
			b[ i ] *= a[ i ];
		setFloats( b );
	}

	@Override
	public void mul( byte a ) { mul( ( float )a ); }

	@Override
	public void mul( short a ) { mul( ( float )a ); }

	@Override
	public void mul( int a ) {	mul( ( float )a ); }

	@Override
	public void mul( long a ) { mul( ( float )a ); }

	@Override
	public void mul( float a )
	{
		int n = container.getPixelType().getNumberOfChannels();
		float[] b = this.getFloats();
		for ( int i = 0; i < n; ++i )
			b[ i ] *= a;
		setFloats( b );
	}

	@Override
	public void mul( double a ) { mul( ( float )a );	}

	
	@Override
	public void sub( PixelPointer p )
	{
		int n = container.getPixelType().getNumberOfChannels();
		float[] b = this.getFloats();
		float[] a = p.getFloats();
		for ( int i = 0; i < n; ++i )
			b[ i ] -= a[ i ];
		setFloats( b );
	}

	@Override
	public void sub( byte a ) { sub( ( float )a ); }

	@Override
	public void sub( short a ) { sub( ( float )a ); }

	@Override
	public void sub( int a ) {	sub( ( float )a ); }

	@Override
	public void sub( long a ) { sub( ( float )a ); }

	@Override
	public void sub( float a )
	{
		int n = container.getPixelType().getNumberOfChannels();
		float[] b = this.getFloats();
		for ( int i = 0; i < n; ++i )
			b[ i ] -= a;
		setFloats( b );
	}

	@Override
	public void sub( double a ) { sub( ( float )a );	}
}
