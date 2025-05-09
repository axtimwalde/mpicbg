/*-
 * #%L
 * MPICBG Core Library.
 * %%
 * Copyright (C) 2008 - 2025 Stephan Saalfeld et. al.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package mpicbg.util;


/**
 * Naive Implementation of the Discrete Cosine Transform Type I.
 * 
 * @author Stephan Saalfeld &lt;saalfeld@mpi-cbg.de&gt;
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
