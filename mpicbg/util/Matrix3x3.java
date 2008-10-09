/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 *
 */
package mpicbg.util;

import mpicbg.models.NoninvertibleModelException;

/**
 * Basic operations on 3x3 float matrices.
 * 
 */
public class Matrix3x3
{
	private Matrix3x3(){}
	
	/**
	 * Estimate the determinant.
	 * 
	 * @param a matrix given row by row
	 * 
	 * @return determinant
	 */
	final static float det( final float[] a )
	{
		assert a.length != 9 : "Matrix3x3 supports 3x3 float[][] only.";
		
		return
			a[ 0 ] * a[ 4 ] * a[ 8 ] +
			a[ 3 ] * a[ 7 ] * a[ 2 ] +
			a[ 6 ] * a[ 1 ] * a[ 5 ] -
			a[ 2 ] * a[ 4 ] * a[ 6 ] -
			a[ 5 ] * a[ 7 ] * a[ 0 ] -
			a[ 8 ] * a[ 1 ] * a[ 3 ];
	}
	
	final static float det(
			final float a00, final float a01, final float a02,
			final float a10, final float a11, final float a12,
			final float a20, final float a21, final float a22 )
	{
		return
			a00 * a11 * a22 +
			a10 * a21 * a02 +
			a20 * a01 * a12 -
			a02 * a11 * a20 -
			a12 * a21 * a00 -
			a22 * a01 * a10;
	}
	
	final static public void invert( float[] a ) throws NoninvertibleModelException
	{
		assert a.length != 9 : "Matrix3x3 supports 3x3 float[][] only.";
		
		final float det = det( a );
		if ( det == 0 ) throw new NoninvertibleModelException( "Matrix not invertible." );
		
		final float i00 = ( a[ 4 ] * a[ 8 ] - a[ 5 ] * a[ 7 ] ) / det;
		final float i01 = ( a[ 2 ] * a[ 7 ] - a[ 1 ] * a[ 8 ] ) / det;
		final float i02 = ( a[ 1 ] * a[ 5 ] - a[ 2 ] * a[ 4 ] ) / det;
		
		final float i10 = ( a[ 5 ] * a[ 6 ] - a[ 3 ] * a[ 8 ] ) / det;
		final float i11 = ( a[ 0 ] * a[ 8 ] - a[ 2 ] * a[ 6 ] ) / det;
		final float i12 = ( a[ 2 ] * a[ 3 ] - a[ 0 ] * a[ 5 ] ) / det;
		
		final float i20 = ( a[ 3 ] * a[ 7 ] - a[ 4 ] * a[ 6 ] ) / det;
		final float i21 = ( a[ 1 ] * a[ 6 ] - a[ 0 ] * a[ 7 ] ) / det;
		final float i22 = ( a[ 0 ] * a[ 4 ] - a[ 1 ] * a[ 3 ] ) / det;
		
		a[ 0 ] = i00;
		a[ 1 ] = i01;
		a[ 2 ] = i02;

		a[ 3 ] = i10;
		a[ 4 ] = i11;
		a[ 5 ] = i12;

		a[ 6 ] = i20;
		a[ 7 ] = i21;
		a[ 8 ] = i22;
	}

	final static public float[][] createInverse(
			final float a00, final float a01, final float a02,
			final float a10, final float a11, final float a12,
			final float a20, final float a21, final float a22 ) throws NoninvertibleModelException
	{
		final float det = det( a00, a01, a02, a10, a11, a12, a20, a21, a22 );
		if ( det == 0 ) throw new NoninvertibleModelException( "Matrix not invertible." );
		
		return new float[][]{
				{ ( a11 * a22 - a12 * a21 ) / det, ( a02 * a21 - a01 * a22 ) / det, ( a01 * a12 - a02 * a11 ) / det },
				{ ( a12 * a20 - a10 * a22 ) / det, ( a00 * a22 - a02 * a20 ) / det, ( a02 * a10 - a00 * a12 ) / det },
				{ ( a10 * a21 - a11 * a20 ) / det, ( a01 * a20 - a00 * a21 ) / det, ( a00 * a11 - a01 * a10 ) / det } };
	}

}
