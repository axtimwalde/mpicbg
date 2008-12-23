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

import mpicbg.models.AbstractAffineModel2D;
import mpicbg.models.Tile;

/**
 * @version 0.1b
 */
public class AffineTile< A extends AbstractAffineModel2D< A > > extends Tile< A >
{
	final protected float width;
	final public float getWidth(){ return width; }
	
	final protected float height;
	final public float getHeight(){ return height; }
	
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
