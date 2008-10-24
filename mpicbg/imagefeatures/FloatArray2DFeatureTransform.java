/**
 *  License: GPL
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
 * NOTE:
 * The SIFT-method is protected by U.S. Patent 6,711,293: "Method and
 * apparatus for identifying scale invariant features in an image and use of
 * same for locating an object in an image" by the University of British
 * Columbia.  That is, for commercial applications the permission of the author
 * is required.
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.4b
 */
package mpicbg.imagefeatures;

import java.util.List;

import mpicbg.models.*;

abstract public class FloatArray2DFeatureTransform< P >
{
	final P p;
	
	/**
	 * Constructor
	 * 
	 * @param p parameters
	 */
	public FloatArray2DFeatureTransform( final P p )
	{
		this.p = p;
	}
	
	
	/**
	 * initialize the scale space as a scale pyramid having octave stubs only
	 * 
	 * @param src image having a generating gaussian kernel of initial_sigma
	 * 	 img must be a 2d-array of float values in range [0.0f, ..., 1.0f]
	 */
	abstract public void init( final FloatArray2D src );
	
	/**
	 * detect features in all scale octaves
	 * 
	 * @param features the list to be filled
	 * 
	 * @return number of detected features
	 */
	abstract public int extractFeatures( final List< Feature > features );
	
	/**
	 * Identify corresponding features
	 * 
	 * @param fs1 feature collection from set 1
	 * @param fs2 feature collection from set 2
	 * @param rod Ratio of distances (closest/next closest match)
	 * 
	 * @return matches
	 */
	static public int matchFeatures(
			final List< Feature > fs1,
			final List< Feature > fs2,
			final List< PointMatch > matches,
			final float rod )
	{
		for ( final Feature f1 : fs1 )
		{
			Feature best = null;
			float best_d = Float.MAX_VALUE;
			float second_best_d = Float.MAX_VALUE;
			
			for ( final Feature f2 : fs2 )
			{
				float d = f1.descriptorDistance( f2 );
				if ( d < best_d )
				{
					second_best_d = best_d;
					best_d = d;
					best = f2;
				}
				else if ( d < second_best_d )
					second_best_d = d;
			}
			if ( best != null && second_best_d < Float.MAX_VALUE && best_d / second_best_d < rod )
				matches.add(
						new PointMatch(
								new Point(
										new float[] { f1.location[ 0 ], f1.location[ 1 ] } ),
								new Point(
										new float[] { best.location[ 0 ], best.location[ 1 ] } ) ) );
		}
		
		// now remove ambiguous matches
		for ( int i = 0; i < matches.size(); )
		{
			boolean amb = false;
			final PointMatch m = matches.get( i );
			final float[] m_p2 = m.getP2().getL(); 
			for ( int j = i + 1; j < matches.size(); )
			{
				final PointMatch n = matches.get( j );
				final float[] n_p2 = n.getP2().getL(); 
				if ( m_p2[ 0 ] == n_p2[ 0 ] && m_p2[ 1 ] == n_p2[ 1 ] )
				{
					amb = true;
					matches.remove( j );
				}
				else ++j;
			}
			if ( amb )
				matches.remove( i );
			else ++i;
		}
		return matches.size();
	}
}
