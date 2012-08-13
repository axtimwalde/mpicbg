package mpicbg.imagefeatures;

/**
 * Simple 2d float array that stores all values in one linear array.
 * 
 * <p>License: GPL
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
 * @author Stephan Preibisch (and Stephan Saalfeld the deleter)
 * @version 0.2b
 */
public class FloatArray2D extends FloatArray
{
	final public int width;
	final public int height;

	public FloatArray2D( final int width, final int height )
	{
		data = new float[ width * height ];
		this.width = width;
		this.height = height;
	}

	public FloatArray2D( final float[] data, final int width, final int height )
	{
		this.data = data;
		this.width = width;
		this.height = height;
	}

	public FloatArray2D clone()
	{
		FloatArray2D clone = new FloatArray2D( data.clone(), width, height );
		return clone;
	}

	final public float get( final int x, final int y )
	{
		return data[ y * width + x ];
	}

	final public void set( final float value, final int x, final int y )
	{
		data[ y * width + x ] = value;
	}
}
