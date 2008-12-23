/**
 *  License: GPL
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
 * NOTE:
 * The SIFT-method is protected by U.S. Patent 6,711,293: "Method and
 * apparatus for identifying scale invariant features in an image and use of
 * same for locating an object in an image" by the University of British
 * Columbia.  That is, for commercial applications the permission of the author
 * is required.
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.4b
 */
package mpicbg.ij;

import ij.process.ImageProcessor;

import java.util.List;

import mpicbg.imagefeatures.Feature;
import mpicbg.imagefeatures.Filter;
import mpicbg.imagefeatures.FloatArray2D;
import mpicbg.imagefeatures.FloatArray2DMOPS;
import mpicbg.imagefeatures.FloatArray2DScaleOctave;
import mpicbg.imagefeatures.ImageArrayConverter;

public class MOPS extends FeatureTransform< FloatArray2DMOPS >
{
	/**
	 * Constructor
	 * 
	 * @param t feature transformation
	 */
	public MOPS( final FloatArray2DMOPS t )
	{
		super( t );
	}
	
	/**
	 * Extract MOPS features from an ImageProcessor
	 * 
	 * @param ip
	 * @param features the list to be filled
	 * 
	 * @return number of detected features
	 */
	@Override
	final public void extractFeatures( final ImageProcessor ip, final List< Feature > features )
	{
		FloatArray2D fa = new FloatArray2D( ip.getWidth(), ip.getHeight() );
		ImageArrayConverter.imageProcessorToFloatArray2D( ip, fa );
		Filter.enhance( fa, 1.0f );
		
		final float[] initialKernel;
		final boolean upscale;
		
		final float initialSigma = t.getInitialSigma();
		if ( initialSigma < 1.0 )
		{
			upscale = true;
			t.setInitialSigma( initialSigma * 2 );
			final FloatArray2D fat = new FloatArray2D( fa.width * 2 - 1, fa.height * 2 - 1 ); 
			FloatArray2DScaleOctave.upsample( fa, fat );
			
			fa = fat;
			initialKernel = Filter.createGaussianKernel( ( float )Math.sqrt( t.getInitialSigma() * t.getInitialSigma() - 1.0 ), true );
		}
		else
		{
			upscale = false;
			initialKernel = Filter.createGaussianKernel( ( float )Math.sqrt( initialSigma * initialSigma - 0.25 ), true );
		}
		
		fa = Filter.convolveSeparable( fa, initialKernel, initialKernel );
		
		t.init( fa );
		t.extractFeatures( features );
		if ( upscale )
		{
			for ( Feature f : features )
			{
				f.scale /= 2;
				f.location[ 0 ] /= 2;
				f.location[ 1 ] /= 2;
			}
			t.setInitialSigma( initialSigma );
		}
	} 
}
