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

import java.awt.geom.AffineTransform;
import java.util.HashSet;
import java.util.Set;

import mpicbg.models.AbstractAffineModel2D;
import mpicbg.models.PointMatch;
import mpicbg.models.Tile;
import mpicbg.models.TileConfiguration;

/**
 * @version 0.1b
 */
public class AffineTile< A extends AbstractAffineModel2D< A > > extends Tile< A >
{
	final protected float width;
	final public float getWidth(){ return width; }
	
	final protected float height;
	final public float getHeight(){ return height; }
	
	/**
	 * A set of virtual point correspondences that are used to connect a tile
	 * to the rest of the {@link TileConfiguration} assuming that the initial
	 * layout was correct.
	 * 
	 * Virtual point correspondences are also stored in matches.  This is just
	 * to keep track about them.
	 * 
	 * Virtual point correspondences have to be removed
	 * for real connections.
	 */
	final protected Set< PointMatch > virtualMatches = new HashSet< PointMatch >();
	final public Set< PointMatch > getVirtualMatches(){ return virtualMatches; }
	
	final public boolean addVirtualMatch( final PointMatch match )
	{
		if ( virtualMatches.add( match ) )
			return matches.add( match );
		return false;
	}
	
	final public boolean removeVirtualMatch( final PointMatch match )
	{
		if ( virtualMatches.remove( match ) )
			return matches.remove( match );
		return false;
	}
	
	/**
	 * Remove all virtual matches
	 * 
	 * @return success
	 */
	final public void clearVirtualMatches()
	{
		for ( PointMatch m : virtualMatches )
			matches.remove( m );
		virtualMatches.clear();
	}
	
	public AffineTile( final A model, final float width, final float height )
	{
		super( model );
		this.width = width;
		this.height = height;
	}
	
	final public AffineTransform createAffine()
	{
		return model.createAffine();
	}
}
