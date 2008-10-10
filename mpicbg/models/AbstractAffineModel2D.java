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
package mpicbg.models;

import java.awt.geom.AffineTransform;

/**
 * Abstract 
 * 
 * @version 0.2b
 * 
 */
public abstract class AbstractAffineModel2D< M extends AbstractAffineModel2D< M > > extends InvertibleModel< M >
{
	/**
	 * Create an {@link AffineTransform} representing the current parameters
	 * the model.
	 * 
	 * @return {@link AffineTransform}
	 */
	abstract public AffineTransform createAffine();
	
	/**
	 * Create an {@link AffineTransform} representing the inverse of the
	 * current parameters of the model.
	 * 
	 * @return {@link AffineTransform}
	 */
	abstract public AffineTransform createInverseAffine();
	
	@Override
	public String toString()
	{
		return ( "[3,3](" + createAffine() + ") " + cost );
	}
	
	abstract public void preConcatenate( final M model );
	abstract public void concatenate( final M model );
}
