package mpicbg.models;

/**
 * A generic n-dimensional point.
 * 
 * Local coordinates are thought to be immutable, application of a model
 * changes the world coordinates of the point.
 */
public class Point
{
	/**
	 * World coordinates
	 */
	final private float[] w;
	final public float[] getW() { return w; }
	
	/**
	 * Local coordinates
	 */
	final private float[] l;
	final public float[] getL() { return l; }
	
	/**
	 * Constructor
	 *          
	 * Sets this.l to the given float[] reference.
	 * 
	 * @param l reference to the local coordinates of the point
	 */
	public Point( float[] l )
	{
		this.l = l;
//		new float[ l.length ];
		w = l.clone();		
	}
	
	/**
	 * Apply a model to the point.
	 * 
	 * Transfers the local coordinates to new world coordinates.
	 */
	final public void apply( Model model )
	{
		System.arraycopy( l, 0, w, 0, l.length );
		model.applyInPlace( w );
	}
	
	/**
	 * apply a model to the point by a given weight
	 * 
	 * transfers the local coordinates to new world coordinates
	 */
	final public void apply( Model model, float amount )
	{
		float[] a = model.apply( l );
		for ( int i = 0; i < a.length; ++i )
			w[ i ] += amount * ( a[ i ] - w[ i ] );
	}
	
	/**
	 * Apply the inverse of a model to the point.
	 * 
	 * Transfers the local coordinates to new world coordinates.
	 */
	final public void applyInverse( Model model ) throws NoninvertibleModelException
	{
		System.arraycopy( l, 0, w, 0, l.length );
		model.applyInverseInPlace( w );
	}
	
	/**
	 * Estimate the square distance of two points in the world
	 *  
	 * @param p1
	 * @param p2
	 * @return square distance
	 */
	final static public float squareDistance( Point p1, Point p2 )
	{
		double sum = 0.0;
		for ( int i = 0; i < p1.w.length; ++i )
		{
			double d = p1.w[ i ] - p2.w[ i ];
			sum += d * d;
		}
		return ( float )sum;
	}
	
	/**
	 * Estimate the Euclidean distance of two points in the world
	 *  
	 * @param p1
	 * @param p2
	 * @return Euclidean distance
	 */
	final static public float distance( Point p1, Point p2 )
	{
		return ( float )Math.sqrt( squareDistance( p1, p2 ) );
	}
	
	/**
	 * Clone this Point instance.
	 */
	public Point clone()
	{
		Point p = new Point( l.clone() );
		for ( int i = 0; i < w.length; ++i )
			p.w[ i ] = w[ i ];
		return p;
	}
}
