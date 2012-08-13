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
 */
package mpicbg.ij.plugin;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

/**
 * Remove saturated pixels by diffusing the neighbors in.
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1a
 */
public class RemoveSaturated implements PlugIn
{
	/**
	 * 
	 * @param ip
	 * @param v
	 */
	public static void run(
			final ImageProcessor ip,
			final float v )
	{
		final int w = ip.getWidth();
		final int h = ip.getHeight();
		final int wh = w * h;
		
		final ImageProcessor ipTarget = ip.duplicate();
		
		int numSaturatedPixels = 0;
		do
		{
			numSaturatedPixels = 0;
			for ( int i = 0; i < wh; ++i )
				if ( ip.getf( i ) == v )
				{
					++numSaturatedPixels;
					
					final int y = i / w;
					final int x = i % w;
					
					float s = 0;
					float n = 0;
					if ( y > 0 )
					{
						if ( x > 0 )
						{
							final float tl = ip.getf( x - 1, y - 1 );
							if ( tl != v )
							{
								s += 0.5f * tl;
								n += 0.5f;
							}
						}
						final float t = ip.getf( x, y - 1 );
						if ( t != v )
						{
							s += t;
							n += 1;
						}
						if ( x < w - 1 )
						{
							final float tr = ip.getf( x + 1, y - 1 );
							if ( tr != v )
							{
								s += 0.5f * tr;
								n += 0.5f;
							}
						}
					}
					
					if ( x > 0 )
					{
						final float l = ip.getf( x - 1, y );
						if ( l != v )
						{
							s += l;
							n += 1;
						}
					}
					if ( x < w - 1 )
					{
						final float r = ip.getf( x + 1, y );
						if ( r != v )
						{
							s += r;
							n += 1;
						}
					}
					
					if ( y < h - 1 )
					{
						if ( x > 0 )
						{
							final float bl = ip.getf( x - 1, y + 1 );
							if ( bl != v )
							{
								s += 0.5f * bl;
								n += 0.5f;
							}
						}
						final float b = ip.getf( x, y + 1 );
						if ( b != v )
						{
							s += b;
							n += 1;
						}
						if ( x < w - 1 )
						{
							final float br = ip.getf( x + 1, y + 1 );
							if ( br != v )
							{
								s += 0.5f * br;
								n += 0.5f;
							}
						}
					}
					
					if ( n > 0 )
						ipTarget.setf( i, s / n );
					
				}
			ip.setPixels( ipTarget.getPixelsCopy() );
		}
		while ( numSaturatedPixels > 0 );
	}

	@Override
	public void run( String arg )
	{
		final ImagePlus imp = IJ.getImage();
		run( IJ.getImage().getProcessor(), 0 );
		imp.updateAndDraw();
		
	}

}
