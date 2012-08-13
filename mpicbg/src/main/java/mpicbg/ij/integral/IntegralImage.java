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
package mpicbg.ij.integral;

/**
 * A 2d integral image (summed-area table) as described by Crow (1984).
 * 
 * <p>BibTeX:</p>
 * <pre>
 * &#64;inproceedings{Crow84,
 *	author    = {Franklin C. Crow},
 *	title     = {Summed-area tables for texture mapping},
 *	booktitle = {Proceedings of the 11th annual conference on Computer graphics and interactive techniques},
 *	series    = {SIGGRAPH '84},
 *	year      = {1984},
 *	pages     = {207--212},
 *	publisher = {ACM},
 *	address   = {New York, NY, USA},
 *	keywords  = {Antialiasing, Shading algorithms, Table lookup algorithms, texture mapping},
 *	isbn      = {0-89791-138-5},
 *	numpages  = {6},
 *	url       = {http://doi.acm.org/10.1145/800031.808600},
 *	doi       = {http://doi.acm.org/10.1145/800031.808600}
 * }
 * </pre>
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
public interface IntegralImage
{
	public int getWidth();
	public int getHeight();
	public int getSum( final int xMin, final int yMin, final int xMax, final int yMax );
	public int getScaledSum( final int xMin, final int yMin, final int xMax, final int yMax, final float scale );
}
