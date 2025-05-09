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

import ij.process.ImageProcessor;
import mpicbg.models.InverseCoordinateTransform;
import mpicbg.models.NoninvertibleModelException;

/**
 * Use an {@link InverseCoordinateTransform} to map
 * {@linkplain ImageProcessor source} into {@linkplain ImageProcessor target}
 * which is a {@link Mapping}.
 *
 * @author Stephan Saalfeld &lt;saalfeld@mpi-cbg.de&gt;
 * @version 0.1b
 */
public class InverseTransformMapping< T extends InverseCoordinateTransform > implements Mapping< T >
{
	final protected T transform;
	@Override
	final public T getTransform(){ return transform; }

	public InverseTransformMapping( final T t )
	{
		this.transform = t;
	}

	@Override
	public void map(
			final ImageProcessor source,
			final ImageProcessor target )
	{
		final double[] t = new double[ 2 ];
		final int sw = source.getWidth() - 1;
		final int sh = source.getHeight() - 1;
		final int tw = target.getWidth();
		final int th = target.getHeight();
		for ( int y = 0; y < th; ++y )
		{
			for ( int x = 0; x < tw; ++x )
			{
				t[ 0 ] = x;
				t[ 1 ] = y;
				try
				{
					transform.applyInverseInPlace( t );
					final int tx = ( int )( t[ 0 ] + 0.5f );
					final int ty = ( int )( t[ 1 ] + 0.5f );
					if (
							tx >= 0 &&
							tx <= sw &&
							ty >= 0 &&
							ty <= sh )
						target.putPixel( x, y, source.getPixel( tx, ty ) );
				}
				catch ( final NoninvertibleModelException e ){}
			}
		}
	}

	@Override
	public void mapInterpolated(
			final ImageProcessor source,
			final ImageProcessor target )
	{
		final double[] t = new double[ 2 ];
		final int sw = source.getWidth() - 1;
		final int sh = source.getHeight() - 1;
		final int tw = target.getWidth();
		final int th = target.getHeight();
		for ( int y = 0; y < th; ++y )
		{
			for ( int x = 0; x < tw; ++x )
			{
				t[ 0 ] = x;
				t[ 1 ] = y;
				try
				{
					transform.applyInverseInPlace( t );
					if (
							t[ 0 ] >= 0 &&
							t[ 0 ] <= sw &&
							t[ 1 ] >= 0 &&
							t[ 1 ] <= sh )
						target.putPixel( x, y, source.getPixelInterpolated( t[ 0 ], t[ 1 ] ) );
				}
				catch ( final NoninvertibleModelException e ){}
			}
		}
	}
}
