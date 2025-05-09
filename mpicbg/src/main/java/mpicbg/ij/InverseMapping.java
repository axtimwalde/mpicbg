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
package mpicbg.ij;

import mpicbg.models.CoordinateTransform;
import ij.process.ImageProcessor;

/**
 * Describes an inverse mapping from {@linkplain ImageProcessor source} into
 * {@linkplain ImageProcessor target}.
 * 
 * @author Stephan Saalfeld &lt;saalfeld@mpi-cbg.de&gt;
 * @version 0.1b
 */
public interface InverseMapping< T extends CoordinateTransform >
{
	public T getTransform();
	
	/**
	 * Map inversely {@linkplain ImageProcessor source} into
	 * {@linkplain ImageProcessor target}
	 * 
	 * @param source
	 * @param target
	 */
	public void mapInverse(
			ImageProcessor source,
			ImageProcessor target );
	
	/**
	 * Map inversely {@linkplain ImageProcessor source} into
	 * {@linkplain ImageProcessor target} using bilinear interpolation.
	 * 
	 * @param source
	 * @param target
	 */
	public void mapInverseInterpolated(
			ImageProcessor source,
			ImageProcessor target );
}
