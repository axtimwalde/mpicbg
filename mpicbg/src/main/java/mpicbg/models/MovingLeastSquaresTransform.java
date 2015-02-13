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
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>Smooth coordinate transformation interpolating between a set of control
 * points that are maped exactly on top of each other using landmark based
 * deformation by means of Moving Least Squares as described by
 * \citet{SchaeferAl06}.</p>
 *
 * <p>This implementation internally stores the passed {@link PointMatch}
 * objects per reference and is thus best suited for an interactive application
 * where these matches are changed from an external context.</p>
 *
 * <p>BibTeX:</p>
 * <pre>
 * &#64;article{SchaeferAl06,
 *   author    = {Scott Schaefer and Travis McPhail and Joe Warren},
 *   title     = {Image deformation using moving least squares},
 *   journal   = {ACM Transactions on Graphics},
 *   volume    = {25},
 *   number    = {3},
 *   month     = {July},
 *   year      = {2006},
 *   issn      = {0730-0301},
 *   pages     = {533--540},
 *   publisher = {ACM},
 *   address   = {New York, NY, USA},
 * }
 * </pre>
 *
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
public class MovingLeastSquaresTransform extends AbstractMovingLeastSquaresTransform
{
	private static final long serialVersionUID = -8566403075161793547L;

	final protected Set< PointMatch > matches = new HashSet< PointMatch >();
	final public Set< PointMatch > getMatches(){ return matches; }

	@Override
	final public void setMatches( final Collection< PointMatch > matches ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		this.matches.clear();
		this.matches.addAll( matches );
		model.fit( matches );
	}

	@Override
	public void applyInPlace( final double[] location )
	{
		final Collection< PointMatch > weightedMatches = new ArrayList< PointMatch >();
		for ( final PointMatch m : matches )
		{
			final double[] l = m.getP1().getL();

//			/* specific for 2d */
//			final double dx = l[ 0 ] - location[ 0 ];
//			final double dy = l[ 1 ] - location[ 1 ];
//
//			final double weight = m.getWeight() * weigh( 1.0 + Math.sqrt( dx * dx + dy * dy ) );

			double s = 0;
			for ( int i = 0; i < location.length; ++i )
			{
				final double dx = l[ i ] - location[ i ];
				s += dx * dx;
			}
			if ( s <= 0 )
			{
				final double[] w = m.getP2().getW();
				for ( int i = 0; i < location.length; ++i )
					location[ i ] = w[ i ];
				return;
			}
			final double weight = m.getWeight() * weigh( s );
			final PointMatch mw = new PointMatch( m.getP1(), m.getP2(), weight );
			weightedMatches.add( mw );
		}

		try
		{
			model.fit( weightedMatches );
			model.applyInPlace( location );
		}
		catch ( final IllDefinedDataPointsException e ){}
		catch ( final NotEnoughDataPointsException e ){}
	}
}
