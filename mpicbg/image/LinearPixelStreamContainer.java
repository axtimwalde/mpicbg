/**
 * 
 */
package mpicbg.image;

/**
 * @author Preibisch and Saalfeld
 *
 */
public class LinearPixelStreamContainer extends PixelContainer
{
	public LinearPixelStreamContainer( int[] size, double[] resolution, PixelType pt )
	{
		super( size, resolution, pt );
		int l = pt.getNumberOfChannels();
		for ( int i = 0; i < size.length; ++i )
			l *= size[ i ];
		
		Class t = type.getType();
		if ( t == byte.class )
			data = new byte[ l ]; 
		else if ( t == short.class )
			data = new short[ l ];
		else if ( t == int.class )
			data = new int[ l ];
		else if ( t == long.class )
			data = new long[ l ];
		else if ( t == float.class )
			data = new float[ l ];
		else if ( t == double.class )
			data = new double[ l ];
	}
}
