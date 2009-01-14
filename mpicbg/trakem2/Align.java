/**
 * 
 */
package mpicbg.trakem2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import ij.IJ;
import ij.gui.GenericDialog;
import mpicbg.ij.FeatureTransform;
import mpicbg.ij.SIFT;
import mpicbg.imagefeatures.Feature;
import mpicbg.imagefeatures.FloatArray2DSIFT;
import mpicbg.models.AbstractAffineModel2D;
import mpicbg.models.AffineModel2D;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.PointMatch;
import mpicbg.models.SimilarityModel2D;

public class Align
{
	static public class Param
	{	
		final public FloatArray2DSIFT.Param sift = new FloatArray2DSIFT.Param();

		/**
		 * Closest/next closest neighbour distance ratio
		 */
		public float rod = 0.92f;
		
		/**
		 * Maximal allowed alignment error in px
		 */
		public float maxEpsilon = 100.0f;
		
		/**
		 * Inlier/candidates ratio
		 */
		public float minInlierRatio = 0.05f;
		
		/**
		 * Implemeted transformation models for choice
		 */
		final static public String[] modelStrings = new String[]{ "Translation", "Rigid", "Similarity", "Affine" };
		public int modelIndex = 1;
		
		public Param()
		{
			sift.fdSize = 8;
		}
	
		final public boolean setup( String title )
		{
			final GenericDialog gd = new GenericDialog( title );
			
			gd.addMessage( "Scale Invariant Interest Point Detector:" );
			gd.addNumericField( "initial_gaussian_blur :", sift.initialSigma, 2, 6, "px" );
			gd.addNumericField( "steps_per_scale_octave :", sift.steps, 0 );
			gd.addNumericField( "minimum_image_size :", sift.minOctaveSize, 0, 6, "px" );
			gd.addNumericField( "maximum_image_size :", sift.maxOctaveSize, 0, 6, "px" );
			
			gd.addMessage( "Feature Descriptor:" );
			gd.addNumericField( "feature_descriptor_size :", sift.fdSize, 0 );
			gd.addNumericField( "feature_descriptor_orientation_bins :", sift.fdBins, 0 );
			gd.addNumericField( "closest/next_closest_ratio :", rod, 2 );
			
			gd.addMessage( "Geometric Consensus Filter:" );
			gd.addNumericField( "maximal_alignment_error :", maxEpsilon, 2, 6, "px" );
			gd.addNumericField( "inlier_ratio :", minInlierRatio, 2 );
			gd.addChoice( "expected_transformation :", modelStrings, modelStrings[ modelIndex ] );
			
			gd.showDialog();
			
			if ( gd.wasCanceled() ) return false;
						
			sift.initialSigma = ( float )gd.getNextNumber();
			sift.steps = ( int )gd.getNextNumber();
			sift.minOctaveSize = ( int )gd.getNextNumber();
			sift.maxOctaveSize = ( int )gd.getNextNumber();
			
			sift.fdSize = ( int )gd.getNextNumber();
			sift.fdBins = ( int )gd.getNextNumber();
			rod = ( float )gd.getNextNumber();
			
			maxEpsilon = ( float )gd.getNextNumber();
			minInlierRatio = ( float )gd.getNextNumber();
			modelIndex = gd.getNextChoiceIndex();
				
			return true;
		}
		
		final public Param clone()
		{
			Param p = new Param();
			p.rod = rod;
			p.maxEpsilon = maxEpsilon;
			p.minInlierRatio = minInlierRatio;
			p.modelIndex = modelIndex;
			
			p.sift.initialSigma = this.sift.initialSigma;
			p.sift.steps = this.sift.steps;
			p.sift.minOctaveSize = this.sift.minOctaveSize;
			p.sift.maxOctaveSize = this.sift.maxOctaveSize;
			p.sift.fdSize = this.sift.fdSize;
			p.sift.fdBins = this.sift.fdBins;
					
			return p;
		}
	}
	
	/**
	 * Extracts a {@link Collection} of {@link Feature SIFT-features} from a
	 * {@link List} of {@link AbstractAffineTile2D Tiles} and feeds them
	 * into a {@link HashMap} that links each {@link AbstractAffineTile2D Tile}
	 * and its {@link Collection Feature-collection}.
	 *
	 */
	static public class ExtractFeaturesThread extends Thread
	{
		final protected Param p;
		final protected List< AbstractAffineTile2D< ? > > tiles;
		final protected HashMap< AbstractAffineTile2D< ? >, Collection< Feature > > tileFeatures;
		final protected AtomicInteger ai;
		final protected AtomicInteger ap;
		final protected int steps;
		
		public ExtractFeaturesThread(
				final Param p,
				final List< AbstractAffineTile2D< ? > > tiles,
				final HashMap< AbstractAffineTile2D< ? >, Collection< Feature > > tileFeatures,
				final AtomicInteger ai,
				final AtomicInteger ap,
				final int steps )
		{
			this.p = p;
			this.tiles = tiles;
			this.tileFeatures = tileFeatures;
			this.ai = ai;
			this.ap = ap;
			this.steps = steps;
		}
		
		final public void run()
		{
			FloatArray2DSIFT sift = new FloatArray2DSIFT( p.sift );
			SIFT ijSIFT = new SIFT( sift );

			for ( int i = ai.getAndIncrement(); i < tiles.size(); i = ai.getAndIncrement() )
			{
				AbstractAffineTile2D< ? > tile = tiles.get( i );
				Collection< Feature > features = new ArrayList< Feature >();
				long s = System.currentTimeMillis();
				ijSIFT.extractFeatures( tile.createMaskedByteImage(), features );
				tileFeatures.put( tile, features );
				IJ.log( features.size() + " features extracted in tile " + i + " \"" + tile.getPatch().getTitle() + "\" (took " + ( System.currentTimeMillis() - s ) + " ms)." );
				IJ.showProgress( ap.getAndIncrement(), steps );
			}
		}
	}
	
	static public class matchFeaturesAndFindModelThread
	{
		final protected Param p;
		final protected List< AbstractAffineTile2D< ? > > tiles;
		final protected HashMap< AbstractAffineTile2D< ? >, Collection< Feature > > tileFeatures;
		final protected List< int[] > tilePairs;
		final protected AtomicInteger ai;
		final protected AtomicInteger ap;
		final protected int steps;
		
		public matchFeaturesAndFindModelThread(
				final Param p,
				final List< AbstractAffineTile2D< ? > > tiles,
				final HashMap< AbstractAffineTile2D< ? >, Collection< Feature > > tileFeatures,
				final List< int[] > tilePairs,
				final AtomicInteger ai,
				final AtomicInteger ap,
				final int steps )
		{
			this.p = p;
			this.tiles = tiles;
			this.tileFeatures = tileFeatures;
			this.tilePairs = tilePairs;
			this.ai = ai;
			this.ap = ap;
			this.steps = steps;
		}
		
		final public void run()
		{
			final List< PointMatch > candidates = new ArrayList< PointMatch >();
			final List< PointMatch > inliers = new ArrayList< PointMatch >();
				
			for ( int i = ai.getAndIncrement(); i < tilePairs.size(); i = ai.getAndIncrement() )
			{
				candidates.clear();
				inliers.clear();
				
				final int[] tilePair = tilePairs.get( i );
				long s = System.currentTimeMillis();
				
				FeatureTransform.matchFeatures(
					tileFeatures.get( tiles.get( tilePair[ 0 ] ) ),
					tileFeatures.get( tiles.get( tilePair[ 1 ] ) ),
					candidates,
					p.rod );

				AbstractAffineModel2D< ? > model;
				switch ( p.modelIndex )
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
	
				boolean modelFound;
				try
				{
					modelFound = model.filterRansac(
							candidates,
							inliers,
							1000,
							p.maxEpsilon,
							p.minInlierRatio,
							3 * model.getMinNumMatches(),
							3 );
				}
				catch ( NotEnoughDataPointsException e )
				{
					modelFound = false;
				}
				
				if ( modelFound )
				{
					IJ.log( "Model found for tiles " + tilePair[ 0 ] + " and " + tilePair[ 1 ] + ":\n  correspondences  " + inliers.size() + " of " + candidates.size() + "\n  average residual error  " + model.getCost() + " px\n  took " + ( System.currentTimeMillis() - s ) + " ms" );
					// TODO continue here ...
				}
				else
					IJ.log( "No model found for tiles " + tilePair[ 0 ] + " and " + tilePair[ 1 ] + "." );
				IJ.showProgress( ap.getAndIncrement(), steps );
			}
		}
	}
}
