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

import ij.process.ByteProcessor;
import ini.trakem2.display.Patch;

import java.awt.geom.AffineTransform;
import java.util.HashSet;
import java.util.Set;

import mpicbg.models.AbstractAffineModel2D;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import mpicbg.models.TileConfiguration;

/**
 * @version 0.1b
 */
abstract public class AbstractAffineTile2D< A extends AbstractAffineModel2D< A > > extends mpicbg.models.Tile< A >
{
	final protected Patch patch;
	final public Patch getPatch(){ return patch; }
	final public double getWidth(){ return patch.getWidth(); }
	final public double getHeight(){ return patch.getHeight(); }
	
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
	
	/**
	 * Try to find the tile which is connected by a particular
	 * {@link PointMatch}.
	 * 
	 * Note that this method searches only the known connected tiles to limit
	 * the cost of that anyway expensive search.
	 * 
	 * @param match
	 * 
	 * @return connectedTile or null
	 */
	final public AbstractAffineTile2D< ? > findConnectedTile( PointMatch match )
	{
		final Point p = match.getP2();
		for ( final mpicbg.models.Tile< ? > t : connectedTiles )
		{
			for ( final PointMatch m : t.getMatches() )
			{
				if ( p == m.getP1() ) return ( AbstractAffineTile2D< ? > )t;
			}
		}
		return null;
	}
	
	abstract protected void initModel();
	
	public AbstractAffineTile2D( final A model, final Patch patch )
	{
		super( model );
		this.patch = patch;
		initModel();
	}
	
	final public AffineTransform createAffine()
	{
		return model.createAffine();
	}
	
	final public void updatePatch()
	{
		patch.setAffineTransform( createAffine() );
		patch.updateMipmaps();
	}
	
	final public ByteProcessor createMaskedByteImage()
	{
		final ByteProcessor mask;
		final Patch.PatchImage pai = patch.createTransformedImage();
		if ( pai.mask == null )
			mask = pai.outside;
		else
			mask = pai.mask;
		
		final ByteProcessor target = ( ByteProcessor )pai.target.convertToByte( true );
		
		if ( mask != null )
		{
			final byte[] targetBytes = ( byte[] )target.getPixels();
			final byte[] maskBytes = (byte[])mask.getPixels();
			
			if ( pai.outside != null )
			{
				final byte[] outsideBytes = (byte[])pai.outside.getPixels();
				for ( int i = 0; i < outsideBytes.length; ++i )
				{
					if ( ( outsideBytes[ i ]&0xff ) != 255 ) maskBytes[ i ] = 0;
					final float a = ( float )( maskBytes[ i ] & 0xff ) / 255f;
					final int t = ( targetBytes[ i ] & 0xff );
					targetBytes[ i ] = ( byte )( t * a + 127 * ( 1 - a ) );
				}
			}
			else
			{
				for ( int i = 0; i < targetBytes.length; ++i )
				{
					final float a = ( float )( maskBytes[ i ] & 0xff ) / 255f;
					final int t = ( targetBytes[ i ] & 0xff );
					targetBytes[ i ] = ( byte )( t * a + 127 * ( 1 - a ) );
				}
			}
		}
		
		return target;
	}
	
	final public boolean intersects( AbstractAffineTile2D< ? > t )
	{
		return patch.intersects( t.patch );
	}
}
