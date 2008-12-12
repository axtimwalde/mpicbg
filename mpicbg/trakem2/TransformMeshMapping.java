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

import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Set;

import mpicbg.models.AffineModel2D;

/**
 *
 * @version 0.1b
 */
public class TransformMeshMapping extends mpicbg.ij.TransformMeshMapping
{
	public TransformMeshMapping( final TransformMesh t )
	{
		super( t );
	}
	
	final public ImageProcessor createMappedImage( final ImageProcessor source )
	{
		Rectangle boundingBox = ( ( TransformMesh )transform ).getBoundingBox();
		final ImageProcessor target = source.createProcessor( boundingBox.width, boundingBox.height );
		target.setColor( Color.black );
		target.fill();
		final Set< AffineModel2D > s = transform.getAV().keySet();
		for ( final AffineModel2D ai : s )
			mapTriangle( transform, ai, source, target );
		return target;
	}
}
