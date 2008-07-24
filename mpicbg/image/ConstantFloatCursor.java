package mpicbg.image;

public class ConstantFloatCursor extends ConstantCursor
{
	final float[] data;
	final float[] a;

	public ConstantFloatCursor( PixelType type )
	{
		super( type );
		data = new float[ type.getNumChannels() ];
		a = new float[ type.getNumChannels() ];
	}

	public ConstantFloatCursor( PixelType type, float[] init )
	{
		this( type );
		for ( int j = 0; j < init.length; ++j )
			data[ j ] = init[ j ];
	}

	//
	// "native" operator methods
	//
	public void add( final Readable c )
	{
		for ( int j = 0; j < a.length; ++j )
			setChannel(j, getFloatChannel(j) + c.getFloatChannel(j));
	}
	public void add( final float c )
	{
		for ( int j = 0; j < a.length; ++j )
			setChannel(j, getFloatChannel(j) + c);
	}

	public void sub( final Readable c )
	{
		for ( int j = 0; j < a.length; ++j )
			setChannel(j, getFloatChannel(j) - c.getFloatChannel(j));
	}
	public void sub( final float c )
	{
		for ( int j = 0; j < a.length; ++j )
			setChannel(j, getFloatChannel(j) - c);
	}

	public void mul( final Readable c )
	{
		for ( int j = 0; j < a.length; ++j )
			setChannel(j, getFloatChannel(j) * c.getFloatChannel(j));
	}
	public void mul( final float c )
	{
		for ( int j = 0; j < a.length; ++j )
			setChannel(j, getFloatChannel(j) * c);
	}

	public void div( final Readable c )
	{
		for ( int j = 0; j < a.length; ++j )
			setChannel(j, getFloatChannel(j) / c.getFloatChannel(j));
	}
	public void div( final float c )
	{
		for ( int j = 0; j < a.length; ++j )
			setChannel(j, getFloatChannel(j) / c);
	}

	//
	// Writable Methods
	//
	public void add( byte c ){ add( ( float )c ); }
	public void add( short c ) { add( ( float )c ); }
	public void add( int c ) { add( ( float )c ); }
	public void add( long c ) {	add( ( float )c ); }
	public void add( double c ) { add( ( float )c ); }

	public void sub( byte c ){ sub( ( float )c ); }
	public void sub( short c ) { sub( ( float )c ); }
	public void sub( int c ) { sub( ( float )c ); }
	public void sub( long c ) {	sub( ( float )c ); }
	public void sub( double c ) { sub( ( float )c ); }

	public void mul( byte c ){ mul( ( float )c ); }
	public void mul( short c ) { mul( ( float )c ); }
	public void mul( int c ) { mul( ( float )c ); }
	public void mul( long c ) {	mul( ( float )c ); }
	public void mul( double c ) { mul( ( float )c ); }

	public void div( byte c ){ div( ( float )c ); }
	public void div( short c ) { div( ( float )c ); }
	public void div( int c ) { div( ( float )c ); }
	public void div( long c ) {	div( ( float )c ); }
	public void div( double c ) { div( ( float )c ); }
}
