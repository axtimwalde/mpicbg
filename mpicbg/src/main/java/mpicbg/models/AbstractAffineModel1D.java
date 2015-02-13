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

/**
 *
 * @param <M>
 *
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
public abstract class AbstractAffineModel1D< M extends AbstractAffineModel1D< M > > extends AbstractModel< M > implements InvertibleBoundable, Affine1D< M >
{
	private static final long serialVersionUID = -986558573112906689L;

	public abstract double[] getMatrix( final double[] m );

	@Override
	public void estimateBounds( final double[] min, final double[] max )
	{
		applyInPlace( min );
		applyInPlace( max );
		if ( min[ 0 ] > max[ 0 ] )
		{
			final double tmp = min[ 0 ];
			min[ 0 ] = max[ 0 ];
			max[ 0 ] = tmp;
		}
	}

	/**
	 * TODO not yet tested!
	 */
	@Override
	public void estimateInverseBounds( final double[] min, final double[] max ) throws NoninvertibleModelException
	{
		applyInverseInPlace( min );
		applyInverseInPlace( max );
		if ( min[ 0 ] > max[ 0 ] )
		{
			final double tmp = min[ 0 ];
			min[ 0 ] = max[ 0 ];
			max[ 0 ] = tmp;
		}
	}
}
