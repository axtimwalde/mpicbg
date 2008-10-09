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
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.plugin.PlugIn;
import ij.process.*;
import ij.gui.*;

import mpicbg.ij.TransformMeshMapping;
import mpicbg.imagefeatures.Feature;
import mpicbg.imagefeatures.Filter;
import mpicbg.imagefeatures.FloatArray2D;
import mpicbg.imagefeatures.FloatArray2DMOPS;
import mpicbg.imagefeatures.FloatArray2DSIFT;
import mpicbg.imagefeatures.FloatArray2DScaleOctave;
import mpicbg.imagefeatures.ImageArrayConverter;
import mpicbg.models.*;

import java.awt.Choice;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;


public class Align_ElasticMeshStack implements PlugIn
{
	/**
	 * Steps per Scale Octave 
	 */
	private static int steps = 3;
	
	/**
	 * Initial sigma of each Scale Octave
	 */
	private static float initialSigma = 1.6f;
	
	/**
	 * Feature descriptor size
	 * 
	 *   for SIFT: How many local histograms per row and column
	 *   for MOPS: How many samples per row and column
	 */
	private static int fdSize = 8;
	
	/**
	 * Feature descriptor orientation bins
	 * 
	 *   for SIFT: How many bins per local histogram
	 */
	private static int fdBins = 8;
	
	/**
	 * Closest/next closest neighbour distance ratio
	 */
	private static float rod = 0.92f;
	
	/**
	 * Size limits for scale octaves in px:
	 * 
	 * minOctaveSize < octave < maxOctaveSize
	 */
	private static int minOctaveSize = 64;
	private static int maxOctaveSize = 1024;
	
	/**
	 * Maximal allowed alignment error in px
	 */
	private static float maxEpsilon = 25.0f;
	
	/**
	 * Inlier/candidates ratio
	 */
	private static float minInlierRatio = 0.05f;
	
	/**
	 * Number of mesh vertices in horizontal direction.  A proper number for
	 * vertical direction is estimated automatically such that triangles are as
	 * equilateral as possible. 
	 */
	private static int numX = 32;

	/**
	 * The exponent applied to the distance of a point to a landmark.
	 * 
	 * [0 smooth, 1 less smooth ]
	 */
	private static float alpha = 1.0f;
	
	/**
	 * Implemeted transformation models for choice
	 */
	final static String[] modelStrings = new String[]{ "Translation", "Rigid", "Affine" };
	final static List< Class< ? extends Model > > modelClasses =
		new ArrayList< Class< ? extends Model > >();
	private static int localModelIndex = 1;
	private static int globalModelIndex = 1;
	private static Class< ? extends Model > localModelClass;
	private static Class< ? extends Model > globalModelClass;
	
	/**
	 * Implemeted feature extraction methods for choice
	 */
	final static String[] featureMethodStrings = new String[]{ "SIFT", "MOPS" };
	final static List< Class< ? > > featureMethodClasses = new ArrayList< Class< ? > >();
	private static int featureMethodIndex = 0;
	private static Class< ? > featureMethodClass;
	
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
	final CoordinateTransformList models = new CoordinateTransformList();
	
	ImageStack stack, stackAligned;
	ImagePlus imp, impAligned;
	
	protected TransformMeshMapping mapping;
	
	public Align_ElasticMeshStack()
	{
		modelClasses.add( TranslationModel2D.class );
		modelClasses.add( RigidModel2D.class );
		modelClasses.add( AffineModel2D.class );
		
		localModelClass = modelClasses.get( localModelIndex );
		globalModelClass = modelClasses.get( globalModelIndex );
		
		featureMethodClasses.add( FloatArray2DSIFT.class );
		featureMethodClasses.add( FloatArray2DMOPS.class );
		
		featureMethodClass = featureMethodClasses.get( featureMethodIndex );
	}
	
	private boolean showDialog()
	{
		final GenericDialog gd = new GenericDialog( "Elastic Stack Registration" );
		
		gd.addMessage( "Scale Invariant Interest Point Detector:" );
		gd.addNumericField( "initial_gaussian_blur :", initialSigma, 2, 6, "px" );
		gd.addNumericField( "steps_per_scale_octave :", steps, 0 );
		gd.addNumericField( "minimum_image_size :", minOctaveSize, 0, 6, "px" );
		gd.addNumericField( "maximum_image_size :", maxOctaveSize, 0, 6, "px" );
		gd.addCheckbox( "upscale_image_first", upscale );
		
		gd.addMessage( "Feature Descriptor:" );
		gd.addChoice( "method :", featureMethodStrings, featureMethodStrings[ featureMethodIndex ] );
		gd.addNumericField( "feature_descriptor_size :", fdSize, 0 );
		gd.addNumericField( "feature_descriptor_orientation_bins :", fdBins, 0 );
		gd.addNumericField( "closest/next_closest_ratio :", rod, 2 );
		
		gd.addMessage( "Geometric Consensus Filter:" );
		gd.addNumericField( "maximal_alignment_error :", ( imp.getWidth() + imp.getHeight() ) / 40, 2, 6, "px" );
		gd.addNumericField( "inlier_ratio :", minInlierRatio, 2 );
		gd.addChoice( "expected_global_transformation :", modelStrings, modelStrings[ globalModelIndex ] );
		
		gd.addMessage( "Mesh Tranformation:" );
		gd.addNumericField( "horizontal_handles :", numX, 0 );
		gd.addNumericField( "alpha :", alpha, 2 );
		gd.addChoice( "desired_local_transformation :", modelStrings, modelStrings[ localModelIndex ] );
		
		final Vector numericFields = gd.getNumericFields();
		final Component featureOrientationBinsField = ( Component )numericFields.get( 5 );
		final Component featureOrientationBinsLabel = gd.getComponent( 15 );
		final Choice featureMethodChoice = ( Choice )gd.getChoices().get( 0 );
		
		class FeatureMethodChoiceItemListener implements ItemListener
		{
			public void itemStateChanged( ItemEvent e )
			{
				updateFields( e.getItem().toString() );
			}
			
			public void updateFields( String item )
			{
				if ( item == featureMethodStrings[ 1 ] )
				{
					featureOrientationBinsField.setEnabled( false );
					featureOrientationBinsLabel.setEnabled( false );
					//featureOrientationBinsField.setVisible( false );
					//featureOrientationBinsLabel.setVisible( false );
				}
				else
				{
					featureOrientationBinsField.setEnabled( true );
					featureOrientationBinsLabel.setEnabled( true );
					//featureOrientationBinsField.setVisible( true );
					//featureOrientationBinsLabel.setVisible( true );
				}
			}
		}
		
		FeatureMethodChoiceItemListener featureMethodChoiceItemListener = new FeatureMethodChoiceItemListener();
		featureMethodChoice.addItemListener( featureMethodChoiceItemListener );
		featureMethodChoiceItemListener.updateFields( featureMethodChoice.getSelectedItem() ); 
		
		gd.showDialog();
		
		if (gd.wasCanceled()) return false;
		
		initialSigma = ( float )gd.getNextNumber();
		steps = ( int )gd.getNextNumber();
		minOctaveSize = ( int )gd.getNextNumber();
		maxOctaveSize = ( int )gd.getNextNumber();
		upscale = gd.getNextBoolean();
		if ( upscale ) scale = 2.0f;
		else scale = 1.0f;
		
		featureMethodIndex = gd.getNextChoiceIndex();
		featureMethodClass = featureMethodClasses.get( featureMethodIndex );
		fdSize = ( int )gd.getNextNumber();
		fdBins = ( int )gd.getNextNumber();
		rod = ( float )gd.getNextNumber();
		
		maxEpsilon = ( float )gd.getNextNumber();
		minInlierRatio = ( float )gd.getNextNumber();
		globalModelIndex = gd.getNextChoiceIndex();
		globalModelClass = modelClasses.get( globalModelIndex );
		
		numX = ( int )gd.getNextNumber();
		alpha = ( float )gd.getNextNumber();
		localModelIndex = gd.getNextChoiceIndex();
		localModelClass = modelClasses.get( localModelIndex );
		
		return true;
	}
	
	public void run( String arg )
    {
		if ( IJ.versionLessThan( "1.40c" ) ) return;
		
		/**
		 * Cleanup
		 */
		meshes.clear();
		
		List< Feature > features1;
		List< Feature > features2;
		ElasticMovingLeastSquaresMesh< ? > m1;
		ElasticMovingLeastSquaresMesh< ? > m2;
		
		imp = WindowManager.getCurrentImage();
		if ( imp == null )  { System.err.println( "You should have a stack open" ); return; }
		
		if ( !showDialog() ) return;		
		
		stack = imp.getStack();
		stackAligned = new ImageStack( stack.getWidth(), stack.getHeight() );
		for ( int i = 1; i <= stack.getSize(); ++i )
			stackAligned.addSlice( "", stack.getProcessor( i ).duplicate() );
		
		impAligned = new ImagePlus( imp.getTitle() + " aligned", stackAligned );
		impAligned.show();
			
		ImageProcessor ip;
			
		ip = stack.getProcessor( 1 ).convertToFloat();
			
		FloatArray2DSIFT sift = new FloatArray2DSIFT( fdSize, fdBins );
			
		FloatArray2D fa = ImageArrayConverter.ImageToFloatArray2D( ip );
		Filter.enhance( fa, 1.0f );
			
		float[] initial_kernel;
			
		if ( upscale )
		{
			FloatArray2D fat = new FloatArray2D( fa.width * 2 - 1, fa.height * 2 - 1 ); 
			FloatArray2DScaleOctave.upsample( fa, fat );
			fa = fat;
			initial_kernel = Filter.createGaussianKernel( ( float )Math.sqrt( initialSigma * initialSigma - 1.0 ), true );
		}
		else
			initial_kernel = Filter.createGaussianKernel( ( float )Math.sqrt( initialSigma * initialSigma - 0.25 ), true );
			
		fa = Filter.convolveSeparable( fa, initial_kernel, initial_kernel );
				
		long start_time = System.currentTimeMillis();
		IJ.log( "processing SIFT ..." );
		sift.init( fa, steps, initialSigma, minOctaveSize, maxOctaveSize );
		features2 = sift.run( maxOctaveSize ); 
		Collections.sort( features2 );
		IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms" );
			
		IJ.log( features2.size() + " features identified and processed" );
		
		m2 = new ElasticMovingLeastSquaresMesh( localModelClass, numX, imp.getWidth(), imp.getHeight(), alpha );
		
		meshes.addMesh( m2 );
			
		for ( int i = 1; i < stack.getSize(); ++i )
		{
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
			IJ.log( "processing SIFT ..." );
			sift.init( fa, steps, initialSigma, minOctaveSize, maxOctaveSize );
			features2 = sift.run( maxOctaveSize);
			Collections.sort( features2 );
			IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms" );
				
			IJ.log( features2.size() + " features identified and processed");
				
			m2 = new ElasticMovingLeastSquaresMesh( localModelClass, numX, imp.getWidth(), imp.getHeight(), alpha );
			
			start_time = System.currentTimeMillis();
			IJ.log( "identifying correspondences using brute force ..." );
			List< PointMatch > candidates = 
					//FloatArray2DSIFT.createMatches( features1, features2, rod );
					FloatArray2DSIFT.createMatches( features2, features1, 1.33f, null, 1, rod );
			IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms" );
				
			IJ.log( candidates.size() + " potentially corresponding features identified" );
				
			List< PointMatch > inliers = new ArrayList< PointMatch >();
			
			if ( scale != 1.0f )
			{
				ArrayList< PointMatch > scaledCandidates = new ArrayList< PointMatch >( candidates.size() );
				for ( PointMatch m : candidates )
				{
					float[] p1 = m.getP1().getL();
					float[] p2 = m.getP2().getL();
					
					scaledCandidates.add(
							new PointMatch(
									new Point( new float[]{ p1[ 0 ] / scale, p1[ 1 ] / scale } ),
									new Point( new float[]{ p2[ 0 ] / scale, p2[ 1 ] / scale } ),
									m.getWeights() ) );
				}
				candidates.clear();
				candidates = scaledCandidates;
			}
			
			Model< ? > model;
			try
			{
				model = globalModelClass.newInstance();
			}
			catch ( Exception e )
			{
				e.printStackTrace();
				return;
			}
			
			boolean modelFound;
			try
			{
				modelFound = model.filterRansac(
						candidates,
						inliers,
						1000,
						maxEpsilon,
						minInlierRatio );
			}
			catch ( Exception e )
			{
				modelFound = false;
			}
			
			if ( modelFound )
			{
				IJ.log( inliers.size() + " corresponding features with an average displacement of " + ElasticMeshStack.decimalFormat.format( model.getCost() ) + "px identified." );
				IJ.log( "Estimated global transformation model: " + model );
				
				for ( final PointMatch pm : inliers )
				{
					final float[] here = pm.getP2().getL();
					final float[] there = pm.getP1().getL();
					Tile< ? > t = m1.findClosest( here );
					Tile< ? > o = m2.findClosest( there );
					
					m1.addMatchWeightedByDistance( new PointMatch( pm.getP2(), pm.getP1(), 0.01f ), alpha );
					m2.addMatchWeightedByDistance( new PointMatch( pm.getP1(), pm.getP2(), 0.01f ), alpha );
					
					t.addConnectedTile( o );
					o.addConnectedTile( t );
				}
				models.add( model );
			}
			else
			{
				try
				{
					models.add( globalModelClass.newInstance() );
				}
				catch ( Exception e )
				{
					e.printStackTrace();
					break;
				}
			}
			m2.apply( models );
			meshes.addMesh( m2 );
		}
		ArrayList< ElasticMovingLeastSquaresMesh< ? > > ms = meshes.meshes;
		//ElasticMovingLeastSquaresMesh mm = ms.get( stack.getSize() / 2 );
		ElasticMovingLeastSquaresMesh mm = ms.get( 0 );
		//Tile< ? > tc = mm.findClosest( new float[]{ imp.getWidth() / 2, imp.getHeight() / 2 } );
		//mm.fixTile( tc );
		
		IJ.log( "Optimizing..." );
		optimize();
		apply();
    }
	
	
	public void optimize()
	{
		try
		{
			meshes.optimize( Float.MAX_VALUE, 10000, 100, impAligned, stack, stackAligned );
			//meshes.optimizeAndFilter( Float.MAX_VALUE, 10000, 100, 2, impAligned, stack, stackAligned );
		}
		catch ( NotEnoughDataPointsException ex )
		{
			ex.printStackTrace( System.err );
		}
		catch ( IllDefinedDataPointsException ex )
		{
			ex.printStackTrace( System.err );
		}
	}
	
	public void apply()
	{
		final ArrayList< ElasticMovingLeastSquaresMesh< ? > > meshStack = meshes.meshes;
		for ( int i = 0; i < stack.getSize(); ++ i )
		{
			ElasticMovingLeastSquaresMesh mesh = meshStack.get( i );
			mapping = new TransformMeshMapping( mesh );
			mapping.mapInterpolated( stack.getProcessor( i + 1 ), stackAligned.getProcessor( i + 1 ) );
		}
		impAligned.updateAndDraw();
	}
}
