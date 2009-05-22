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
		public float maxEpsilon = 25.0f;
		
		/**
		 * Inlier/candidates ratio
		 */
		public float minInlierRatio = 0.05f;
		
		/**
		 * Implemeted transformation models for choice
		 */
		final static public String[] modelStrings = new String[]{ "Translation", "Rigid", "Similarity", "Affine" };
		public int expectedModelIndex = 1;
		public int desiredModelIndex = 3;
		public int localModelIndex = 3;
		
		public float alpha = 1.0f;
		public int meshResolution = 16;
		
		public float minCCC = 0.6f;
		
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
				return;
			}
			
			try { model.fit( matches ); }
			catch ( Exception e )
			{
				e.printStackTrace();
				return;
			}
			
			/* block match forward */
			final Collection< Point > sourcePoints = new ArrayList< Point >();
			final Collection< Point > targetPoints = new ArrayList< Point >();
			final int blockRadius = Math.max( 16, p.imp1.getWidth() / p.meshResolution / 2 );
			final TransformMesh mesh = new TransformMesh( p.meshResolution, p.imp1.getWidth(), p.imp1.getHeight() );
			PointMatch.sourcePoints( mesh.getVA().keySet(), sourcePoints );
			final Collection< PointMatch > sourceMatches = new ArrayList< PointMatch >();
			BlockMatching.matchByMaximalPMCC(
					( FloatProcessor )p.imp1.getProcessor().convertToFloat().duplicate(),
					( FloatProcessor )p.imp2.getProcessor().convertToFloat().duplicate(),
					512.0f / p.imp1.getWidth(),
					model,
					blockRadius,
					blockRadius,
					Math.round( p.maxEpsilon ),
					Math.round( p.maxEpsilon ),
					p.minCCC,
					sourcePoints,
					sourceMatches );
			
			sourcePoints.clear();
			PointMatch.sourcePoints( sourceMatches, sourcePoints );
			PointMatch.targetPoints( sourceMatches, targetPoints );
			
			p.imp1.setRoi( Util.pointsToPointRoi( sourcePoints ) );
			p.imp2.setRoi( Util.pointsToPointRoi( targetPoints ) );
		}
	}
}
