/*-
 * #%L
 * MPICBG Core Library.
 * %%
 * Copyright (C) 2008 - 2025 Stephan Saalfeld et. al.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
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
 * @author Stephan Saalfeld &lt;saalfeld@mpi-cbg.de&gt;
 */
final public class InteractiveMean extends AbstractInteractiveBlockFilter
{
	private Mean mean;
	
	@Override
	final protected void init()
	{
		mean = Mean.create( imp.getProcessor() );
	}
	
	@Override
	final protected void draw()
	{
		mean.mean( blockRadiusX, blockRadiusY );
	}
	
	@Override
	final public void showHelp()
	{
		IJ.showMessage(
				"Interactive Mean Smooth",
				"Click and drag to change the size of the smoothing kernel." + NL +
				"ENTER - Apply" + NL +
				"ESC - Cancel" );
	}
}
