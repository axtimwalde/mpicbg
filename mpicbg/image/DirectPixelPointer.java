/**
 * 
 */
package mpicbg.image;

/**
 * @author Preibisch and Saalfeld
 *
 */
public abstract class DirectPixelPointer extends PixelPointer
{
	final int[] coordinates;
	
	DirectPixelPointer( PixelContainer pc )
	{
		super( pc );
		coordinates = new int[ pc.getPixelType().getNumberOfChannels() ];
	}
	
	public int[] getDirectCoordinates()
	{
		return coordinates.clone();
	}
	
	public void getDirectCoordinates( final int[] coordinates )
	{
		System.arraycopy( this.coordinates, 0, coordinates, 0, coordinates.length );
	}
	
	abstract public void add( PixelPointer p );
	abstract public void add( byte a );
	abstract public void add( short a );
	abstract public void add( int a );
	abstract public void add( long a );
	abstract public void add( float a );
	abstract public void add( double a );
	
	abstract public void sub( PixelPointer p );
	abstract public void sub( byte a );
	abstract public void sub( short a );
	abstract public void sub( int a );
	abstract public void sub( long a );
	abstract public void sub( float a );
	abstract public void sub( double a );
	
	abstract public void mul( PixelPointer p );
	abstract public void mul( byte a );
	abstract public void mul( short a );
	abstract public void mul( int a );
	abstract public void mul( long a );
	abstract public void mul( float a );
	abstract public void mul( double a );
	
	abstract public void div( PixelPointer p );
	abstract public void div( byte a );
	abstract public void div( short a );
	abstract public void div( int a );
	abstract public void div( long a );
	abstract public void div( float a );
	abstract public void div( double a );
}
