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
package mpicbg.ij;

import ij.process.ImageProcessor;

/**
 * This interface describes a mapping from a source into a target
 * {@link ImageProcessor Image}.
 * 
 *
 */
public interface Mapping
{
	/**
	 * Map source into target.
	 * 
	 * @param source
	 * @param target
	 */
	abstract public void map(
			ImageProcessor source,
			ImageProcessor target );
	
	/**
	 * Map source into target using bilinear interpolation.
	 * 
	 * @param source
	 * @param target
	 */
	abstract public void mapInterpolated(
			ImageProcessor source,
			ImageProcessor target );
}
