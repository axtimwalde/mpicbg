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
package mpicbg.models;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 *
 */
public class InverseCoordinateTransformList implements InverseCoordinateTransform
{

	final private List< InverseCoordinateTransform > l = new ArrayList< InverseCoordinateTransform >();
	
	final public void add( InverseCoordinateTransform t ){ l.add( t ); }
	final public void remove( InverseCoordinateTransform t ){ l.remove( t ); }
	final public InverseCoordinateTransform remove( int i ){ return l.remove( i ); }
	final public InverseCoordinateTransform get( int i ){ return l.get( i ); }
	final public void clear(){ l.clear(); }
	
	final public float[] applyInverse( float[] location ) throws NoninvertibleModelException
	{
		float[] a = location.clone();
		applyInverseInPlace( a );
		return a;
	}

	final public void applyInverseInPlace( float[] location ) throws NoninvertibleModelException
	{
		for ( InverseCoordinateTransform t : l )
			t.applyInverseInPlace( location );
	}
}
