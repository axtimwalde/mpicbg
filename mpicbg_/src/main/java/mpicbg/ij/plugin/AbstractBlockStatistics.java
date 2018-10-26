package mpicbg.ij.plugin;
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


import ij.ImagePlus;

/**
 * Abstract base class for variance and STD filters.
 *
 * @author Stephan Saalfeld &lt;saalfeld@mpi-cbg.de&gt;
 * @version 0.1a
 */
abstract public class AbstractBlockStatistics extends AbstractBlockFilter
{
	protected mpicbg.ij.integral.BlockStatistics[] bss;
	
	@Override
	protected void copyParameters()
	{
		synchronized( this )
		{
			brx = blockRadiusX;
			bry = blockRadiusY;
		}
	}
	
	@Override
	protected void init( final ImagePlus imp )
	{
		super.init( imp );
		if ( imp.getType() == ImagePlus.COLOR_RGB )
		{
			bss = new mpicbg.ij.integral.BlockStatistics[]{
					new mpicbg.ij.integral.BlockStatistics( fps[ 0 ] ),
					new mpicbg.ij.integral.BlockStatistics( fps[ 1 ] ),
					new mpicbg.ij.integral.BlockStatistics( fps[ 2 ] ) };
		}
		else
		{
			bss = new mpicbg.ij.integral.BlockStatistics[]{ new mpicbg.ij.integral.BlockStatistics( fps[ 0 ] ) };
		}
	}	
}
