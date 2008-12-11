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
import java.util.ListIterator;

/**
 * 
 * @version 0.4b
 */
public class InvertibleCoordinateTransformList implements InvertibleCoordinateTransform
{

	final protected List< InvertibleCoordinateTransform > l = new ArrayList< InvertibleCoordinateTransform >();
	
	public void add( InvertibleCoordinateTransform t ){ l.add( t ); }
	public void remove( InvertibleCoordinateTransform t ){ l.remove( t ); }
	public InvertibleCoordinateTransform remove( int i ){ return l.remove( i ); }
	public InvertibleCoordinateTransform get( int i ){ return l.get( i ); }
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
	
	//@Override
	final public float[] applyInverse( float[] location ) throws NoninvertibleModelException
	{
		final float[] a = location.clone();
		applyInverseInPlace( a );
		return a;
	}

	//@Override
	final public void applyInverseInPlace( float[] location ) throws NoninvertibleModelException
	{
		final ListIterator< InvertibleCoordinateTransform > i = l.listIterator( l.size() );
		while ( i.hasPrevious() )
			i.previous().applyInverseInPlace( location );
	}
}
