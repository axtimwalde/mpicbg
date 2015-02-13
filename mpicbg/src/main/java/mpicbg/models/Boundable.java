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
 * A {@link CoordinateTransform} that, for a given source interval in
 * <i>n</i>-space, can estimate the target interval in <i>n</i>-space.
 *
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
public interface Boundable extends CoordinateTransform
{
	/**
	 * Estimate the bounds of an n-dimensional interval [min,max] with min and
	 * max being n-dimensional vectors.
	 *
	 * @param min
	 * @param max
	 */
	public void estimateBounds( final double[] min, final double[] max );
}
