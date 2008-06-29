package mpicbg.models;

/**
 * a generic n-dimensional point location
 * 
 * keeps local coordinates final, application of a model changes the world
 * coordinates of the point
 */
public class Point
{
	/**
	 * World coordinates
	 */
	private float[] w;
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
	final public void apply( Model model, float weight )
	{
		weight = Math.max( 0.0f, Math.min( 1.0f, weight ) );
		float[] a = model.apply( l );
		float weight1 = 1.0f - weight;
		for ( int i = 0; i < a.length; ++i )
			w[ i ] = weight * a[ i ] + weight1 * w[ i ];
	}
	
	/**
	 * estimate the Euclidean distance of two points in the world
	 *  
	 * @param p1
	 * @param p2
	 * @return Euclidean distance
	 */
	final public static float distance( Point p1, Point p2 )
	{
		double sum = 0.0;
		for ( int i = 0; i < p1.w.length; ++i )
		{
			double d = p1.w[ i ] - p2.w[ i ];
			sum += d * d;
		}
		return ( float )Math.sqrt( sum );
	}
	
	/**
	 * Clone this Point instance.
	 */
	public Point clone()
	{
		Point p = new Point( l.clone() );
		p.w = w.clone();
		return p;
	}
}
