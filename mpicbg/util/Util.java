package mpicbg.util;

/**
 * Methods collection for general purpose that do not have a common context
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 */
final public class Util
{
	private Util(){}
	
	/**
	 * (Hopefully) fast floor log<sub>2</sub> of an unsigned(!) integer value.
	 * 
	 * @param v unsigned integer
	 * @return floor log<sub>2</sub>
	 */
	final static public int ldu( int v )
	{
	    int c = 0;
	    do
	    {
	    	v >>= 1;
	        ++c;
	    }
	    while ( v > 1 );
	    return c;
	}
	
	/**
	 * Return an unsigned integer that bounces in a ping pong manner in the range [0 ... mod - 1]
	 *
	 * @param a the value to be flipped
	 * @param range the size of the range
	 * @return a flipped in range like a ping pong ball
	 */
	final static public int pingPong( int a, final int mod )
	{
		final int p = 2 * mod;
		if ( a < 0 ) a = p + a % p;
		if ( a >= p ) a = a % p;
		if ( a >= mod ) a = mod - a % mod - 1;
		return a;
	}
	
	/**
	 * Integer version of {@link Math#pow(double, double)}.
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	final static public long pow( final int a, final int b )
	{
		long c = 1;
		for ( int i = 0; i < b; ++i )
			c *= a;
		return c;
	}
	
	/**
	 * Float/Integer version of {@link Math#pow(double, double)}.
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	final static public float pow( final float a, final int b )
	{
		float c = 1;
		for ( int i = 0; i < b; ++i )
			c *= a;
		return c;
	}
	
	/**
	 * Double/Integer version of {@link Math#pow(double, double)}.
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	final static public double pow( final double a, final int b )
	{
		double c = 1;
		for ( int i = 0; i < b; ++i )
			c *= a;
		return c;
	}
}
