/*-
 * #%L
 * MPICBG plugin for Fiji.
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



/**
 * Sample variance block filter.
 *
 * @author Stephan Saalfeld &lt;saalfeld@mpi-cbg.de&gt;
 * @version 0.1a
 */
public class SampleVariance extends AbstractBlockStatistics
{
	@Override
	protected String dialogTitle()
	{
		return "Sample Variance";
	}
	
	@Override
	protected void toRGB( final int[] rgbs )
	{
		final float[] rs = ( float[] )fps[ 0 ].getPixels();
		final float[] gs = ( float[] )fps[ 1 ].getPixels();
		final float[] bs = ( float[] )fps[ 2 ].getPixels();
		
		for ( int i = 0; i < rgbs.length; ++i )
		{
			final int r = Math.min( 255, Math.round( rs[ i ] ) );
			final int g = Math.min( 255, Math.round( gs[ i ] ) );
			final int b = Math.min( 255, Math.round( bs[ i ] ) );
			
			/* preserves alpha even though ImageJ ignores it */
			rgbs[ i ] = ( rgbs[ i ] & 0xff000000 ) | ( ( ( ( r << 8 ) | g ) << 8 ) | b );
		}
	}
	
	@Override
	protected void toByte( final byte[] bytes )
	{
		final float[] fs = ( float[] )fps[ 0 ].getPixels();
		for ( int i = 0; i < bytes.length; ++i )
			bytes[ i ] = ( byte )Math.min( 255, Math.round( fs[ i ] ) );
	}
	
	@Override
	protected void toShort( final short[] shorts )
	{
		final float[] fs = ( float[] )fps[ 0 ].getPixels();
		for ( int i = 0; i < shorts.length; ++i )
			shorts[ i ] = ( short )Math.min( 65535, Math.round( fs[ i ] ) );
	}
	
	@Override
	protected void process( final int i )
	{
		bss[ i ].sampleVariance( brx, bry );
	}
}
