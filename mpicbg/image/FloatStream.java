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
 * @author Saalfeld <saalfeld@mpi-cbg.de>
 *
 */
public class FloatStream< P extends FloatPixel > extends Stream
{
	final float[] data;
	
	public FloatStream( final P type, final int[] dim )
	{
		super( type, dim );
		data = new float[ numPixels ];
	}

	public FloatStream( P type, int[] dim, double[] res )
	{
		this( type, dim );
		setRes( res );		
	}
	
	@Override
	final public AccessStrategy createDirectAccessStrategy()
	{
		return new AccessStrategyFloatStream(this, null);
	}
}
