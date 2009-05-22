package mpicbg.util;


/**
 * Naive Implementation of the Discrete Cosine Transform Type I.
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 *
 */
final public class DCT
{
	/**
	 * Transfer data values into DCT coefficients.  The x-spacing of data
	 * values is 1. 
	 * 
	 * @param f source data values
	 * @param c destination dct coefficients
	 */
	static public void dct( final float[] f, final float c[] )
	{
		final float pn = ( float )( Math.PI / f.length );
		for ( int x = 0; x < f.length; ++x )
		{
			c[ 0 ] += f[ x ];
		}
		c[ 0 ] *= 1.0 / Math.sqrt(  2.0 ) / f.length;
		for ( int k = 1; k < c.length; ++k )
		{
			for ( int x = 0; x < f.length; ++x )
			{
				c[ k ] += f[ x ] * ( float )Math.cos( pn * k * ( x + 0.5 ) );
			}
			c[ k ] /= f.length;
		}
	}
	
	/**
	 * Reconstruct a sample from DCT coefficients.
	 * 
	 * @param c source DCT coefficients
	 * @param x sample location
	 */
	static public float idct( final float[] c, final float x, final float lambda )
	{
		float f = c[ 0 ] * ( float )( 1.0 / Math.sqrt( 2 ) );
		for ( int k = 1; k < c.length; ++k )
		{
			f += c[ k ] * ( float )Math.cos( Math.PI * k * ( x + 0.5 ) / lambda );
		}
		return f * 2;
	}
	
	/**
	 * Reconstruct data values from DCT coefficients.
	 * 
	 * @param c source DCT coefficients
	 * @param f destination data values
	 */
	final static public void idct( float[] c, float f[] )
	{
		float pn = ( float )( Math.PI / f.length );
		float inv_sqrt2_c0 = c[ 0 ] * ( float )( 1.0 / Math.sqrt( 2 ) );
		for ( int x = 0; x < f.length; ++x )
		{
			f[ x ] = inv_sqrt2_c0 ;
			for ( int k = 1; k < c.length; ++k )
			{
				f[ x ] += c[ k ] * ( float )Math.cos( pn * k * ( x + 0.5 ) );
			}
			f[ x ] *= 2;
		}
	}
}
