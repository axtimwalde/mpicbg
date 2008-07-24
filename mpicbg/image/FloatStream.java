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
public class FloatStream extends Stream
{
	final float[] data;
	final protected FloatStreamRead reader;
	final protected FloatStreamWrite writer;
	
	public FloatStream( final PixelType type, final int[] dim )
	{
		super( type, dim );
		data = new float[ numPixels ];
		reader = new FloatStreamRead( this );
		writer = new FloatStreamWrite( this );
	}

	public FloatStream( final PixelType type, final int[] dim, final double[] res )
	{
		this( type, dim );
		setRes( res );		
	}
	
	@Override
	final public ContainerRead<StreamCursor> getReader(){ return reader; }
	
	@Override
	final public ContainerWrite<StreamCursor> getWriter(){ return writer; }
}
