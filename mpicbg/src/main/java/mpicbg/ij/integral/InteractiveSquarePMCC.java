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

import ij.IJ;

/**
 * 
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
final public class InteractiveSquarePMCC extends InteractivePMCC
{
	@Override
	final protected void calculate()
	{
		bc.rSignedSquare( blockRadiusX, blockRadiusY );
	}
	
	@Override
	final protected void showHelp()
	{
		IJ.showMessage(
				"Interactive Signed Square Block Correlation",
				"Click and drag to change the block size." + NL +
				"ENTER - Apply" + NL +
				"ESC - Cancel" );
	}
}
