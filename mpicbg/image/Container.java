package mpicbg.image;

/**
 * Abstract n-dimensional image container.
 * 
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de> and Stephan Preibisch <preibisch@mpi-cbg.de>
 *
 */
public abstract class Container< P extends PixelType, R extends ContainerRead, W extends ContainerWrite >
{
	final int[] dim;
	final double[] res;
	final double[] size;
	final P type;
	
	abstract public R getReader();
	abstract public W getWriter();
	
	/**
	 * Create a new container specifying its size.
	 * 
	 * The resolution is set to 1.0px/m for all dimensions.
	 * 
	 * @param type
	 * @param dim per dimension size in px
	 */
	Container( final P type, final int[] dim )
	{
		assert dim.length > 0 : "Container(): Size of dim[] is " + dim.length;
		
		this.type = type;
		this.dim = dim.clone();
		res = new double[ dim.length ];
		size = new double[ dim.length ];
		for ( int i = 0; i < dim.length; ++i )
		{
			res[ i ] = 1.0;
			size[ i ] = dim[ i ];
		}
	}
	
	/**
	 * Create a new container specifying its size and resolution.
	 * 
	 * If resolution.length > size.length then the first size.length elements
	 * of resolution are used.
	 * If resolution.length < size.length then all
	 * resolution[ size.length + {0,1,...,n} ] = 1.0
	 * 
	 * @param dim per dimension size in px
	 * @param res per dimension resolution in px/m
	 */
	Container( final P type, final int[] dim, final double[] res )
	{
		this( type, dim );
		setRes( res );
	}

	/**
	 * Get the pixel type.
	 * 
	 * @return pixel type
	 */
	final public PixelType getPixelType(){ return type; }
	
	/**
	 * Get the number of Dimensions.
	 * 
	 * @return number of dimensions
	 */
	final public int getNumDim(){ return dim.length; }
	
	/**
	 * Get the pixel dimensions of the image.
	 * 
	 * @return copy of pixel dimensions
	 */
	final public int[] getDim(){ return dim.clone(); }
	
	/**
	 * Get one pixel dimensions of the image.
	 * 
	 * @return one pixel dimension
	 */
	final public int getDim( final int d ){ return dim[ d ]; }
	
	/**
	 * Get the physical dimensions of the image.
	 * 
	 * @return copy of physical dimensions
	 */
	final public double[] getSize(){ return size.clone(); }
	
	/**
	 * Get one physical dimension of the image.
	 * 
	 * @return one physical dimension
	 */
	final public double getSize( final int d ){ return size[ d ]; }
	
	/**
	 * Get the resolution of the image.
	 * 
	 * @return copy of the resolution
	 */
	final public double[] getRes(){ return res.clone(); }

	/**
	 * Get one dimensional resolution of the image.
	 * 
	 * @return copy of the resolution
	 */
	final public double getRes( final int d ){ return res[ d ]; }
	
	/**
	 * Set the resolution of the image.
	 * 
	 * If resolution.length > size.length then the first size.length elements
	 * of resolution are used.
	 * If resolution.length < size.length then all
	 * resolution[ size.length + {0,1,...,n} ] = 1.0
	 * 
	 * @param res
	 */
	final public void setRes( final double[] res )
	{
		assert res.length > 0 : "Container.setRes(): Size of res[] is " + dim.length;

		final int l = Math.min( dim.length, res.length );
		for ( int i = 0; i < l; ++i )
		{
			this.res[ i ] = res[ i ];
			size[ i ] = dim[ i ] / res[ i ];
		}
	}
	
	/**
	 * Set one dimensional resolution of the image.
	 * 
	 * @param res dimensional resolution
	 */
	final public void setRes( final double res, final int d )
	{
		this.res[ d ] = res;
		size[ d ] = dim[ d ] / res;
	}
}
