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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * An {@link InverseCoordinateTransform} that is saved as a LUT on integer
 * coordinates.
 *
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
public class InverseCoordinateTransformMap2D implements InverseCoordinateTransform
{
	private static final long serialVersionUID = 6356069579515246368L;

	/**
	 * target coordinates addressed interleaved as
	 * map[ row ][ 2 * column ] = x
	 * map[ row ][ 2 * column + 1 ] = y
	 */
	final protected float[][] map;

	final protected int width;
	final protected int height;

	final public int getWidth(){ return width; }
	final public int getHeight(){ return height; }

	public InverseCoordinateTransformMap2D( final float[][] map )
	{
		this.map = map;
		this.width = map[ 0 ].length / 2;
		this.height = map.length;
	}

	public InverseCoordinateTransformMap2D( final InverseCoordinateTransform t, final int width, final int height )
	{
		this.width = width;
		this.height = height;
		map = new float[ height ][ width * 2 ];
		final double[] l = new double[ 2 ];
		for ( int y = 0; y < height; ++y )
		{
			for ( int x = 0; x < width; ++x )
			{
				l[ 0 ] = x;
				l[ 1 ] = y;
				try
				{
					t.applyInverseInPlace( l );
					map[ y ][ 2 * x ] = ( float )l[ 0 ];
					map[ y ][ 2 * x + 1 ] = ( float )l[ 1 ];
				}
				catch ( final NoninvertibleModelException e )
				{
					map[ y ][ 2 * x ] = Float.NaN;
					map[ y ][ 2 * x + 1 ] = Float.NaN;
				}
			}
		}
	}

	public InverseCoordinateTransformMap2D( final FileInputStream fis ) throws IOException
	{
		final byte[] header = new byte[ 8 ];
		fis.read( header );

		/*
		 * Unsigned integers have to be calculated as long thanks Java's lack
		 * of unsigned basic types.
		 */
		final long longWidth = ( ( 0xffl & header[ 0 ] ) << 24 ) + ( ( 0xffl & header[ 1 ] ) << 16 ) + ( ( 0xffl & header[ 2 ] ) << 8 ) + ( 0xffl & header[ 3 ] );
		this.width = ( int )longWidth;
		final long longHeight = ( ( 0xffL & header[ 4 ] ) << 24 ) + ( ( 0xffL & header[ 5 ] ) << 16 ) + ( ( 0xffL & header[ 6 ] ) << 8 ) + ( 0xffL & header[ 7 ] );
		this.height = ( int )longHeight;

		map = new float[ height ][ 2 * width ];

		final byte[] byteRow = new byte[ width * 8 ];
		for ( int y = 0; y < height; ++y )
		{
			fis.read( byteRow );

			final float[] row = map[ y ];
			for ( int i = 0; i < row.length; i += 2 )
			{
				final int j = 4 * i;
				final int tx =
					( ( 0xff & byteRow[ j ] ) << 24 ) |
					( ( 0xff & byteRow[ j + 1 ] ) << 16 ) |
					( ( 0xff & byteRow[ j + 2 ] ) << 8 ) |
					( 0xff & byteRow[ j + 3 ] );
				final int ty =
					( ( 0xff & byteRow[ j + 4 ] ) << 24 ) |
					( ( 0xff & byteRow[ j + 5 ] ) << 16 ) |
					( ( 0xff & byteRow[ j + 6 ] ) << 8 ) |
					( 0xff & byteRow[ j + 7 ] );

				row[ i ] = Float.intBitsToFloat( tx );
				row[ i + 1 ] = Float.intBitsToFloat( ty );
			}
		}
	}

	public void export( final FileOutputStream fos ) throws IOException
	{
		final byte[] header = new byte[ 8 ];

		header[ 0 ] = ( byte )( width >> 24 );
		header[ 1 ] = ( byte )( width >> 16 );
		header[ 2 ] = ( byte )( width >> 8 );
		header[ 3 ] = ( byte )width;

		header[ 4 ] = ( byte )( height >> 24 );
		header[ 5 ] = ( byte )( height >> 16 );
		header[ 6 ] = ( byte )( height >> 8 );
		header[ 7 ] = ( byte )height;

		fos.write( header );

		final byte[] byteRow = new byte[ width * 8 ];
		for ( int y = 0; y < height; ++y )
		{
			final float[] row = map[ y ];
			for ( int i = 0; i < row.length; i += 2 )
			{
				final int j = 4 * i;

				final int tx = Float.floatToIntBits( row[ i ] );
				final int ty = Float.floatToIntBits( row[ i + 1 ] );

				byteRow[ j ] = ( byte )( tx >> 24 );
				byteRow[ j + 1 ] = ( byte )( tx >> 16 );
				byteRow[ j + 2 ] = ( byte )( tx >> 8 );
				byteRow[ j + 3 ] = ( byte )tx;

				byteRow[ j + 4 ] = ( byte )( ty >> 24 );
				byteRow[ j + 5 ] = ( byte )( ty >> 16 );
				byteRow[ j + 6 ] = ( byte )( ty >> 8 );
				byteRow[ j + 7 ] = ( byte )ty;
			}

			fos.write( byteRow );
		}
		fos.close();
	}

	@Override
	public double[] applyInverse( final double[] location )
	{
		final double[] t = location.clone();
		applyInverseInPlace( t );
		return t;
	}

	@Override
	public void applyInverseInPlace( final double[] location )
	{
		final int ix = 2 * ( int )location[ 0 ];
		final int iy = ( int )location[ 1 ];
		location[ 0 ] = map[ iy ][ ix ];
		location[ 1 ]= map[ iy ][ ix + 1 ];
	}
}
