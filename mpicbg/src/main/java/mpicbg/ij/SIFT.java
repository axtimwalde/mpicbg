package mpicbg.ij;

import ij.gui.GenericDialog;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.util.Collection;

import mpicbg.imagefeatures.Feature;
import mpicbg.imagefeatures.Filter;
import mpicbg.imagefeatures.FloatArray2D;
import mpicbg.imagefeatures.FloatArray2DSIFT;
import mpicbg.imagefeatures.FloatArray2DScaleOctave;
import mpicbg.imagefeatures.ImageArrayConverter;

/**
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.4b
 */
public class SIFT extends FeatureTransform< FloatArray2DSIFT >
{
	/**
	 * Constructor
	 * 
	 * @param t feature transformation
	 */
	public SIFT( final FloatArray2DSIFT t )
	{
		super( t );
	}
	
	final static public void addFields( final GenericDialog gd, final FloatArray2DSIFT.Param p )
	{
		gd.addMessage( "Scale Invariant Interest Point Detector:" );
		gd.addNumericField( "initial_gaussian_blur :", p.initialSigma, 2, 6, "px" );
		gd.addNumericField( "steps_per_scale_octave :", p.steps, 0 );
		gd.addNumericField( "minimum_image_size :", p.minOctaveSize, 0, 6, "px" );
		gd.addNumericField( "maximum_image_size :", p.maxOctaveSize, 0, 6, "px" );
		
		gd.addMessage( "Feature Descriptor:" );
		gd.addNumericField( "feature_descriptor_size :", p.fdSize, 0 );
		gd.addNumericField( "feature_descriptor_orientation_bins :", p.fdBins, 0 );
	}
	
	final static public void readFields( final GenericDialog gd, final FloatArray2DSIFT.Param p )
	{
		p.initialSigma = ( float )gd.getNextNumber();
		p.steps = ( int )gd.getNextNumber();
		p.minOctaveSize = ( int )gd.getNextNumber();
		p.maxOctaveSize = ( int )gd.getNextNumber();
		p.fdSize = ( int )gd.getNextNumber();
		p.fdBins = ( int )gd.getNextNumber();
	}

	
	/**
	 * Extract SIFT features from an ImageProcessor
	 * 
	 * @param ip
	 * @param features the list to be filled
	 * 
	 * @return number of detected features
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
