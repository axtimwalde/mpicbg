import mpicbg.ij.SIFT;
import mpicbg.ij.blockmatching.BlockMatching;
import mpicbg.ij.util.Util;
import mpicbg.imagefeatures.*;
import mpicbg.models.*;

import ij.plugin.*;
import ij.process.FloatProcessor;
import ij.gui.*;
import ij.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Align_SIFT_Blockmatching implements PlugIn
{
	static private class Param
	{	
		final public FloatArray2DSIFT.Param sift = new FloatArray2DSIFT.Param();
		
		public ImagePlus imp1;
		public ImagePlus imp2;
		
		/**
		 * Closest/next closest neighbour distance ratio
		 */
		public float rod = 0.92f;
		
		/**
		 * Maximal allowed alignment error in px
		 */
		public float maxEpsilon = 200.0f;
		
		/**
		 * Inlier/candidates ratio
		 */
		public float minInlierRatio = 0.2f;
		
		/**
		 * Implemeted transformation models for choice
		 */
		final static public String[] modelStrings = new String[]{ "Translation", "Rigid", "Similarity", "Affine" };
		public int expectedModelIndex = 1;
		public int desiredModelIndex = 1;
		public int localModelIndex = 1;
		
		public float alpha = 1.0f;
		public int meshResolution = 32;
		
		public float minR = 0.7f;
		public float maxCurvatureR = 5f;
		public float rodR = 0.8f;
		
		Param()
		{
			sift.fdSize = 8;
			sift.maxOctaveSize = 512;
		}
		
		public boolean setup()
		{
			if ( IJ.versionLessThan( "1.41" ) ) return false;
			
			final int[] ids = WindowManager.getIDList();
			if ( ids == null || ids.length < 2 )
			{
				IJ.showMessage( "You should have at least two images open." );
				return false;
			}
			
			final String[] titles = new String[ ids.length ];
			for ( int i = 0; i < ids.length; ++i )
				titles[ i ] = ( WindowManager.getImage( ids[ i ] ) ).getTitle();
			
			final GenericDialog gd = new GenericDialog( "Align using SIFT and Blockmatchig" );
			
			gd.addMessage( "Image Selection" );
			final String current = WindowManager.getCurrentImage().getTitle();
			gd.addChoice( "source_image :", titles, current );
			gd.addChoice( "target_image :", titles, current.equals( titles[ 0 ] ) ? titles[ 1 ] : titles[ 0 ] );
			
			SIFT.addFields( gd, sift );
			gd.addNumericField( "closest/next_closest_ratio :", rod, 2 );
			
			gd.addMessage( "Geometric Consensus Filter" );
			gd.addNumericField( "maximal_alignment_error :", maxEpsilon, 2, 6, "px" );
			gd.addNumericField( "inlier_ratio :", minInlierRatio, 2 );
			gd.addChoice( "expected_transformation :", modelStrings, modelStrings[ expectedModelIndex ] );
			
			gd.addMessage( "Align" );
			gd.addChoice( "approximate_transformation :", modelStrings, modelStrings[ desiredModelIndex ] );
			gd.addNumericField( "alpha :", alpha, 2 );
			gd.addNumericField( "mesh_resolution :", meshResolution, 0 );
			gd.addChoice( "local_transformation :", modelStrings, modelStrings[ localModelIndex ] );
			
			gd.showDialog();
			
			if (gd.wasCanceled()) return false;
			
			imp1 = WindowManager.getImage( ids[ gd.getNextChoiceIndex() ] );
			imp2 = WindowManager.getImage( ids[ gd.getNextChoiceIndex() ] );
			
			sift.initialSigma = ( float )gd.getNextNumber();
			sift.steps = ( int )gd.getNextNumber();
			sift.minOctaveSize = ( int )gd.getNextNumber();
			sift.maxOctaveSize = ( int )gd.getNextNumber();
			
			sift.fdSize = ( int )gd.getNextNumber();
			sift.fdBins = ( int )gd.getNextNumber();
			rod = ( float )gd.getNextNumber();
			
			maxEpsilon = ( float )gd.getNextNumber();
			minInlierRatio = ( float )gd.getNextNumber();
			expectedModelIndex = gd.getNextChoiceIndex();
			
			desiredModelIndex = gd.getNextChoiceIndex();
			alpha = ( float )gd.getNextNumber();
			meshResolution = ( int )gd.getNextNumber();
			localModelIndex = gd.getNextChoiceIndex();
			
			return true;
		}
	}
	
	final static private Param p = new Param();
	
	final private static void findMatches(
			final Param p,
			final Model< ? > initialModel,
			final Collection< PointMatch > sourceMatches )
	{
		CoordinateTransform ict = initialModel;
		final Collection< Point > sourcePoints = new ArrayList< Point >();
		final Collection< Point > targetPoints = new ArrayList< Point >();
		final Class< ? extends AbstractAffineModel2D< ? > > localModelClass;
		switch ( p.localModelIndex )
		{
		case 0:
			localModelClass = TranslationModel2D.class;
			break;
		case 1:
			localModelClass = RigidModel2D.class;
			break;
		case 2:
			localModelClass = SimilarityModel2D.class;
			break;
		case 3:
			localModelClass = AffineModel2D.class;
			break;
		default:
			IJ.error( "Invalid model selected." );
			return;
		}
		
		for ( int n = 4; n <= p.meshResolution; n *= 2 )
		{
			n = Math.min( p.meshResolution, n );
			
			final MovingLeastSquaresTransform mlst = new MovingLeastSquaresTransform();
			try
			{
				mlst.setModel( localModelClass );
			}
			catch ( Exception e )
			{
				IJ.error( "Invalid local model selected." );
				return;
			}
			
			final int blockRadius = Math.max( 16, p.imp1.getWidth() / n );
			// TODO adapt the search radius to the last search results (largest shift * constant)
			final int searchRadius = ( int )( sourceMatches.size() >= mlst.getModel().getMinNumMatches() ? Math.min( p.maxEpsilon + 0.5f, blockRadius ) : p.maxEpsilon );
			
			/* block match forward */
			sourcePoints.clear();
			sourceMatches.clear();
			
			final TransformMesh mesh = new TransformMesh( n, p.imp1.getWidth(), p.imp1.getHeight() );
			PointMatch.sourcePoints( mesh.getVA().keySet(), sourcePoints );
			BlockMatching.matchByMaximalPMCC(
					( FloatProcessor )p.imp1.getProcessor().convertToFloat().duplicate(),
					( FloatProcessor )p.imp2.getProcessor().convertToFloat().duplicate(),
					//512.0f / p.imp1.getWidth(),
					Math.min(  1.0f, 16.0f / searchRadius ),
					ict,
					blockRadius,
					blockRadius,
					searchRadius,
					searchRadius,
					p.minR,
					p.rodR,
					p.maxCurvatureR,
					sourcePoints,
					sourceMatches );
			
			if  ( sourceMatches.size() >= mlst.getModel().getMinNumMatches() )
			{	
				mlst.setAlpha( p.alpha );
				mlst.setMatches( sourceMatches );
				
				ict = mlst;
				
				sourcePoints.clear();
				targetPoints.clear();
				PointMatch.sourcePoints( sourceMatches, sourcePoints );
				PointMatch.targetPoints( sourceMatches, targetPoints );
			
				p.imp1.setRoi( Util.pointsToPointRoi( sourcePoints ) );
				p.imp2.setRoi( Util.pointsToPointRoi( targetPoints ) );
			}
		}
		// TODO refine the search results by rematching at higher resolution with lower search radius
	}
	
	public void run( String args )
	{
		if ( p.setup() )
		{
			/* lazy match features call, losing sub-pixel-accuracy here */
			IJ.run(
					"Extract SIFT Correspondences",
					"source_image='" + p.imp1.getTitle()
					+ "' target_image='" + p.imp2.getTitle()
					+ "' initial_gaussian_blur=" + p.sift.initialSigma
					+ " steps_per_scale_octave=" + p.sift.steps
					+ " minimum_image_size=" + p.sift.minOctaveSize
					+ " maximum_image_size=" + p.sift.maxOctaveSize
					+ " feature_descriptor_size=" + p.sift.fdSize
					+ " feature_descriptor_orientation_bins=" + p.sift.fdBins
					+ " closest/next_closest_ratio=" + p.rod
					+ " maximal_alignment_error=" + p.maxEpsilon
					+ " inlier_ratio=" + p.minInlierRatio
					+ " expected_transformation=" + Param.modelStrings[ p.expectedModelIndex ] );
			
			/* gather all the points */
			final List< PointMatch > matches = Util.pointRoisToPointMatches(
					( PointRoi )p.imp1.getRoi(),
					( PointRoi )p.imp2.getRoi() );
			
			/* estimate the approximate transformation */
			AbstractAffineModel2D< ? > model;
			switch ( p.desiredModelIndex )
			{
			case 0:
				model = new TranslationModel2D();
				break;
			case 1:
				model = new RigidModel2D();
				break;
			case 2:
				model = new SimilarityModel2D();
				break;
			case 3:
				model = new AffineModel2D();
				break;
			default:
				IJ.error( "Invalid model selected." );
				return;
			}
			
			try { model.fit( matches ); }
			catch ( Exception e )
			{
				e.printStackTrace();
				return;
			}
			
			final Collection< PointMatch > sourceMatches = new ArrayList< PointMatch >();
			findMatches( p, model, sourceMatches );
		}
	}
}
