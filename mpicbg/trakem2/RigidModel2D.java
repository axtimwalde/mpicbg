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
package mpicbg.trakem2;

public class RigidModel2D extends mpicbg.models.RigidModel2D implements InvertibleCoordinateTransform
{

	//@Override
	final public void init( final String data )
	{
		final String[] fields = data.split( "\\s+" );
		if ( fields.length == 3 )
		{
			final float theta = Float.parseFloat( fields[ 0 ] );
			final float tx = Float.parseFloat( fields[ 1 ] );
			final float ty = Float.parseFloat( fields[ 2 ] );
			set( theta, tx, ty );
		}
		else throw new NumberFormatException( "Inappropriate parameters for " + this.getClass().getCanonicalName() );
	}

	//@Override
	final public String toXML()
	{
		return "<ict_transform class=\"" + this.getClass().getCanonicalName() + "\" data=\"" + toDataString() + "\"/>";
	}
	
	//@Override
	final public String toDataString()
	{
		return Math.atan2( sin, cos ) + " " + tx + " " + ty;
	}
}
