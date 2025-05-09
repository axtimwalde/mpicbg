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
package mpicbg.models;

import java.awt.geom.AffineTransform;

/**
 *
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 */
public interface Affine2D< T extends Affine2D< T > > extends InvertibleCoordinateTransform
{
	/**
	 * Create an {@link AffineTransform} representing the current parameters
	 * the model.
	 *
	 * @return {@link AffineTransform}
	 */
	public AffineTransform createAffine();

	/**
	 * Create an {@link AffineTransform} representing the inverse of the
	 * current parameters of the model.
	 *
	 * @return {@link AffineTransform}
	 */
	public AffineTransform createInverseAffine();

	public void preConcatenate( final T affine2d );
	public void concatenate( final T affine2d );

	/**
	 * Write the 6 parameters of the affine into a double array.  The order is
	 * m00, m10, m01, m11, m02, m12
	 */
	public void toArray( final double[] data );

	/**
	 * Write the 6 parameters of the affine into a 3x2 double array.  The order
	 * is
	 * <pre>{@code
	 * [0][0] -> m00; [0][1] -> m01; [0][2] -> m02;
	 * [1][0] -> m10; [1][1] -> m11; [1][2] -> m12;
	 * }</pre>
	 */
	public void toMatrix( final double[][] data );

	@Override
	public T createInverse();
}
