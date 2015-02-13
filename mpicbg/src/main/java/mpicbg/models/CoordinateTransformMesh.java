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

import java.util.Set;

/**
 *
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
public class CoordinateTransformMesh extends TransformMesh
{
	private static final long serialVersionUID = 678862124003647263L;

	public CoordinateTransformMesh(
			final CoordinateTransform t,
			final int numX,
			final double width,
			final double height )
	{
		super( numX, numY( numX, width, height ), width, height );

		final Set< PointMatch > vertices = va.keySet();
		for ( final PointMatch vertex : vertices )
			vertex.getP2().apply( t );

		updateAffines();
	}
}
