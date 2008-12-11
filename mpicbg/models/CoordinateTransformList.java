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
 * @version 0.4b
 */
public class CoordinateTransformList implements CoordinateTransform
{

	final protected List< CoordinateTransform > l = new ArrayList< CoordinateTransform >();
	
	final public void add( CoordinateTransform t ){ l.add( t ); }
	final public void remove( CoordinateTransform t ){ l.remove( t ); }
	final public CoordinateTransform remove( int i ){ return l.remove( i ); }
	final public CoordinateTransform get( int i ){ return l.get( i ); }
	final public void clear(){ l.clear(); }
	
	//@Override
	final public float[] apply( final float[] location )
	{
		final float[] a = location.clone();
		applyInPlace( a );
		return a;
	}

	//@Override
	final public void applyInPlace( final float[] location )
	{
		for ( final CoordinateTransform t : l )
			t.applyInPlace( location );
	}
}
