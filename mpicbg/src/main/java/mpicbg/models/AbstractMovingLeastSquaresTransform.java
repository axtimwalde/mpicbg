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

import java.io.Serializable;
import java.util.Collection;

/**
 * Smooth coordinate transformation interpolating between a set of control
 * points that are maped exactly on top of each other using landmark based deformation by means
 * of Moving Least Squares as described by \citet{SchaeferAl06}.
 *
 * BibTeX:
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
public abstract class AbstractMovingLeastSquaresTransform implements CoordinateTransform, Serializable
{
	private static final long serialVersionUID = 7058285996477613433L;

	protected Model< ? > model = null;
	final public Model< ? > getModel(){ return model; }
	final public void setModel( final Model< ? > model ){ this.model = model; }
	final public void setModel( final Class< ? extends Model< ? > > modelClass ) throws Exception
	{
		model = modelClass.newInstance();
	}

	protected double alpha = 1.0f;
	final public double getAlpha(){ return alpha; }
	final public void setAlpha( final double alpha ){ this.alpha = alpha; }

	abstract public void setMatches( final Collection< PointMatch > matches )
		throws NotEnoughDataPointsException, IllDefinedDataPointsException;

	protected double weigh( final double d )
	{
		return 1.0 / Math.pow( d, alpha );
	}

	@Override
	final public double[] apply( final double[] location )
	{
		final double[] a = location.clone();
		applyInPlace( a );
		return a;
	}
}
