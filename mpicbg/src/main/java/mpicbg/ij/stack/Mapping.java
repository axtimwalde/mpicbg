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
package mpicbg.ij.stack;

import ij.ImageStack;
import ij.process.ImageProcessor;
import mpicbg.models.CoordinateTransform;
import mpicbg.models.InverseCoordinateTransform;

/**
 * Describes a mapping from {@linkplain ImageStack source} into
 * {@linkplain ImageProcessor target}.
 *
 * The generic parameter <em>T</em> is the function generating the mapping,
 * usually a {@link CoordinateTransform} or an
 * {@link InverseCoordinateTransform}.
 *
 * @author Stephan Saalfeld &lt;saalfeld@mpi-cbg.de&gt;
 * @version 0.1b
 */
public interface Mapping< T >
{
	public T getTransform();

	/**
	 * Map {@linkplain ImageStack source} into {@linkplain ImageProcessor
	 * target}
	 *
	 * @param source
	 * @param target
	 */
	public void map( ImageStack source, ImageProcessor target );

	/**
	 * Map {@linkplain ImageStack source} into {@linkplain ImageProcessor
	 * target} using bilinear interpolation.
	 *
	 * @param source
	 * @param target
	 */
	public void mapInterpolated( ImageStack source, ImageProcessor target );

	/**
	 * Set the slice
	 *
	 * @param slice
	 */
	public void setSlice( final float slice );
}
