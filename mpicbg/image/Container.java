package mpicbg.image;

/**
 * Abstract n-dimensional image container.
 * 
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de> and Stephan Preibisch <preibisch@mpi-cbg.de>
 *
 */
public abstract class Container
{
	int[] dim;
	double[] res;
	double[] size;
	final PixelType type;
	
	/**
	 * Create a new container specifying its size.
	 * 
	 * The resolution is set to 1.0px/m for all dimensions.
	 * 
	 * @param type
	 * @param dim per dimension size in px
	 */
	Container( PixelType type, int[] dim )
	{
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
	Container( PixelType type, int[] dim, double[] res )
	{
		this( type, dim );
		setRes( res );
	}
	
	/**
	 * Get the pixel type.
	 * 
	 * @return pixel type
	 */
	public PixelType getPixelType(){ return type; }
	
	/**
	 * Get the number of Dimensions.
	 * 
	 * @return number of dimensions
	 */
	public int getNumDim(){ return dim.length; }
	
	/**
	 * Get the pixel dimensions of the image.
	 * 
	 * @return copy of pixel dimensions
	 */
	public int[] getDim(){ return dim.clone(); }
	
	/**
	 * Get one pixel dimensions of the image.
	 * 
	 * @return one pixel dimension
	 */
	public int getDim( int d ){ return dim[ d ]; }
	
	/**
	 * Get the physical dimensions of the image.
	 * 
	 * @return copy of physical dimensions
	 */
	public double[] getSize(){ return size.clone(); }
	
	/**
	 * Get one physical dimension of the image.
	 * 
	 * @return one physical dimension
	 */
	public double getSize( int d ){ return size[ d ]; }
	
	/**
	 * Get the resolution of the image.
	 * 
	 * @return copy of the resolution
	 */
	public double[] getRes(){ return res.clone(); }

	/**
	 * Get one dimensional resolution of the image.
	 * 
	 * @return copy of the resolution
	 */
	public double getRes( int d ){ return res[ d ]; }
	
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
	public void setRes( double[] res )
	{
		int l = Math.min( dim.length, res.length );
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
	public void setRes( double res, int d )
	{
		this.res[ d ] = res;
		size[ d ] = dim[ d ] / res;
	}
}
