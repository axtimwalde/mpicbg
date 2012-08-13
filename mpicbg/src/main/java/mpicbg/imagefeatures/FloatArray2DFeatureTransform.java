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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

abstract public class FloatArray2DFeatureTransform< P >
{
	final protected P p;
	
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
	 * Initialize the feature transform.
	 * 
	 * @param src image having a generating gaussian kernel of initial_sigma
	 * 	 img must be a 2d-array of float values in range [0.0f, ..., 1.0f]
	 */
	abstract public void init( final FloatArray2D src );
	
	/**
	 * Detect features.
	 * 
	 * @param features the {@link Collection} to be filled
	 */
	abstract public void extractFeatures( final Collection< Feature > features );
	
	final public List< Feature > extractFeatures()
	{
		final List< Feature > features = new ArrayList< Feature >();
		extractFeatures( features );
		return features;
	}
}
