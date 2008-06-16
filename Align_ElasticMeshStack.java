import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.plugin.PlugIn;
import ij.process.*;
import ij.gui.*;

import mpicbg.imagefeatures.Feature;
import mpicbg.imagefeatures.Filter;
import mpicbg.imagefeatures.FloatArray2D;
import mpicbg.imagefeatures.FloatArray2DMOPS;
import mpicbg.imagefeatures.FloatArray2DScaleOctave;
import mpicbg.imagefeatures.ImageArrayConverter;
import mpicbg.models.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Align_ElasticMeshStack implements PlugIn
{
	// steps
	private static int steps = 3;
	// initial sigma
	private static float initial_sigma = 1.6f;
	// feature descriptor size
	private static int fdsize = 22;
	// closest/next closest neighbour distance ratio
	private static float rod = 0.92f;
	// size restrictions for scale octaves, use octaves < max_size and > min_size only
	private static int min_size = 64;
	private static int max_size = 1024;
	// maximal allowed alignment error in px
	private static float max_epsilon = 25.0f;
	private static float min_inlier_ratio = 0.05f;
	private static int numX = 12;
	private static int numY = 12;
	
	/**
	 * Set true to double the size of the image by linear interpolation to
	 * ( with * 2 + 1 ) * ( height * 2 + 1 ).  Thus we can start identifying
	 * DoG extrema with $\sigma = INITIAL_SIGMA / 2$ like proposed by
	 * \citet{Lowe04}.
	 * 
	 * This is useful for images scmaller than 1000px per side only. 
	 */ 
	private static boolean upscale = false;
	private static float scale = 1.0f;
	
	boolean showMesh = false;
	
	final ElasticMeshStack meshes = new ElasticMeshStack();
	
	ImageStack stack, stackAligned;
	ImagePlus imp, impAligned;
	
	public void run( String arg )
    {
		if ( IJ.versionLessThan( "1.40c" ) ) return;
		
		/**
		 * Cleanup
		 */
		meshes.clear();
		
		List< Feature > features1;
		List< Feature > features2;
		ElasticMesh m1;
		ElasticMesh m2;
		
		imp = WindowManager.getCurrentImage();
		if ( imp == null )  { System.err.println( "You should have a stack open" ); return; }
		
			
		GenericDialog gd = new GenericDialog( "Elastic Stack Registration" );
		gd.addMessage( "MOPS Parameters:" );
		gd.addNumericField( "steps_per_scale_octave :", steps, 0 );
		gd.addNumericField( "initial_gaussian_blur :", initial_sigma, 2 );
		gd.addNumericField( "feature_descriptor_width :", fdsize, 0 );
		gd.addNumericField( "minimum_image_size :", min_size, 0 );
		gd.addNumericField( "maximum_image_size :", max_size, 0 );
		gd.addNumericField( "closest/next_closest_ratio :", rod, 2 );
		gd.addNumericField( "maximal_alignment_error :", ( imp.getWidth() + imp.getHeight() ) / 40, 2 );
		gd.addNumericField( "inlier_ratio :", min_inlier_ratio, 2 );
		gd.addCheckbox( "upscale_image_first", upscale );
		gd.addMessage( "Mesh Parameters:" );
		gd.addNumericField( "horizontal_handles :", numX, 0 );
		gd.addNumericField( "vertical_handles :", numY, 0 );
		gd.showDialog();
		if (gd.wasCanceled()) return;
			
		steps = ( int )gd.getNextNumber();
		initial_sigma = ( float )gd.getNextNumber();
		fdsize = ( int )gd.getNextNumber();
//		fdbins = ( int )gd.getNextNumber();
		min_size = ( int )gd.getNextNumber();
		max_size = ( int )gd.getNextNumber();
		rod = ( float )gd.getNextNumber();
		max_epsilon = ( float )gd.getNextNumber();
		min_inlier_ratio = ( float )gd.getNextNumber();
		upscale = gd.getNextBoolean();
		if ( upscale ) scale = 2.0f;
		else scale = 1.0f;
		numX = ( int )gd.getNextNumber();
		numY = ( int )gd.getNextNumber();
		
		stack = imp.getStack();
		stackAligned = new ImageStack( stack.getWidth(), stack.getHeight() );
		for ( int i = 1; i <= stack.getSize(); ++i )
			stackAligned.addSlice( "", stack.getProcessor( i ).duplicate() );
		
		impAligned = new ImagePlus( imp.getTitle() + " aligned", stackAligned );
		impAligned.show();
			
		ImageProcessor ip;
			
		ip = stack.getProcessor( 1 ).convertToFloat();
			
		FloatArray2DMOPS mops = new FloatArray2DMOPS( fdsize );
			
		FloatArray2D fa = ImageArrayConverter.ImageToFloatArray2D( ip );
		Filter.enhance( fa, 1.0f );
			
		float[] initial_kernel;
			
		if ( upscale )
		{
			FloatArray2D fat = new FloatArray2D( fa.width * 2 - 1, fa.height * 2 - 1 ); 
			FloatArray2DScaleOctave.upsample( fa, fat );
			fa = fat;
			initial_kernel = Filter.createGaussianKernel( ( float )Math.sqrt( initial_sigma * initial_sigma - 1.0 ), true );
		}
		else
			initial_kernel = Filter.createGaussianKernel( ( float )Math.sqrt( initial_sigma * initial_sigma - 0.25 ), true );
			
		fa = Filter.convolveSeparable( fa, initial_kernel, initial_kernel );
				
		long start_time = System.currentTimeMillis();
		IJ.log( "processing MOPS ..." );
		mops.init( fa, steps, initial_sigma, min_size, max_size );
		features2 = mops.run( max_size ); 
		Collections.sort( features2 );
		IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms" );
			
		IJ.log( features2.size() + " features identified and processed" );
		
		m2 = new ElasticMesh();
		m2.init( numX, numY, imp.getWidth(), imp.getHeight() );
		m2.init();
		
		meshes.addMesh( m2 );
			
		for ( int i = 1; i < stack.getSize(); ++i )
		{
			System.out.println( numX + " " + numY );
			ip = stack.getProcessor( i + 1 ).convertToFloat();
			fa = ImageArrayConverter.ImageToFloatArray2D( ip );
			Filter.enhance( fa, 1.0f );
			
			if ( upscale )
			{
				FloatArray2D fat = new FloatArray2D( fa.width * 2 - 1, fa.height * 2 - 1 ); 
				FloatArray2DScaleOctave.upsample( fa, fat );
				fa = fat;
			}
				
			fa = Filter.convolveSeparable( fa, initial_kernel, initial_kernel );
			
			features1 = features2;
			m1 = m2;
				
			start_time = System.currentTimeMillis();
			IJ.log( "processing MOPS ..." );
			mops.init( fa, steps, initial_sigma, min_size, max_size );
			features2 = mops.run( max_size);
			Collections.sort( features2 );
			IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms" );
				
			IJ.log( features2.size() + " features identified and processed");
				
			m2 = new ElasticMesh();
			m2.init( numX, numY, imp.getWidth(), imp.getHeight() );
			m2.init();
			
			start_time = System.currentTimeMillis();
			IJ.log( "identifying correspondences using brute force ..." );
			List< PointMatch > candidates = 
					FloatArray2DMOPS.createMatches( features1, features2, rod );
			IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms" );
				
			IJ.log( candidates.size() + " potentially corresponding features identified" );
				
			List< PointMatch > inliers = new ArrayList< PointMatch >();
				
			RigidModel2D model = null;
			try
			{
				model = Model.filterRansac(
						RigidModel2D.class,
						candidates,
						inliers,
						1000,
						max_epsilon,
						min_inlier_ratio );
			}
			catch ( Exception e )
			{
				System.err.println( e.getMessage() );
			}
			if ( model != null )
			{
				for ( PointMatch pm : inliers )
				{
					float[] here = pm.getP1().getL();
					float[] there = pm.getP2().getL();
					Tile t = m1.findClosest( here );
					Tile o = m2.findClosest( there );
					 
					try
					{
						here = t.getModel().applyInverse( here );
						there = o.getModel().applyInverse( there );
						Point p1 = new Point( here );
						p1.apply( t.getModel() );
						
						Point p2 = new Point( there );
						p2.apply( o.getModel() );
						
						t.addMatch( new PointMatch( p1, p2, 10f ) );
						o.addMatch( new PointMatch( p2, p1, 10f ) );
					}
					catch ( NoninvertibleModelException e )
					{
						e.printStackTrace( System.err );
					}
				}
			}
			
			meshes.addMesh( m2 );
		}
		ArrayList< ElasticMesh > ms = meshes.meshes;
		ElasticMesh mm = ms.get( stack.getSize() / 2 );
		Tile tc = mm.findClosest( new float[]{ imp.getWidth() / 2, imp.getHeight() / 2 } );
		mm.fixTile( tc );
		
		IJ.log( "Optimizing..." );
		optimize();
		apply();
    }
	
	
	public void optimize()
	{
		try
		{
			meshes.optimize( Float.MAX_VALUE, 10000, 100, stack, stackAligned, impAligned );
			apply();
		}
		catch ( NotEnoughDataPointsException ex )
		{
			ex.printStackTrace( System.err );
		}
	}
	
	public void apply()
	{
		ArrayList< ElasticMesh > meshStack = meshes.meshes;
		for ( int i = 0; i < stack.getSize(); ++ i )
		{
			ElasticMesh mesh = meshStack.get( i );
			mesh.apply( stack.getProcessor( i + 1 ), stackAligned.getProcessor( i + 1 ) );
		}
		impAligned.updateAndDraw();
	}
}
