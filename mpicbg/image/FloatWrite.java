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
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 *
 */
package mpicbg.image;

/**
 * @author saalfeld
 *
 */
abstract public class FloatWrite< C extends Cursor > implements ContainerWrite< C >
{
	final protected float[] a;
	final protected Container container;
	
	/**
	 * Create a {@link FloatWrite FloatWriter} for a specific {@link Container}.
	 * 
	 * @param container
	 */
	public FloatWrite( final Container container )
	{
		this.container = container;
		a = new float[ container.getPixelType().getNumChannels() ];
	}
	
	abstract public void setChannel( C c, int i, float f );
	abstract public void set( C c, float[] f );
	
	
	final public void setChannel( final C c, final int i, final Object f ){ setChannel( c, i, ( Float )f ); }
	final public void setChannel( final C c, final int i, final byte f ){ setChannel( c, i, ( float )f ); }
	final public void setChannel( final C c, final int i, final short f ){ setChannel( c, i, ( float )f ); }
	final public void setChannel( final C c, final int i, final int f ){ setChannel( c, i, ( float )f ); }
	final public void setChannel( final C c, final int i, final long f ){ setChannel( c, i, ( float )f ); }
	final public void setChannel( final C c, final int i, final double f ){ setChannel( c, i, ( float )f ); }

	
	final public void set( final C c, final Object[] f )
	{
		for ( int i = 0; i < a.length; ++i )
			setChannel( c, i, ( Float )a[ i ] );
	}
	final public void set( final C c, final byte[] f )
	{
		for ( int i = 0; i < a.length; ++i )
			setChannel( c, i, ( float )a[ i ] );
	}
	final public void set( final C c, final short[] f )
	{
		for ( int i = 0; i < a.length; ++i )
			setChannel( c, i, ( float )a[ i ] );
	}
	final public void set( final C c, final int[] f )
	{
		for ( int i = 0; i < a.length; ++i )
			setChannel( c, i, ( float )a[ i ] );
	}
	final public void set( final C c, final long[] f )
	{
		for ( int i = 0; i < a.length; ++i )
			setChannel( c, i, ( float )a[ i ] );
	}
	final public void set( final C c, final double[] f )
	{
		for ( int i = 0; i < a.length; ++i )
			setChannel( c, i, ( float )a[ i ] );
	}
}
