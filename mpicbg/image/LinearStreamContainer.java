/**
 * 
 */
package mpicbg.image;

/**
 * @author Preibisch and Saalfeld
 *
 */
public class LinearStreamContainer extends Container implements Iteratable
{
	final DirectIterator directIterator;
	final DirectDimensionIteratable directDimensionIteratable; 
	final DirectRandomAccessible directRandomAccessible; 
	int streamLength;
	
	public int getStreamLength(){ return streamLength; }
	
	public LinearStreamContainer( int[] size, double[] resolution, PixelType pt )
	{
		super( size, resolution, pt );
		streamLength = pt.getNumChannels();
		for ( int i = 0; i < size.length; ++i )
			streamLength *= size[ i ];
		
		Class t = type.getType();
		if ( t == byte.class )
		{
			data = new byte[ streamLength ];
			directIterator = new DirectLinearByteStreamIterator( this, pt );
		}	
		else if ( t == short.class )
			data = new short[ streamLength ];
		else if ( t == int.class )
			data = new int[ streamLength ];
		else if ( t == long.class )
			data = new long[ streamLength ];
		else if ( t == float.class )
			data = new float[ streamLength ];
		else if ( t == double.class )
			data = new double[ streamLength ];
		else data = new Object[ streamLength ];
	}
	
	public DirectRandomAccessible getDirectRandomAccessor()
	{
		
	}
}
