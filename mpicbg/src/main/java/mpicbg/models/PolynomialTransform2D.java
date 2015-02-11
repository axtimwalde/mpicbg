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
	/**
	 * holds two coefficients for each polynomial coefficient, including 1
	 * initialized at 0 order, i.e. translation, the order follows that
	 * specified at
	 *
	 * http://bishopw.loni.ucla.edu/AIR5/2Dnonlinear.html#default
	 *
	 * two times
	 */
	protected double[] a = new double[ 2 ];

	/**
	 * register to hold all polynomial terms during applyInPlace following the
	 * order specified at
	 *
	 * http://bishopw.loni.ucla.edu/AIR5/2Dnonlinear.html#default
	 *
	 * excluding 1 because we want to avoid repeated multiplication with 1
	 */
	protected double[] polTerms = new double[ 0 ];

	public void set( final double... a ){
		this.a =  a.clone();
		polTerms = new double[ a.length * 2 ];
	}

	protected void populateCoefficients()
	{

	}

	@Override
	public float[] apply( final float[] location )
	{
		final float[] copy = location.clone();
		applyInPlace( copy );
		return copy;
	}

	@Override
	public void applyInPlace( final float[] location )
	{
		final double x = location[ 0 ];
		final double y = location[ 1 ];
		final double u =
				a[ 0 ] +
				a[ 1 ] * x +
				a[ 2 ] * y +
				a[ 3 ] * x * x +
				a[ 4 ] * x * y +
				a[ 5 ] * y * y;

		final double v =
				a[ 6 ] +
				a[ 7 ] * x +
				a[ 8 ] * y +
				a[ 9 ] * x * x +
				a[ 10 ] * x * y +
				a[ 11 ] * y * y;

		location[ 0 ] = ( float )u;
		location[ 1 ] = ( float )v;
	}

	final static public void main( final String... args )
	{
		new ImageJ();

		final ImagePlus imp = new ImagePlus( "/tier2/flyTEM/khairy/FOR_STEPHAN/second_order_polynomial_transformation/original_image.tif" );
		imp.show();

		final PolynomialTransform2D t = new PolynomialTransform2D();
		t.set(
				67572.7357, 0.97263708, -0.0266434795,
				-3.08962708e-06, 3.52672467e-06, 1.36924462e-07,
				5446.8534, 0.022404762, 0.96120261,
				-3.3675352e-07, -8.9721973e-07, -5.4985399e-06 );

		final CoordinateTransformMesh boundsMesh = new CoordinateTransformMesh( t, 64, imp.getWidth(), imp.getHeight() );
		final float[] min = new float[2];
		final float[] max = new float[2];
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
	}

}
