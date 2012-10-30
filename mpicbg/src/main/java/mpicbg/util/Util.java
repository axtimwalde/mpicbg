package mpicbg.util;


/**
 * Methods collection for general purpose that do not have a common context
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 */
final public class Util
{
	final static public float SQRT2 = ( float )Math.sqrt( 2 );
	
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
	
	/**
	 * Writes min(a,b) into a
	 * 
	 * @param a
	 * @param b
	 */
	final static public void min( final float[] a, final float[] b )
	{
		for ( int i = 0; i < a.length; ++i )
			if ( b[ i ] < a[ i ] ) a[ i ] = b[ i ];
	}
	
	/**
	 * Writes max(a,b) into a
	 * 
	 * @param a
	 * @param b
	 */
	final static public void max( final float[] a, final float[] b )
	{
		for ( int i = 0; i < a.length; ++i )
			if ( b[ i ] > a[ i ] ) a[ i ] = b[ i ];
	}
	
	/**
	 * Round a
	 * 
	 * @param a
	 */
	final static public int round( final float a )
	{
		return ( int )( a + Math.signum( a ) * 0.5f );
	}
	
	/**
	 * Round a
	 * 
	 * @param a
	 */
	final static public int round( final double a )
	{
		return ( int )( a + Math.signum( a ) * 0.5 );
	}
	
	/**
	 * Round a positive a
	 * 
	 * @param a
	 */
	final static public int roundPos( final float a )
	{
		return ( int )( a + 0.5f );
	}
	
	/**
	 * Round a positive a
	 * 
	 * @param a
	 */
	final static public int roundPos( final double a )
	{
		return ( int )( a + 0.5 );
	}
	
	/**
	 * An equivalent to div for float
	 * 
	 * @param a
	 * @param b
	 * @return a div b
	 */
	final static public float div( final float a, final float b )
	{
		final float div = ( int )( a / b );
		if ( b >= 0 )
			return div;
		else
			return div + 1;
	}
	
	
	/**
	 * An equivalent to % for float
	 * 
	 * @param a
	 * @param mod
	 * @return 0 <= b < mod
	 */
	final static public float mod( final float a, final float mod )
	{
		final float b = a - mod * ( int )( a / mod );
		if ( b >= 0 )
			return b;
		else
			return b + mod;
	}
	
	
	/**
	 * For Java, the fastest way doing memset(x)
	 * 
	 * Found at
	 * 
	 * http://burks.brighton.ac.uk/burks/language/java/jprogfaq/faq_b.htm
	 * 
	 * @param array
	 * @param value
	 */
	final public static void memset( final byte[] array, final byte value )
	{
	    final int len = array.length;
	    if ( len > 0 )
	    array[ 0 ] = value;
	    for ( int i = 1; i < len; i += i )
	        System.arraycopy( array, 0, array, i, ( ( len - i ) < i ) ? ( len - i ) : i );
	}
	
	/**
	 * For Java, the fastest way doing memset(x)
	 * 
	 * Found at
	 * 
	 * http://burks.brighton.ac.uk/burks/language/java/jprogfaq/faq_b.htm
	 * 
	 * @param array
	 * @param value
	 */
	final public static void memset( final short[] array, final short value )
	{
	    final int len = array.length;
	    if ( len > 0 )
	    array[ 0 ] = value;
	    for ( int i = 1; i < len; i += i )
	        System.arraycopy( array, 0, array, i, ( ( len - i ) < i ) ? ( len - i ) : i );
	}
	
	/**
	 * For Java, the fastest way doing memset(x)
	 * 
	 * Found at
	 * 
	 * http://burks.brighton.ac.uk/burks/language/java/jprogfaq/faq_b.htm
	 * 
	 * @param array
	 * @param value
	 */
	final public static void memset( final int[] array, final int value )
	{
	    final int len = array.length;
	    if ( len > 0 )
	    array[ 0 ] = value;
	    for ( int i = 1; i < len; i += i )
	        System.arraycopy( array, 0, array, i, ( ( len - i ) < i ) ? ( len - i ) : i );
	}
	
	/**
	 * For Java, the fastest way doing memset(x)
	 * 
	 * Found at
	 * 
	 * http://burks.brighton.ac.uk/burks/language/java/jprogfaq/faq_b.htm
	 * 
	 * @param array
	 * @param value
	 */
	final public static void memset( final long[] array, final long value )
	{
	    final int len = array.length;
	    if ( len > 0 )
	    array[ 0 ] = value;
	    for ( int i = 1; i < len; i += i )
	        System.arraycopy( array, 0, array, i, ( ( len - i ) < i ) ? ( len - i ) : i );
	}
	
	/**
	 * For Java, the fastest way doing memset(x)
	 * 
	 * Found at
	 * 
	 * http://burks.brighton.ac.uk/burks/language/java/jprogfaq/faq_b.htm
	 * 
	 * @param array
	 * @param value
	 */
	final public static void memset( final float[] array, final float value )
	{
	    final int len = array.length;
	    if ( len > 0 )
	    array[ 0 ] = value;
	    for ( int i = 1; i < len; i += i )
	        System.arraycopy( array, 0, array, i, ( ( len - i ) < i ) ? ( len - i ) : i );
	}
	
	/**
	 * For Java, the fastest way doing memset(x)
	 * 
	 * Found at
	 * 
	 * http://burks.brighton.ac.uk/burks/language/java/jprogfaq/faq_b.htm
	 * 
	 * @param array
	 * @param value
	 */
	final public static void memset( final double[] array, final double value )
	{
	    final int len = array.length;
	    if ( len > 0 )
	    array[ 0 ] = value;
	    for ( int i = 1; i < len; i += i )
	        System.arraycopy( array, 0, array, i, ( ( len - i ) < i ) ? ( len - i ) : i );
	}
	
	/**
	 * For Java, the fastest way doing memset(x)
	 * 
	 * Found at
	 * 
	 * http://burks.brighton.ac.uk/burks/language/java/jprogfaq/faq_b.htm
	 * 
	 * @param array
	 * @param value
	 */
	final public static void memset( final boolean[] array, final boolean value )
	{
	    final int len = array.length;
	    if ( len > 0 )
	    array[ 0 ] = value;
	    for ( int i = 1; i < len; i += i )
	        System.arraycopy( array, 0, array, i, ( ( len - i ) < i ) ? ( len - i ) : i );
	}
	
	/**
	 * For Java, the fastest way doing memset(x)
	 * 
	 * Found at
	 * 
	 * http://burks.brighton.ac.uk/burks/language/java/jprogfaq/faq_b.htm
	 * 
	 * @param array
	 * @param value
	 */
	final public static void memset( final char[] array, final char value )
	{
	    final int len = array.length;
	    if ( len > 0 )
	    array[ 0 ] = value;
	    for ( int i = 1; i < len; i += i )
	        System.arraycopy( array, 0, array, i, ( ( len - i ) < i ) ? ( len - i ) : i );
	}	
}
