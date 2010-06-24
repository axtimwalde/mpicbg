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
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/**
 * Diffuse
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1a
 */
public class DiffusionMask implements PlugIn
{
	/**
	 * 
	 * @param ip
	 * @param v
	 */
	public static void run(
			final ImageProcessor ip,
			final FloatProcessor mask,
			final int n )
	{
		final int w = Math.min( ip.getWidth(), mask.getWidth() );
		final int h = Math.min( ip.getHeight(), mask.getHeight() );
		final int wh = w * h;
		
		final ImageProcessor ipTarget = ip.duplicate();
		final FloatProcessor maskTarget = ( FloatProcessor )mask.duplicate();
		
		int numSaturatedPixels = n;
		
		do
		{
			int ri0 = 0;
			int ri1 = w;
			int ri2 = 2 * w;
			
			while ( ri2 < wh )
			{
				int i00 = ri0;
				int i01 = ri0 + 1;
				int i02 = ri0 + 2;
				
				int i10 = ri1;
				int i11 = ri1 + 1;
				int i12 = ri1 + 2;
				
				int i20 = ri2;
				int i21 = ri2 + 1;
				int i22 = ri2 + 2;
				
				while ( i02 < ri1 )
				{
					final float m00 = mask.getf( i00 );
					final float m01 = mask.getf( i01 );
					final float m02 = mask.getf( i02 );
					
					final float m10 = mask.getf( i10 );
					final float m11 = mask.getf( i11 );
					final float m12 = mask.getf( i12 );
					
					final float m20 = mask.getf( i20 );
					final float m21 = mask.getf( i21 );
					final float m22 = mask.getf( i22 );
					
					final float mm11 = 1.0f - m11;
					
					final float mm00 = mm11 * m00 * 0.25f;
					final float mm01 = mm11 * m01 * 0.5f;
					final float mm02 = mm11 * m02 * 0.25f;
					
					final float mm10 = mm11 * m10 * 0.5f;
					final float mm12 = mm11 * m12 * 0.5f;
					
					final float mm20 = mm11 * m20 * 0.25f;
					final float mm21 = mm11 * m21 * 0.5f;
					final float mm22 = mm11 * m22 * 0.25f;
					
					
					final float s = mm00 + mm01 + mm02 + mm10 + mm11 + mm12 + mm20 + mm21 + mm22;
					
					if ( s != 0 )
					{
						ipTarget.setf(
								i11, 
								( ip.getf( i00 ) * mm00 + ip.getf( i01 ) * mm01 + ip.getf( i02 ) * mm02 +
								  ip.getf( i10 ) * mm10 + ip.getf( i11 ) * mm11 + ip.getf( i12 ) * mm12 +
								  ip.getf( i20 ) * mm20 + ip.getf( i21 ) * mm21 + ip.getf( i22 ) * mm22 ) / s );
						
						maskTarget.setf(
								i11, 
								( m00 * mm00 + m01 * mm01 + m02 * mm02 +
								  m10 * mm10 + m11 * mm11 + m12 * mm12 +
								  m20 * mm20 + m21 * mm21 + m22 * mm22 ) / s );
					}
					
					++i00;
					++i01;
					++i02;
					
					++i10;
					++i11;
					++i12;
					
					++i20;
					++i21;
					++i22;
				}
				
				IJ.log( "row: " + ( ri1 / w ) );
				
				ri0 += w;
				ri1 += w;
				ri2 += w;
			}
			IJ.log( "" + ( ri1 / w ) );
			
			System.arraycopy( ipTarget.getPixels(), 0, ip.getPixels(), 0, wh );
			System.arraycopy( maskTarget.getPixels(), 0, mask.getPixels(), 0, wh );
			
			--numSaturatedPixels;
		}
		while ( numSaturatedPixels > 0 );
	}

	@Override
	public void run( String arg )
	{
		final ImagePlus imp = IJ.getImage();
		run( IJ.getImage().getProcessor(), ( FloatProcessor )IJ.getImage().getProcessor().convertToFloat(), 5 );
		imp.updateAndDraw();
		
	}

}
