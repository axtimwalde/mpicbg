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
import mpicbg.models.InverseCoordinateTransform;
import mpicbg.models.NoninvertibleModelException;

/**
 * Use an {@link InverseCoordinateTransform} to map {@linkplain ImageStack
 * source} into {@linkplain ImageProcessor target} which is a projective
 * {@link Mapping}.
 *
 * Bilinear interpolation is supported.
 *
 * @author Stephan Saalfeld &lt;saalfeld@mpi-cbg.de&gt;
 * @version 0.1b
 */
public class InverseTransformMapping< T extends InverseCoordinateTransform > extends AbstractTransformMapping< T >
{
	public InverseTransformMapping( final T t )
	{
		super( t );
	}

	@Override
	public void map( final ImageStack source, final ImageProcessor target )
	{
		final double[] t = new double[ 3 ];
		final int sw = source.getWidth() - 1;
		final int sh = source.getHeight() - 1;
		final int sd = source.getSize();
		final int tw = target.getWidth();
		final int th = target.getHeight();

		/* ImageJ creates a !NEW! ImageProcessor for each call to getProcessor() */
		final ImageProcessor slice = source.getProcessor( 1 );

		for ( int y = 0; y < th; ++y )
		{
			for ( int x = 0; x < tw; ++x )
			{
				t[ 0 ] = x;
				t[ 1 ] = y;
				t[ 2 ] = z;
				try
				{
					transform.applyInverseInPlace( t );
					final int tx = ( int ) ( t[ 0 ] + 0.5f );
					final int ty = ( int ) ( t[ 1 ] + 0.5f );
					final int tz = ( int ) ( t[ 2 ] + 1.5f );
					if ( tx >= 0 && tx <= sw && ty >= 0 && ty <= sh && tz >= 1 && tz <= sd )
					{
						slice.setPixels( source.getPixels( tz ) );
						target.putPixel( x, y, slice.getPixel( tx, ty ) );
					}
				}
				catch ( final NoninvertibleModelException e )
				{}
			}
		}
	}

	@Override
	public void mapInterpolated( final ImageStack source, final ImageProcessor target )
	{
		final double[] t = new double[ 3 ];
		final int sw = source.getWidth() - 1;
		final int sh = source.getHeight() - 1;
		final int sd = source.getSize();
		final int tw = target.getWidth();
		final int th = target.getHeight();

		/* ImageJ creates a !NEW! ImageProcessor for each call to getProcessor() */
		final ImageProcessor slice = source.getProcessor( 1 );
		slice.setInterpolationMethod( ImageProcessor.BILINEAR );

		final Interpolator interpolator = pickInterpolator( slice );

		for ( int y = 0; y < th; ++y )
		{
			for ( int x = 0; x < tw; ++x )
			{
				t[ 0 ] = x;
				t[ 1 ] = y;
				t[ 2 ] = z;
				try
				{
					transform.applyInverseInPlace( t );
					final int tza = ( int ) ( t[ 2 ] + 1.0f );
					final int tzb = ( int ) ( t[ 2 ] + 2.0f );
					if ( t[ 0 ] >= 0 && t[ 0 ] <= sw && t[ 1 ] >= 0 && t[ 1 ] <= sh && tza >= 1 && tzb <= sd )
					{
						slice.setPixels( source.getPixels( tza ) );
						final int a = slice.getPixelInterpolated( t[ 0 ], t[ 1 ] );
						slice.setPixels( source.getPixels( tzb ) );
						final int b = slice.getPixelInterpolated( t[ 0 ], t[ 1 ] );

						target.putPixel( x, y, interpolator.interpolate( a, b, t[ 2 ] - tza + 1.0f ) );
					}
				}
				catch ( final NoninvertibleModelException e )
				{}
			}
		}
	}
}
