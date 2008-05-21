/**
 * 
 */
package mpicbg.image;

/**
 * Stores an n-dimensional float[] image in a monolithic float array.
 * 
 * TODO This container is limited to a total number of 2G pixels due to the
 *   usage of signed integers for addressing arrays.  This is a miss-design of
 *   the Java programming language and we are full of hope to see that change
 *   in future versions of Java...
 *   
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 *
 */
public class FloatStream extends Container
{
	PixelType type;
	float[] data;
	
	FloatStream( PixelType type, int[] dim )
	{
		super( type, dim );
		
		int l = type.getNumChannels();
		for ( int i = 0; i < dim.length; ++i )
			l *= size[ i ];
		
		data = new float[ l ];
	}
	
	FloatStream( PixelType type, int[] dim, double[] res )
	{
		super( type, dim, res );
	}
	
	public FloatStreamCursor getCursor()
	{
		return new FloatStreamCursor( this );
	}
}
