package mpicbg.ij;

import ij.gui.GenericDialog;
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
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
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
	 * Extract MOPS features from an ImageProcessor
	 * 
	 * @param ip
	 * @param features the list to be filled
	 * 
	 * @return number of detected features
	 */
	@Override
	final public void extractFeatures( final ImageProcessor ip, final Collection< Feature > features )
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
