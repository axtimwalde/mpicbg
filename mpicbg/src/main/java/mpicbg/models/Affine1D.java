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
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
public interface Affine1D< T extends Affine1D< T > > extends InvertibleCoordinateTransform
{
	public void preConcatenate( final T affine3d );
	public void concatenate( final T affine3d );

	/**
	 * Write the 2 parameters of the affine into a double array.  The order is
	 * m00, m01
	 *
	 * @return
	 */
	public void toArray( final double[] data );

	/**
	 * Write the 2 parameters of the affine into a 2x1 double array.  The order
	 * is
	 * [0][0] -> m00; [0][1] -> m01;
	 *
	 * @return
	 */
	public void toMatrix( final double[][] data );

	@Override
	public T createInverse();
}
