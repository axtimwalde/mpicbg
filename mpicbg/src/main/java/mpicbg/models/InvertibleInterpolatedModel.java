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


/**
 * Invertible specialization of {@link InterpolatedModel}.
 *
 * For invertible matrices A and B, the interpolated model l*A + (1-l)*B does not have to be invertible. In particular,
 * the inverse can never be expressed in terms of A^{-1} and B^{-1}, in general.
 * However, in some cases, the action of the inverse on a point can be computed. Subclasses have to provide this
 * behavior as {@link #applyInverseInPlace(double[])}, but should never implement {@link #createInverse()}.
 *
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 */
public class InvertibleInterpolatedModel<
		A extends Model< A > & InvertibleCoordinateTransform,
		B extends Model< B > & InvertibleCoordinateTransform,
		M extends InvertibleInterpolatedModel< A, B, M > > extends InterpolatedModel< A, B, M > implements InvertibleCoordinateTransform
{
	private static final long serialVersionUID = -1800786784345843623L;
	protected static final RuntimeException creatingInverseNotSupportedException = new UnsupportedOperationException(
			"Inverse of an InterpolatedModel cannot be expressed in terms of InterpolatedModel. " +
			"Use applyInverse[InPlace] instead.");

	public InvertibleInterpolatedModel( final A a, final B b, final double lambda )
	{
		super( a, b, lambda );
	}

	@Override
	public M copy()
	{
		@SuppressWarnings( "unchecked" )
		final M copy = ( M )new InvertibleInterpolatedModel< A, B, M >( a.copy(), b.copy(), lambda );
		copy.cost = cost;
		return copy;
	}

	@Override
	public double[] applyInverse( final double[] point ) throws NoninvertibleModelException
	{
		final double[] copy = point.clone();
		applyInverseInPlace( copy );
		return copy;
	}

	@Override
	public void applyInverseInPlace( final double[] point ) throws NoninvertibleModelException
	{
		throw new UnsupportedOperationException("Inverse cannot be applied for general interpolated models. " +
														"Subclasses should implement this behavior if suitable.");
	}

	@Override
	public InvertibleCoordinateTransform createInverse()
	{
		throw creatingInverseNotSupportedException;
	}
}
