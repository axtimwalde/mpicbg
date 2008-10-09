import mpicbg.imagefeatures.*;

import ij.plugin.*;
import ij.gui.*;
import ij.*;
import ij.process.*;

public class Scale_Space implements PlugIn
{
	// steps
	private static int steps = 3;
	// initial sigma
	private static float initialSigma = 1.6f;

	public void run( String args )
	{
		if ( IJ.versionLessThan( "1.37i" ) ) return;

		final ImagePlus imp = WindowManager.getCurrentImage();
		if ( imp == null )
		{
			System.out.println( "There are no images open." );
			return;
		}

		GenericDialog gd = new GenericDialog( "Scale space" );
		gd.addNumericField( "initial_gaussian_blur :", initialSigma, 2, 6, "px" );
		gd.addNumericField( "steps_per_scale_octave :", steps, 0 );
		gd.showDialog();

		if ( gd.wasCanceled() ) return;

		initialSigma = ( float )gd.getNextNumber();
		steps = ( int )gd.getNextNumber();
		
		ImageProcessor ip = imp.getProcessor().convertToFloat();
		FloatArray2D fa = ImageArrayConverter.ImageToFloatArray2D( ip );
		Filter.enhance( fa, 1.0f );
		float[] initial_kernel = Filter.createGaussianKernel( ( float )Math.sqrt( initialSigma * initialSigma - 0.25 ), true );
		fa = Filter.convolveSeparable( fa, initial_kernel, initial_kernel );
		
		FloatArray2DSIFT sift = new FloatArray2DSIFT( 0, 0 );
		
		long start_time = System.currentTimeMillis();
		IJ.log( "Initializing scale space ..." );
		sift.init( fa, steps, initialSigma, 32, Math.max( fa.width, fa.height ) );
		IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms." );
		
		ImageStack stackScaleSpace = new ImageStack( imp.getWidth(), imp.getHeight() );
		ImageStack stackDoG = new ImageStack( imp.getWidth(), imp.getHeight() );
		
		FloatArray2DScaleOctave[] sos = sift.getOctaves();
		for ( int o = 0; o < sos.length; ++o )
		{
			FloatArray2DScaleOctave so = sos[ o ];
			
			start_time = System.currentTimeMillis();
			IJ.log( "Building scale octave ..." );
			so.build();
			IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms." );
			
//			FloatArray2DScaleOctaveDoGDetector dog = new FloatArray2DScaleOctaveDoGDetector();
//		
//			start_time = System.currentTimeMillis();
//			IJ.log( "Identifying difference of gaussian extrema ..." );
//			dog.run( so );
//			IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms." );
		
			FloatArray2D[] l = so.getL();
			FloatArray2D[] d = so.getD();
			
			final ImageStack stackOctave = new ImageStack( so.width, so.height );
			for ( int i = 0; i < l.length; ++i )
			{
				final FloatProcessor fp = new FloatProcessor( so.width, so.height );
				ImageArrayConverter.FloatArrayToFloatProcessor( fp, l[ i ] );
				fp.setMinAndMax( 0.0, 1.0 );
				final ImageProcessor bp = fp.convertToByte( true );
				stackOctave.addSlice( null, bp );
			}
			ImagePlus impStackOctave = new ImagePlus( "", stackOctave );
			impStackOctave.show();
			
			final ImageStack stackOctaveDoG = new ImageStack( so.width, so.height );
			for ( int i = 0; i < d.length; ++i )
			{
				final FloatProcessor fp = new FloatProcessor( so.width, so.height );
				ImageArrayConverter.FloatArrayToFloatProcessor( fp, d[ i ] );
				fp.setMinAndMax( -0.25, 0.25 );
				final ImageProcessor bp = fp.convertToByte( true );
				stackOctaveDoG.addSlice( null, bp );
			}
			ImagePlus impStackOctaveDoG = new ImagePlus( "", stackOctaveDoG );
			impStackOctaveDoG.show();

			for ( int i = 0; i < steps; ++i )
			{
				FloatArray2D ls = l[ i ];
				FloatArray2D ds = d[ i ];
				int os;
				for ( int oi = o; oi > 0; --oi )
				{
					os = ( int )Math.pow( 2, oi - 1 );
					int w = imp.getWidth();
					int h = imp.getHeight();
					for ( os = oi; os > 1; --os )
					{
						w = w / 2 + w % 2;
						h = h / 2 + h % 2;
					}
					//System.out.println( "o: " + o + ", w: " + w + ", h: " + h );
					FloatArray2D ld = new FloatArray2D( w, h );
					FloatArray2D dd = new FloatArray2D( w, h );
					FloatArray2DScaleOctave.upsample( ls, ld );
					FloatArray2DScaleOctave.upsample( ds, dd );
					ls = ld;
					ds = dd;
				}
				os = ( int )Math.pow( 2, o );
				FloatProcessor fp = new FloatProcessor( ls.width, ls.height );
				ImageArrayConverter.FloatArrayToFloatProcessor( fp, ls );
				fp.setMinAndMax( 0.0, 1.0 );
				//ImageProcessor ipl = fp.convertToRGB();
				ImageProcessor ipl = fp.duplicate();
				ImageArrayConverter.FloatArrayToFloatProcessor( fp, ds );
				fp.setMinAndMax( -1.0, 1.0 );
				ImageProcessor ipd = fp.convertToRGB();
			
//				// draw DoG detections
//				ipl.setLineWidth( ( int )( initialSigma * ( 1 + ( float )i / steps ) * os ) );
//				ipl.setColor( Color.green );
//				ipd.setLineWidth( ( int )( initialSigma * ( 1 + ( float )i / steps ) * os ) );
//				ipd.setColor( Color.green );
//				
//				Vector< float[] > candidates = dog.getCandidates();
//				for ( float[] c : candidates )
//				{
//					if ( i == ( int )Math.round( c[ 2 ] ) )
//					{
//						ipl.drawDot(
//								( int )Math.round( ( float )os * c[ 0 ] ),
//								( int )Math.round( ( float )os * c[ 1 ] ) );
//						ipd.drawDot(
//								( int )Math.round( ( float )os * c[ 0 ] ),
//								( int )Math.round( ( float )os * c[ 1 ] ) );
//					}	
//				}
				
				stackScaleSpace.addSlice( null, ipl );
				stackDoG.addSlice( null, ipd );
				
				/*
				FloatArray2D[] gradients = so.getL1( i );
				ImageArrayConverter.FloatArrayToFloatProcessor( fp, gradients[ 0 ] );
				stackGradientAmplitude.addSlice( null, fp );
				ImageArrayConverter.FloatArrayToFloatProcessor( fp, gradients[ 1 ] );
				stackGradientOrientation.addSlice( null, fp );
				*/
			}
			
			/*
			for ( int i = 0; i < d.length; ++i )
			{
				FloatProcessor fp = new FloatProcessor( d[ i ].width, d[ i ].height );
				ImageArrayConverter.FloatArrayToFloatProcessor( fp, d[ i ] );
				fp.setMinAndMax( -255.0, 255.0 );
				ImageProcessor ipl = fp.convertToRGB();
				
				// draw DoG detections
				ipl.setLineWidth( 2 );
				ipl.setColor( Color.green );
				
				Vector< float[] > candidates = dog.getCandidates();
				for ( float[] c : candidates )
				{
					if ( i == ( int )Math.round( c[ 2 ] ) )
						ipl.drawDot( ( int )Math.round( c[ 0 ] ), ( int )Math.round( c[ 1 ] ) );
				}
				
				stackDoG.addSlice( null, ipl );
				
				
				//stackDoG.addSlice( null, fp );			
			}
			*/
		}
		ImagePlus impScaleSpace = new ImagePlus( "Scales", stackScaleSpace );
		ImagePlus impDoG = new ImagePlus( "Differences of Scales", stackDoG );
		//ImagePlus impGradientAmplitude = new ImagePlus( "Gradient amplitudes of Scales", stackGradientAmplitude );
		//ImagePlus impGradientOrientation = new ImagePlus( "Gradient orientations of Scales", stackGradientOrientation );
		impScaleSpace.show();
		impDoG.show();
		//impGradientAmplitude.show();
		//impGradientOrientation.show();
		
	}
}