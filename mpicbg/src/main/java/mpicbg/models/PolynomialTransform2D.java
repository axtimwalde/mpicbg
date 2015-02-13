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
 */
package mpicbg.models;

import ij.ImageJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

import java.util.Arrays;

import mpicbg.ij.TransformMeshMapping;

/**
 *
 *
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
public class PolynomialTransform2D implements CoordinateTransform
{
	private static final long serialVersionUID = 1101543117241957329L;

	/**
	 * order of the polynomial transform
	 */
	protected int order = 0;

	/**
	 * holds two coefficients for each polynomial coefficient, including 1
	 * initialized at 0 order, i.e. translation, the order follows that
	 * specified at
	 *
	 * http://bishopw.loni.ucla.edu/AIR5/2Dnonlinear.html#polylist
	 *
	 * two times
	 */
	protected double[] a = new double[ 2 ];

	/**
	 * register to hold all polynomial terms during applyInPlace following the
	 * order specified at
	 *
	 * http://bishopw.loni.ucla.edu/AIR5/2Dnonlinear.html#polylist
	 *
	 * excluding 1 because we want to avoid repeated multiplication with 1
	 */
	protected double[] polTerms = new double[ 0 ];

	/**
	 * Calculate the maximum order of a polynom whose number of polyynomial
	 * terms is smaller or equal a given number.
	 *
	 * @param numPolTerms
	 * @return
	 */
	final static public int orderOf( final int numPolTerms )
	{
		return ( int )Math.nextUp( ( Math.sqrt( 2 * numPolTerms + 0.25 ) - 1.5 ) );
	}

	/**
	 * Calculate the number of polynomial terms for a 2d polynomial transform
	 * of given order.
	 *
	 * @param order
	 * @return
	 */
	final static public int numPolTerms( final int order )
	{
		return ( int )Math.round( ( order + 2 ) * ( order + 1 ) * 0.5 );
	}

	/**
	 * Set the coefficients.  The number of coefficients implicitly specifies
	 * the order of the {@link PolynomialTransform2D} which is set to the
	 * highest order that is fully specified by the provided coefficients.
	 * The coefficients are interpreted in the order specified at
	 *
	 * http://bishopw.loni.ucla.edu/AIR5/2Dnonlinear.html#polylist
	 *
	 * , first for x', then for y'.  It is thus not possible to omit higher
	 * order coefficients assuming that they would become 0.  The passed vararg
	 * array is used directly without copy which enables direct access to the
	 * coefficients from calling code.  Use this option wisely.
	 *
	 * @param a coefficients
	 */
	public void set( final double... a ){
		order = orderOf( a.length / 2 );
		final int numPolTerms = numPolTerms( order );

		this.a = a;
		/* this would certainly be safer but means that we do not have access to the coefficients later */
//		this.a =  new double[ numPolTerms * 2 ];
//		System.arraycopy( a, 0, this.a, 0, this.a.length );

		polTerms = new double[ numPolTerms - 1 ];
	}

	protected void populateTerms( final double x, final double y )
	{
		if ( order == 0 ) return;
		polTerms[ 0 ] = x;
		polTerms[ 1 ] = y;
		for ( int o = 2, i = 2; o <= order; ++o, i += o )
		{
			for ( int p = 0; p < o; ++p)
			{
				polTerms[ i + p ] = polTerms[ i + p - o ] * x;
			}
			polTerms[ i + o ] = polTerms[ i - 1 ] * y;
		}
	}

	protected void printTerms()
	{
		final String[] polTermString = new String[ polTerms.length ];
		if ( order == 0 )
			System.out.println( "No polynomial terms." );
		polTermString[ 0 ] = "x";
		polTermString[ 1 ] = "y";
		for ( int o = 2, i = 2; o <= order; ++o, i += o )
		{
			for ( int p = 0; p < o; ++p)
			{
				polTermString[ i + p ] = polTermString[ i + p - o ] + "x";
			}
			polTermString[ i + o ] = polTermString[ i - 1 ] + "y";
		}
		System.out.println( Arrays.toString( polTermString ) );
	}

	@Override
	public double[] apply( final double[] location )
	{
		final double[] copy = location.clone();
		applyInPlace( copy );
		return copy;
	}

	@Override
	public void applyInPlace( final double[] location )
	{
		populateTerms( location[ 0 ], location[ 1 ] );
		location[ 0 ] = a[ 0 ];
		for ( int i = 0; i < polTerms.length;)
			location[ 0 ] += polTerms[ i ] * a[ ++i ];
		final int numPolTerms = polTerms.length + 1;
		location[ 1 ] = a[ numPolTerms ];
		for ( int i = 0; i < polTerms.length;)
			location[ 1 ] += polTerms[ i ] * a[ ++i + numPolTerms ];
	}

	final static public void main( final String... args )
	{
		new ImageJ();

		for ( int numPolTerms = 0; numPolTerms < 100; ++numPolTerms )
		{
			System.out.println( numPolTerms + " " + orderOf( numPolTerms ) + " " + numPolTerms( orderOf( numPolTerms ) ) );
		}

		final ImagePlus imp = new ImagePlus( "/tier2/flyTEM/khairy/FOR_STEPHAN/second_order_polynomial_transformation/original_image.tif" );
		imp.show();

		final PolynomialTransform2D t = new PolynomialTransform2D();
		t.set(
				67572.7357, 0.97263708, -0.0266434795,
				-3.08962708e-06, 3.52672467e-06, 1.36924462e-07,
				5446.8534, 0.022404762, 0.96120261,
				-3.3675352e-07, -8.9721973e-07, -5.4985399e-06 );

		final CoordinateTransformMesh boundsMesh = new CoordinateTransformMesh( t, 64, imp.getWidth(), imp.getHeight() );
		final double[] min = new double[2];
		final double[] max = new double[2];
		boundsMesh.bounds( min, max );
		System.out.println( Arrays.toString( min ) );
		final TranslationModel2D shift = new TranslationModel2D();
		shift.set( -min[0], -min[1] );
		final CoordinateTransformList< CoordinateTransform > ctl = new CoordinateTransformList< CoordinateTransform >();
		ctl.add( t );
		ctl.add( shift );

		final CoordinateTransformMesh mesh = new CoordinateTransformMesh( ctl, 64, imp.getWidth(), imp.getHeight() );
		final TransformMeshMapping< TransformMesh > mapping = new TransformMeshMapping< TransformMesh >( mesh );

		final ImageProcessor target = imp.getProcessor().createProcessor( imp.getWidth(), imp.getHeight() );
		imp.getProcessor().setInterpolationMethod( ImageProcessor.BILINEAR );
		mapping.mapInterpolated( imp.getProcessor(), target );
		new ImagePlus( imp.getTitle() + " warped", target ).show();

		t.set( new double[ 66 * 2 ] );
		t.printTerms();
	}
}
