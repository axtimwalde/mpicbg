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
 * Exclusively static methods that would ideally be implemented in
 * {@link CoordinateTransform} and related interfaces, but well---this is Java
 * where multiple inheritance is considered evil...
 *
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
public class Transforms
{
	private Transforms(){}

	/**
	 * Check if a {@link CoordinateTransform} is the identity transform with
	 * respect to a set of {@link Point Points} and a given tolerance.
	 *
	 * @param t
	 * @param points
	 * @param tolerance
	 * @return
	 */
	static public boolean isIdentity( final CoordinateTransform t, final Iterable< Point > points, final double tolerance )
	{
		final double t2 = tolerance * tolerance;
		for ( final Point p : points )
		{
			p.apply( t );
			if ( p.squareDistance() > t2 ) return false;
		}
		return true;
	}
}
