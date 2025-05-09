/*-
 * #%L
 * MPICBG Core Library.
 * %%
 * Copyright (C) 2008 - 2025 Stephan Saalfeld et. al.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package mpicbg.ij;

import ij.gui.GenericDialog;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.util.Collection;

import mpicbg.imagefeatures.Feature;
import mpicbg.imagefeatures.Filter;
import mpicbg.imagefeatures.FloatArray2D;
import mpicbg.imagefeatures.FloatArray2DMOPS;
import mpicbg.imagefeatures.FloatArray2DScaleOctave;
import mpicbg.imagefeatures.ImageArrayConverter;

/**
 * 
 * @author Stephan Saalfeld &lt;saalfeld@mpi-cbg.de&gt;
 * @version 0.1b
 */
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
	
	final static public void addFields( final GenericDialog gd, final FloatArray2DMOPS.Param p )
	{
		gd.addMessage( "Scale Invariant Interest Point Detector:" );
		gd.addNumericField( "initial_gaussian_blur :", p.initialSigma, 2, 6, "px" );
		gd.addNumericField( "steps_per_scale_octave :", p.steps, 0 );
		gd.addNumericField( "minimum_image_size :", p.minOctaveSize, 0, 6, "px" );
		gd.addNumericField( "maximum_image_size :", p.maxOctaveSize, 0, 6, "px" );
		
		gd.addMessage( "Feature Descriptor:" );
		gd.addNumericField( "feature_descriptor_size :", p.fdSize, 0 );
	}
	
	final static public void readFields( final GenericDialog gd, final FloatArray2DMOPS.Param p )
	{
		p.initialSigma = ( float )gd.getNextNumber();
		p.steps = ( int )gd.getNextNumber();
		p.minOctaveSize = ( int )gd.getNextNumber();
		p.maxOctaveSize = ( int )gd.getNextNumber();
		p.fdSize = ( int )gd.getNextNumber();
	}

	/**
	 * Extract MOPS features from an {@link ImageProcessor}
	 * 
	 * @param ip
	 * @param features the list to be filled
	 */
	@Override
	final public void extractFeatures( final ImageProcessor ip, final Collection< Feature > features )
	{
		/* make sure that integer rounding does not result in an image of t.getMaxOctaveSize() + 1 */
		final float maxSize = t.getMaxOctaveSize() - 1;
		float scale = 1.0f;
		FloatArray2D fa;
		if ( maxSize < ip.getWidth() || maxSize < ip.getHeight() )
		{
			/* scale the image respectively */
			scale = ( float )Math.min( maxSize / ip.getWidth(), maxSize / ip.getHeight() );
			final FloatProcessor fp = ( FloatProcessor )ip.convertToFloat();
			fp.setMinAndMax( ip.getMin(), ip.getMax() );
			final FloatProcessor ipScaled = mpicbg.ij.util.Filter.createDownsampled( fp, scale, 0.5f, 0.5f );
			fa = new FloatArray2D( ipScaled.getWidth(), ipScaled.getHeight() );
			ImageArrayConverter.imageProcessorToFloatArray2DCropAndNormalize( ipScaled, fa );
		}
		else
		{
			fa = new FloatArray2D( ip.getWidth(), ip.getHeight() );
			ImageArrayConverter.imageProcessorToFloatArray2DCropAndNormalize( ip, fa );
		}
		
		final float[] initialKernel;
		
		final float initialSigma = t.getInitialSigma();
		if ( initialSigma < 1.0 )
		{
			scale *= 2.0f;
			t.setInitialSigma( initialSigma * 2 );
			final FloatArray2D fat = new FloatArray2D( fa.width * 2 - 1, fa.height * 2 - 1 ); 
			FloatArray2DScaleOctave.upsample( fa, fat );
			
			fa = fat;
			initialKernel = Filter.createGaussianKernel( ( float )Math.sqrt( t.getInitialSigma() * t.getInitialSigma() - 1.0 ), true );
		}
		else
			initialKernel = Filter.createGaussianKernel( ( float )Math.sqrt( initialSigma * initialSigma - 0.25 ), true );
		
		fa = Filter.convolveSeparable( fa, initialKernel, initialKernel );
		
		t.init( fa );
		t.extractFeatures( features );
		if ( scale != 1.0f )
		{
			for ( Feature f : features )
			{
				f.scale /= scale;
				f.location[ 0 ] /= scale;
				f.location[ 1 ] /= scale;
			}
			t.setInitialSigma( initialSigma );
		}
	} 
}
