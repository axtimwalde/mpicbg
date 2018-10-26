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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
* @author Igor Pisarev
*/
public class IllDefinedConfigurationTest
{
	@Test( expected = IllDefinedDataPointsException.class )
	public void testIllDefined() throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		final List< PointMatch > pointMatches = new ArrayList< PointMatch >();
		pointMatches.add( new PointMatch( new Point( new double[] { 11391.245522, -9557.644515, 3214.912133 } ), new Point( new double[] { 2334.168005583636, 3518.967770862933, 107.03217906932194 } ) ) );
		pointMatches.add( new PointMatch( new Point( new double[] { 11070.939867, -8226.053258, 3213.153702 } ), new Point( new double[] { 2015.8052608873058, 4847.946467837462, 117.1739487701094 } ) ) );
		pointMatches.add( new PointMatch( new Point( new double[] { 11070.928153, -8891.92001, 3213.146659 } ), new Point( new double[] { 2013.7415143287162, 4180.5686283211535, 114.917495945355 } ) ) );
		pointMatches.add( new PointMatch( new Point( new double[] { 11391.257236, -8891.777763, 3214.919176 } ), new Point( new double[] { 2335.582386241431, 4185.54358979955, 111.22513943121862 } ) ) );
		pointMatches.add( new PointMatch( new Point( new double[] { 11711.574604, -9557.502269, 3216.68465 } ), new Point( new double[] { 2652.076926238893, 3520.681050219568, 102.93656776245956 } ) ) );
		pointMatches.add( new PointMatch( new Point( new double[] { 11711.586318, -8891.635516, 3216.691693 } ), new Point( new double[] { 2653.9875667012084, 4181.60078651819, 107.51853905527568 } ) ) );
		pointMatches.add( new PointMatch( new Point( new double[] { 11070.91644, -9557.786762, 3213.139616 } ), new Point( new double[] { 2011.6923045944154, 3513.598776666881, 110.35896771552083 } ) ) );

		final double weight = 1. / pointMatches.size();
		for ( final PointMatch pointMatch : pointMatches )
			pointMatch.setWeights( new double[] { weight } );

		Collections.shuffle( pointMatches );

		final AffineModel3D model = new AffineModel3D();
		model.fit( pointMatches );
	}

	@Test
	public void testWellDefined() throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		final List< PointMatch > pointMatches = new ArrayList< PointMatch >();
		pointMatches.add( new PointMatch( new Point( new double[] { 10109.940901, -8892.346749, 3207.829103 } ), new Point( new double[] { 1053.264137949094, 4178.499556821791, 123.9601076750317 } ) ) );
		pointMatches.add( new PointMatch( new Point( new double[] { 10750.610779, -8226.195504, 3211.38118 } ), new Point( new double[] { 1697.794268507087, 4847.981272290586, 119.63945933786255 } ) ) );
		pointMatches.add( new PointMatch( new Point( new double[] { 9789.623532, -8226.622244, 3206.063629 } ), new Point( new double[] { 735.531633561916, 4840.987725209846, 127.4219850438575 } ) ) );
		pointMatches.add( new PointMatch( new Point( new double[] { 10430.269983, -8892.204503, 3209.60162 } ), new Point( new double[] { 1374.5419728598247, 4176.975761289115, 121.43534016162218 } ) ) );
		pointMatches.add( new PointMatch( new Point( new double[] { 10430.281697, -8226.337751, 3209.608663 } ), new Point( new double[] { 1375.6180691601485, 4841.696951542491, 120.5498694360961 } ) ) );
		pointMatches.add( new PointMatch( new Point( new double[] { 10750.599065, -8892.062256, 3211.374137 } ), new Point( new double[] { 1696.3724170447979, 4180.403160693752, 117.54197364214336 } ) ) );
		pointMatches.add( new PointMatch( new Point( new double[] { 9469.294444, -8226.764491, 3204.291107 } ), new Point( new double[] { 419.10957349136515, 4837.719300430445, 136.8057825553509 } ) ) );
		pointMatches.add( new PointMatch( new Point( new double[] { 9469.28273, -8892.631243, 3204.284064 } ), new Point( new double[] { 414.84339554709277, 4172.678354893637, 131.63173672791973 } ) ) );
		pointMatches.add( new PointMatch( new Point( new double[] { 10109.952614, -8226.479997, 3207.836146 } ), new Point( new double[] { 1053.9922003467032, 4843.204481890159, 123.80925600454901 } ) ) );
		pointMatches.add( new PointMatch( new Point( new double[] { 9789.611818, -8892.488996, 3206.056586 } ), new Point( new double[] { 733.8905377640995, 4173.987433223819, 126.7735051753456 } ) ) );

		final double weight = 1. / pointMatches.size();
		for ( final PointMatch pointMatch : pointMatches )
			pointMatch.setWeights( new double[] { weight } );

		Collections.shuffle( pointMatches );

		final AffineModel3D model = new AffineModel3D();
		model.fit( pointMatches );
		Assert.assertTrue( model.isInvertible );
	}
}
