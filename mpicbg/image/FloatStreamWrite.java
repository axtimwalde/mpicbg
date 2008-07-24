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
 * @author Preibisch & saalfeld
 *
 */
public class FloatStreamWrite extends FloatWrite< StreamCursor >
{
	final FloatStream stream;

	public FloatStreamWrite( final FloatStream stream )
	{
		super( stream );
		this.stream = stream;
	}

	@Override
	final public void setChannel( final StreamCursor c, final int i, final float f )
	{
		stream.data[ c.getStreamIndex() + i ] = f;
	}
	
	@Override
	final public void set( final StreamCursor c, final float[] f )
	{
		for ( int i = 0; i < f.length; i++ )
			setChannel( c, i, f[ i ] );
	}
}
