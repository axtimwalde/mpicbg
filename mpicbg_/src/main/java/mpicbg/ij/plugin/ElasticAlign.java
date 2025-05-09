/*-
 * #%L
 * MPICBG plugin for Fiji.
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
 */
package mpicbg.ij.plugin;


import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.io.DirectoryChooser;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.TextField;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.ij.SIFT;
import mpicbg.ij.TransformMeshMapping;
import mpicbg.ij.blockmatching.BlockMatching;
import mpicbg.imagefeatures.Feature;
import mpicbg.imagefeatures.FloatArray2DSIFT;
import mpicbg.models.AbstractModel;
import mpicbg.models.AffineModel2D;
import mpicbg.models.CoordinateTransformMesh;
import mpicbg.models.ErrorStatistic;
import mpicbg.models.HomographyModel2D;
import mpicbg.models.InvertibleCoordinateTransform;
import mpicbg.models.MovingLeastSquaresTransform;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import mpicbg.models.RigidModel2D;
import mpicbg.models.SimilarityModel2D;
import mpicbg.models.Spring;
import mpicbg.models.SpringMesh;
import mpicbg.models.Tile;
import mpicbg.models.TileConfiguration;
import mpicbg.models.Transforms;
import mpicbg.models.TranslationModel2D;
import mpicbg.models.Vertex;
import mpicbg.util.Util;

/**
 * @author Stephan Saalfeld &lt;saalfeld@mpi-cbg.de&gt;
 * @version 0.1a
 */
public class ElasticAlign implements PlugIn, KeyListener
{
	final static private class Triple< A, B, C >
	{
		final public A a;
		final public B b;
		final public C c;

		Triple( final A a, final B b, final C c )
		{
			this.a = a;
			this.b = b;
			this.c = c;
		}
	}

	final static private class Param implements Serializable
	{
		private static final long serialVersionUID = -4772614223784150836L;

		public String outputPath = "";

		final public FloatArray2DSIFT.Param sift = new FloatArray2DSIFT.Param();
		{
			sift.fdSize = 8;
		}

		public int maxNumThreadsSift = Runtime.getRuntime().availableProcessors();

		/**
		 * Closest/next closest neighbor distance ratio
		 */
		public float rod = 0.92f;

		/**
		 * Maximal accepted alignment error in px
		 */
		public float maxEpsilon = 200.0f;

		/**
		 * Inlier/candidates ratio
		 */
		public float minInlierRatio = 0.0f;

		/**
		 * Minimal absolute number of inliers
		 */
		public int minNumInliers = 12;

		/**
		 * Transformation models for choice
		 */
		final static public String[] modelStrings = new String[]{ "Translation", "Rigid", "Similarity", "Affine", "Perspective" };
		public int modelIndex = 3;

		/**
		 * Ignore identity transform up to a given tolerance
		 */
		public boolean rejectIdentity = true;
		public float identityTolerance = 5.0f;

		/**
		 * Maximal number of consecutive sections to be tested for an alignment model
		 */
		public int maxNumNeighbors = 10;

		/**
		 * Maximal number of consecutive slices for which no model could be found
		 */
		public int maxNumFailures = 3;

		public double sectionScale = -1;
		public float minR = 0.8f;
		public float maxCurvatureR = 3f;
		public float rodR = 0.8f;
		public int searchRadius = 200;
		public int blockRadius = -1;

		public boolean useLocalSmoothnessFilter = true;
		public int localModelIndex = 1;
		public float localRegionSigma = searchRadius / 4;
		public float maxLocalEpsilon = searchRadius / 4;
		public float maxLocalTrust = 3;

		public boolean mask = false;

		public int modelIndexOptimize = 1;
		public int maxIterationsOptimize = 1000;
		public int maxPlateauwidthOptimize = 200;

		public int resolutionSpringMesh = 16;
		public double stiffnessSpringMesh = 0.1;
		public double dampSpringMesh = 0.9;
		public double maxStretchSpringMesh = 2000.0;
		public int maxIterationsSpringMesh = 1000;
		public int maxPlateauwidthSpringMesh = 200;

		public boolean interpolate = true;
		public boolean visualize = true;
		public int resolutionOutput = 128;
		public boolean rgbWithGreenBackground = false;

		public boolean clearCache = true;

		public int maxNumThreads = Runtime.getRuntime().availableProcessors();

		public boolean isAligned = false;

		public boolean setup( final ImagePlus imp )
		{
			DirectoryChooser.setDefaultDirectory( outputPath );
			final DirectoryChooser dc = new DirectoryChooser( "Elastically align stack: Output directory" );
			outputPath = dc.getDirectory();
			if ( outputPath == null )
			{
				outputPath = "";
				return false;
			}
			else
			{
				final File d = new File( outputPath );
				if ( d.exists() && d.isDirectory() )
					outputPath += "/";
				else
					return false;
			}

			final GenericDialog gdOutput = new GenericDialog( "Elastically align stack: Output" );

			gdOutput.addCheckbox( "interpolate", interpolate );
			gdOutput.addCheckbox( "visualize", visualize );
			gdOutput.addNumericField( "resolution :", resolutionOutput, 0 );
			gdOutput.addCheckbox( "render RGB with green background", rgbWithGreenBackground );

			gdOutput.showDialog();

			if ( gdOutput.wasCanceled() )
				return false;

			interpolate = gdOutput.getNextBoolean();
			visualize = gdOutput.getNextBoolean();
			resolutionOutput = ( int )gdOutput.getNextNumber();
			rgbWithGreenBackground = gdOutput.getNextBoolean();



			/* Block Matching */
			if ( sectionScale < 0 )
			{
				final Calibration calib = imp.getCalibration();
				sectionScale = calib.pixelWidth / calib.pixelDepth;
			}
			if ( blockRadius < 0 )
			{
				blockRadius = imp.getWidth() / resolutionSpringMesh / 2;
			}
			final GenericDialog gdBlockMatching = new GenericDialog( "Elastically align stack: Block Matching parameters" );

			gdBlockMatching.addMessage( "Block Matching:" );
			gdBlockMatching.addNumericField( "scale :", sectionScale, 2 );
			gdBlockMatching.addNumericField( "search_radius :", searchRadius, 0, 6, "px" );
			gdBlockMatching.addNumericField( "block_radius :", blockRadius, 0, 6, "px" );
			gdBlockMatching.addNumericField( "resolution :", resolutionSpringMesh, 0 );

			gdBlockMatching.addMessage( "Correlation Filters:" );
			gdBlockMatching.addNumericField( "minimal_PMCC_r :", minR, 2 );
			gdBlockMatching.addNumericField( "maximal_curvature_ratio :", maxCurvatureR, 2 );
			gdBlockMatching.addNumericField( "maximal_second_best_r/best_r :", rodR, 2 );

			gdBlockMatching.addMessage( "Local Smoothness Filter:" );
			gdBlockMatching.addCheckbox( "use_local_smoothness_filter", useLocalSmoothnessFilter );
			gdBlockMatching.addChoice( "approximate_local_transformation :", Param.modelStrings, Param.modelStrings[ localModelIndex ] );
			gdBlockMatching.addNumericField( "local_region_sigma:", localRegionSigma, 2, 6, "px" );
			gdBlockMatching.addNumericField( "maximal_local_displacement (absolute):", maxLocalEpsilon, 2, 6, "px" );
			gdBlockMatching.addNumericField( "maximal_local_displacement (relative):", maxLocalTrust, 2 );

			gdBlockMatching.addMessage( "Miscellaneous:" );
			gdBlockMatching.addCheckbox( "green_mask_(TODO_more_colors)", mask );
			gdBlockMatching.addCheckbox( "series_is_aligned", isAligned );
			gdBlockMatching.addNumericField( "test_maximally :", maxNumNeighbors, 0, 6, "layers" );


			gdBlockMatching.showDialog();

			if ( gdBlockMatching.wasCanceled() )
				return false;

			sectionScale = gdBlockMatching.getNextNumber();
			searchRadius = ( int )gdBlockMatching.getNextNumber();
			blockRadius = ( int )gdBlockMatching.getNextNumber();
			resolutionSpringMesh = ( int )gdBlockMatching.getNextNumber();
			minR = ( float )gdBlockMatching.getNextNumber();
			maxCurvatureR = ( float )gdBlockMatching.getNextNumber();
			rodR = ( float )gdBlockMatching.getNextNumber();
			useLocalSmoothnessFilter = gdBlockMatching.getNextBoolean();
			localModelIndex = gdBlockMatching.getNextChoiceIndex();
			localRegionSigma = ( float )gdBlockMatching.getNextNumber();
			maxLocalEpsilon = ( float )gdBlockMatching.getNextNumber();
			maxLocalTrust = ( float )gdBlockMatching.getNextNumber();
			mask = gdBlockMatching.getNextBoolean();
			isAligned = gdBlockMatching.getNextBoolean();
			maxNumNeighbors = ( int )gdBlockMatching.getNextNumber();


			if ( !isAligned )
			{
				/* SIFT */
				final GenericDialog gdSIFT = new GenericDialog( "Elastically align stack: SIFT parameters" );

				SIFT.addFields( gdSIFT, sift );

				gdSIFT.addMessage( "Local Descriptor Matching:" );
				gdSIFT.addNumericField( "closest/next_closest_ratio :", rod, 2 );

				gdSIFT.addMessage( "Miscellaneous:" );
				gdSIFT.addCheckbox( "clear_cache", clearCache );
				gdSIFT.addNumericField( "feature_extraction_threads :", maxNumThreadsSift, 0 );

				gdSIFT.showDialog();

				if ( gdSIFT.wasCanceled() )
					return false;

				SIFT.readFields( gdSIFT, sift );

				rod = ( float )gdSIFT.getNextNumber();
				clearCache = gdSIFT.getNextBoolean();
				maxNumThreadsSift = ( int )gdSIFT.getNextNumber();


				/* Geometric filters */

				final GenericDialog gdGeom = new GenericDialog( "Elastically align stack: Geometric filters" );

				gdGeom.addNumericField( "maximal_alignment_error :", maxEpsilon, 2, 6, "px" );
				gdGeom.addNumericField( "minimal_inlier_ratio :", minInlierRatio, 2 );
				gdGeom.addNumericField( "minimal_number_of_inliers :", minNumInliers, 0 );
				gdGeom.addChoice( "approximate_transformation :", Param.modelStrings, Param.modelStrings[ modelIndex ] );
				gdGeom.addCheckbox( "ignore constant background", rejectIdentity );
				gdGeom.addNumericField( "tolerance :", identityTolerance, 2, 6, "px" );

				gdGeom.addNumericField( "give_up_after :", maxNumFailures, 0, 6, "failures" );

				gdGeom.showDialog();

				if ( gdGeom.wasCanceled() )
					return false;

				maxEpsilon = ( float )gdGeom.getNextNumber();
				minInlierRatio = ( float )gdGeom.getNextNumber();
				minNumInliers = ( int )gdGeom.getNextNumber();
				modelIndex = gdGeom.getNextChoiceIndex();
				rejectIdentity = gdGeom.getNextBoolean();
				identityTolerance = ( float )gdGeom.getNextNumber();
				maxNumFailures = ( int )gdGeom.getNextNumber();
			}


			/* Optimization */
			final GenericDialog gdOptimize = new GenericDialog( "Elastically align stack: Optimization" );

			gdOptimize.addMessage( "Approximate Optimizer:" );
			gdOptimize.addChoice( "approximate_transformation :", Param.modelStrings, Param.modelStrings[ modelIndexOptimize ] );
			gdOptimize.addNumericField( "maximal_iterations :", maxIterationsOptimize, 0 );
			gdOptimize.addNumericField( "maximal_plateauwidth :", maxPlateauwidthOptimize, 0 );

			gdOptimize.addMessage( "Spring Mesh:" );
			gdOptimize.addNumericField( "stiffness :", stiffnessSpringMesh, 2 );
			gdOptimize.addNumericField( "maximal_stretch :", maxStretchSpringMesh, 2, 6, "px" );
			gdOptimize.addNumericField( "maximal_iterations :", maxIterationsSpringMesh, 0 );
			gdOptimize.addNumericField( "maximal_plateauwidth :", maxPlateauwidthSpringMesh, 0 );

			gdOptimize.showDialog();

			if ( gdOptimize.wasCanceled() )
				return false;

			modelIndexOptimize = gdOptimize.getNextChoiceIndex();
			maxIterationsOptimize = ( int )gdOptimize.getNextNumber();
			maxPlateauwidthOptimize = ( int )gdOptimize.getNextNumber();

			stiffnessSpringMesh = gdOptimize.getNextNumber();
			maxStretchSpringMesh = gdOptimize.getNextNumber();
			maxIterationsSpringMesh = ( int )gdOptimize.getNextNumber();
			maxPlateauwidthSpringMesh = ( int )gdOptimize.getNextNumber();

			return true;
		}

		public boolean equalSiftPointMatchParams( final Param param )
		{
			return sift.equals( param.sift )
			    && maxEpsilon == param.maxEpsilon
			    && minInlierRatio == param.minInlierRatio
			    && minNumInliers == param.minNumInliers
			    && modelIndex == param.modelIndex
				&& rejectIdentity == param.rejectIdentity
				&& identityTolerance == param.identityTolerance
				&& maxNumNeighbors == param.maxNumNeighbors
				&& maxNumFailures == param.maxNumFailures;
		}
	}

	final static public AbstractModel< ? > createModel( final int modelIndex )
	{
		switch ( modelIndex )
		{
		case 0:
			return new TranslationModel2D();
		case 1:
			return new RigidModel2D();
		case 2:
			return new SimilarityModel2D();
		case 3:
			return new AffineModel2D();
		case 4:
			return new HomographyModel2D();
		default:
			return null;
		}
	}

	final static Param p = new Param();

	@Override
	final public void run( final String args )
	{
		if ( IJ.versionLessThan( "1.41n" ) ) return;
		try { run(); }
		catch ( final Throwable t ) { t.printStackTrace(); }
	}

	final public void run() throws Exception
	{
		final ImagePlus imp = WindowManager.getCurrentImage();
		if ( imp == null )
		{
			System.err.println( "There are no images open" );
			return;
		}

		if ( !p.setup( imp ) ) return;

		final ImageStack stack = imp.getStack();
		final double displayRangeMin = imp.getDisplayRangeMin();
		final double displayRangeMax = imp.getDisplayRangeMax();

		final ArrayList< Tile< ? > > tiles = new ArrayList< Tile<?> >();
		for ( int i = 0; i < stack.getSize(); ++i )
		{
			switch ( p.modelIndexOptimize )
			{
			case 0:
				tiles.add( new Tile< TranslationModel2D >( new TranslationModel2D() ) );
				break;
			case 1:
				tiles.add( new Tile< RigidModel2D >( new RigidModel2D() ) );
				break;
			case 2:
				tiles.add( new Tile< SimilarityModel2D >( new SimilarityModel2D() ) );
				break;
			case 3:
				tiles.add( new Tile< AffineModel2D >( new AffineModel2D() ) );
				break;
			case 4:
				tiles.add( new Tile< HomographyModel2D >( new HomographyModel2D() ) );
				break;
			default:
				return;
			}
		}

		final ArrayList< Triple< Integer, Integer, AbstractModel< ? > > > pairs = new ArrayList< Triple< Integer, Integer, AbstractModel< ? > > >();

		if ( !p.isAligned )
		{
			final ExecutorService execSift = Executors.newFixedThreadPool( p.maxNumThreadsSift );

			/* extract features for all slices and store them to disk */
			final AtomicInteger counter = new AtomicInteger( 0 );
			final ArrayList< Future< ArrayList< Feature > > > siftTasks = new ArrayList< Future< ArrayList< Feature > > >();

			for ( int i = 1; i <= stack.getSize(); i++ )
			{
				final int slice = i;
				siftTasks.add(
						execSift.submit( new Callable< ArrayList< Feature > >()
						{
							@Override
							public ArrayList< Feature > call()
							{
								IJ.showProgress( counter.getAndIncrement(), stack.getSize() );

								//final String path = p.outputPath + stack.getSliceLabel( slice ) + ".features";
								final String path = p.outputPath + String.format( "%05d", slice - 1 ) + ".features";
								ArrayList< Feature > fs = null;
								if ( !p.clearCache )
									fs = deserializeFeatures( p.sift, path );
								if ( fs == null )
								{
									final FloatArray2DSIFT sift = new FloatArray2DSIFT( p.sift );
									final SIFT ijSIFT = new SIFT( sift );
									fs = new ArrayList< Feature >();
									final ImageProcessor ip = stack.getProcessor( slice );
									ip.setMinAndMax( displayRangeMin, displayRangeMax );
									ijSIFT.extractFeatures( ip, fs );

									if ( !serializeFeatures( p.sift, fs, path ) )
									{
										//IJ.log( "FAILED to store serialized features for " + stack.getSliceLabel( slice ) );
										IJ.log( "FAILED to store serialized features for " + String.format( "%05d", slice - 1 ) );
									}
								}
								//IJ.log( fs.size() + " features extracted for slice " + stack.getSliceLabel ( slice ) );
								IJ.log( fs.size() + " features extracted for slice " + String.format( "%05d", slice - 1 ) );

								return fs;
							}
						} ) );
			}

			/* join */
			for ( final Future< ArrayList< Feature > > fu : siftTasks )
				fu.get();

			siftTasks.clear();
			execSift.shutdown();


			/* collect all pairs of slices for which a model could be found */

			counter.set( 0 );
			int numFailures = 0;

			for ( int i = 0; i < stack.getSize(); ++i )
			{
				final ArrayList< Thread > threads = new ArrayList< Thread >( p.maxNumThreads );

				final int sliceA = i;
				final int range = Math.min( stack.getSize(), i + p.maxNumNeighbors + 1 );

J:				for ( int j = i + 1; j < range; )
				{
					final int numThreads = Math.min( p.maxNumThreads, range - j );
					final ArrayList< Triple< Integer, Integer, AbstractModel< ? > > > models =
						new ArrayList< Triple< Integer, Integer, AbstractModel< ? > > >( numThreads );

					for ( int k = 0; k < numThreads; ++k )
						models.add( null );

					for ( int t = 0;  t < numThreads && j < range; ++t, ++j )
					{
						final int ti = t;
						final int sliceB = j;

						final Thread thread = new Thread()
						{
							@Override
							public void run()
							{
								IJ.showProgress( sliceA, stack.getSize() - 1 );

								IJ.log( "matching " + sliceB + " -> " + sliceA + "..." );

								ArrayList< PointMatch > candidates = null;
								final String path = p.outputPath + String.format( "%05d", sliceB ) + "-" + String.format( "%05d", sliceA ) + ".pointmatches";
								if ( !p.clearCache )
									candidates = deserializePointMatches( p, path );

								if ( null == candidates )
								{
									final ArrayList< Feature > fs1 = deserializeFeatures( p.sift, p.outputPath + String.format( "%05d", sliceA ) + ".features" );
									final ArrayList< Feature > fs2 = deserializeFeatures( p.sift, p.outputPath + String.format( "%05d", sliceB ) + ".features" );
									candidates = new ArrayList< PointMatch >( FloatArray2DSIFT.createMatches( fs2, fs1, p.rod ) );

									if ( !serializePointMatches( p, candidates, path ) )
										IJ.log( "Could not store point matches!" );
								}

								final AbstractModel< ? > model = createModel( p.modelIndex );
								if ( model == null ) return;

								final ArrayList< PointMatch > inliers = new ArrayList< PointMatch >();

								boolean modelFound;
								boolean again = false;
								try
								{
									do
									{
										modelFound = model.filterRansac(
												candidates,
												inliers,
												1000,
												p.maxEpsilon,
												p.minInlierRatio,
												p.minNumInliers,
												3 );
										if ( modelFound && p.rejectIdentity )
										{
											final ArrayList< Point > points = new ArrayList< Point >();
											PointMatch.sourcePoints( inliers, points );
											if ( Transforms.isIdentity( model, points, p.identityTolerance ) )
											{
												IJ.log( "Identity transform for " + inliers.size() + " matches rejected." );
												candidates.removeAll( inliers );
												inliers.clear();
												again = true;
											}
										}
									}
									while ( again );
								}
								catch ( final Exception e )
								{
									modelFound = false;
									System.err.println( e.getMessage() );
								}

								if ( modelFound )
								{
									IJ.log( sliceB + " -> " + sliceA + ": " + inliers.size() + " corresponding features with an average displacement of " + PointMatch.meanDistance( inliers ) + "px identified." );
									IJ.log( "Estimated transformation model: " + model );
									models.set( ti, new Triple< Integer, Integer, AbstractModel< ? > >( sliceA, sliceB, model ) );
								}
								else
								{
									IJ.log( sliceB + " -> " + sliceA + ": no correspondences found." );
									return;
								}
							}
						};
						threads.add( thread );
						thread.start();
					}

					try
					{
						for ( final Thread thread : threads )
							thread.join();
					}
					catch ( final InterruptedException e )
					{
						IJ.log( "Establishing feature correspondences interrupted." );
						for ( final Thread thread : threads )
							thread.interrupt();
						try
						{
							for ( final Thread thread : threads )
								thread.join();
						}
						catch ( final InterruptedException f ) {}
						return;
					}

					threads.clear();

					/* collect successfully matches pairs and break the search on gaps */
					for ( int t = 0; t < models.size(); ++t )
					{
						final Triple< Integer, Integer, AbstractModel< ? > > pair = models.get( t );
						if ( pair == null )
						{
							if ( ++numFailures > p.maxNumFailures )
								break J;
						}
						else
						{
							numFailures = 0;
							pairs.add( pair );
						}
					}
				}
			}
		}
		else
		{
			for ( int i = 0; i < stack.getSize(); ++i )
			{
				final int range = Math.min( stack.getSize(), i + p.maxNumNeighbors + 1 );

				for ( int j = i + 1; j < range; ++j )
				{
					pairs.add( new Triple< Integer, Integer, AbstractModel< ? > >( i, j, new TranslationModel2D() ) );
				}
			}
		}

		/* Elastic alignment */

		/* Initialization */
		final TileConfiguration initMeshes = new TileConfiguration();

		final ArrayList< SpringMesh > meshes = new ArrayList< SpringMesh >( stack.getSize() );
		for ( int i = 0; i < stack.getSize(); ++i )
			meshes.add(
					new SpringMesh(
							p.resolutionSpringMesh,
							stack.getWidth(),
							stack.getHeight(),
							p.stiffnessSpringMesh,
							p.maxStretchSpringMesh,
							p.dampSpringMesh ) );

//		final int blockRadius = Math.max( 32, stack.getWidth() / p.resolutionSpringMesh / 2 );
		final int blockRadius = Math.max( Util.roundPos( 16 / p.sectionScale ), p.blockRadius );

		/** TODO set this something more than the largest error by the approximate model */
		final int searchRadius = p.searchRadius;

		final AbstractModel< ? > localSmoothnessFilterModel = createModel( p.localModelIndex );

		for ( final Triple< Integer, Integer, AbstractModel< ? > > pair : pairs )
		{
			final SpringMesh m1 = meshes.get( pair.a );
			final SpringMesh m2 = meshes.get( pair.b );

			final ArrayList< PointMatch > pm12 = new ArrayList< PointMatch >();
			final ArrayList< PointMatch > pm21 = new ArrayList< PointMatch >();

			final ArrayList< Vertex > v1 = m1.getVertices();
			final ArrayList< Vertex > v2 = m2.getVertices();

			final FloatProcessor ip1 = ( FloatProcessor )stack.getProcessor( pair.a + 1 ).convertToFloat().duplicate();
			final FloatProcessor ip2 = ( FloatProcessor )stack.getProcessor( pair.b + 1 ).convertToFloat().duplicate();
			final FloatProcessor ip1Mask;
			final FloatProcessor ip2Mask;

			if ( imp.getType() == ImagePlus.COLOR_RGB && p.mask )
			{
				ip1Mask = createMask( stack.getProcessor( pair.a + 1 ) );
				ip2Mask = createMask( stack.getProcessor( pair.b + 1 ) );
			}
			else
			{
				ip1Mask = null;
				ip2Mask = null;
			}

			try
			{
				BlockMatching.matchByMaximalPMCC(
						ip1,
						ip2,
						ip1Mask,
						ip2Mask,
						Math.min( 1.0, p.sectionScale ),
						( ( InvertibleCoordinateTransform )pair.c ).createInverse(),
						blockRadius,
						blockRadius,
						searchRadius,
						searchRadius,
						p.minR,
						p.rodR,
						p.maxCurvatureR,
						v1,
						pm12,
						new ErrorStatistic( 1 ) );
			}
			catch ( final InterruptedException e )
			{
				IJ.log( "Block matching interrupted." );
				IJ.showProgress( 1.0 );
				return;
			}
			if ( Thread.interrupted() )
			{
				IJ.log( "Block matching interrupted." );
				IJ.showProgress( 1.0 );
				return;
			}

			if ( p.useLocalSmoothnessFilter )
			{
				IJ.log( pair.a + " > " + pair.b + ": found " + pm12.size() + " correspondence candidates." );
				localSmoothnessFilterModel.localSmoothnessFilter( pm12, pm12, p.localRegionSigma, p.maxLocalEpsilon, p.maxLocalTrust );
				IJ.log( pair.a + " > " + pair.b + ": " + pm12.size() + " candidates passed local smoothness filter." );
			}
			else
			{
				IJ.log( pair.a + " > " + pair.b + ": found " + pm12.size() + " correspondences." );
			}

			/* <visualisation> */
			//			final List< Point > s1 = new ArrayList< Point >();
			//			PointMatch.sourcePoints( pm12, s1 );
			//			final ImagePlus imp1 = new ImagePlus( i + " >", ip1 );
			//			imp1.show();
			//			imp1.setOverlay( BlockMatching.illustrateMatches( pm12 ), Color.yellow, null );
			//			imp1.setRoi( Util.pointsToPointRoi( s1 ) );
			//			imp1.updateAndDraw();
			/* </visualisation> */

			try
			{
				BlockMatching.matchByMaximalPMCC(
						ip2,
						ip1,
						ip2Mask,
						ip1Mask,
						Math.min( 1.0, p.sectionScale ),
						pair.c,
						blockRadius,
						blockRadius,
						searchRadius,
						searchRadius,
						p.minR,
						p.rodR,
						p.maxCurvatureR,
						v2,
						pm21,
						new ErrorStatistic( 1 ) );
			}
			catch ( final InterruptedException e )
			{
				IJ.log( "Block matching interrupted." );
				IJ.showProgress( 1.0 );
				return;
			}
			if ( Thread.interrupted() )
			{
				IJ.log( "Block matching interrupted." );
				IJ.showProgress( 1.0 );
				return;
			}

			if ( p.useLocalSmoothnessFilter )
			{
				IJ.log( pair.a + " < " + pair.b + ": found " + pm21.size() + " correspondence candidates." );
				localSmoothnessFilterModel.localSmoothnessFilter( pm21, pm21, p.localRegionSigma, p.maxLocalEpsilon, p.maxLocalTrust );
				IJ.log( pair.a + " < " + pair.b + ": " + pm21.size() + " candidates passed local smoothness filter." );
			}
			else
			{
				IJ.log( pair.a + " < " + pair.b + ": found " + pm21.size() + " correspondences." );
			}

			/* <visualisation> */
			//			final List< Point > s2 = new ArrayList< Point >();
			//			PointMatch.sourcePoints( pm21, s2 );
			//			final ImagePlus imp2 = new ImagePlus( i + " <", ip2 );
			//			imp2.show();
			//			imp2.setOverlay( BlockMatching.illustrateMatches( pm21 ), Color.yellow, null );
			//			imp2.setRoi( Util.pointsToPointRoi( s2 ) );
			//			imp2.updateAndDraw();
			/* </visualisation> */

			final double springConstant  = 1.0 / ( pair.b - pair.a );
			IJ.log( pair.a + " <> " + pair.b + " spring constant = " + springConstant );

			for ( final PointMatch pm : pm12 )
			{
				final Vertex p1 = ( Vertex )pm.getP1();
				final Vertex p2 = new Vertex( pm.getP2() );
				p1.addSpring( p2, new Spring( 0, springConstant ) );
				m2.addPassiveVertex( p2 );
			}

			for ( final PointMatch pm : pm21 )
			{
				final Vertex p1 = ( Vertex )pm.getP1();
				final Vertex p2 = new Vertex( pm.getP2() );
				p1.addSpring( p2, new Spring( 0, springConstant ) );
				m1.addPassiveVertex( p2 );
			}

			final Tile< ? > t1 = tiles.get( pair.a );
			final Tile< ? > t2 = tiles.get( pair.b );

			if ( pm12.size() > pair.c.getMinNumMatches() )
			{
				initMeshes.addTile( t1 );
				initMeshes.addTile( t2 );
				t1.connect( t2, pm12 );
			}
			if ( pm21.size() > pair.c.getMinNumMatches() )
			{
				initMeshes.addTile( t1 );
				initMeshes.addTile( t2 );
				t2.connect( t1, pm21 );
			}
		}

		/* pre-align by optimizing a piecewise linear model */
		initMeshes.optimize(
				p.maxEpsilon,
				p.maxIterationsSpringMesh,
				p.maxPlateauwidthSpringMesh );
		for ( int i = 0; i < stack.getSize(); ++i )
			meshes.get( i ).init( tiles.get( i ).getModel() );

		/* optimize the meshes */
		try
		{
			final long t0 = System.currentTimeMillis();
			IJ.log("Optimizing spring meshes...");

			SpringMesh.optimizeMeshes(
					meshes,
					p.maxEpsilon,
					p.maxIterationsSpringMesh,
					p.maxPlateauwidthSpringMesh,
					p.visualize );

			IJ.log( "Done optimizing spring meshes. Took " + ( System.currentTimeMillis() - t0 ) + " ms" );

		}
		catch ( final NotEnoughDataPointsException e )
		{
			IJ.log( "There were not enough data points to get the spring mesh optimizing." );
			e.printStackTrace();
			return;
		}

		/* calculate bounding box */
		final double[] min = new double[ 2 ];
		final double[] max = new double[ 2 ];
		for ( final SpringMesh mesh : meshes )
		{
			final double[] meshMin = new double[ 2 ];
			final double[] meshMax = new double[ 2 ];

			mesh.bounds( meshMin, meshMax );

			Util.min( min, meshMin );
			Util.max( max, meshMax );
		}

		/* translate relative to bounding box */
		for ( final SpringMesh mesh : meshes )
		{
			for ( final Vertex vertex : mesh.getVertices() )
			{
				final double[] w = vertex.getW();
				w[ 0 ] -= min[ 0 ];
				w[ 1 ] -= min[ 1 ];
			}
			mesh.updateAffines();
			mesh.updatePassiveVertices();
		}

		final int width = ( int )Math.ceil( max[ 0 ] - min[ 0 ] );
		final int height = ( int )Math.ceil( max[ 1 ] - min[ 1 ] );
		for ( int i = 0; i < stack.getSize(); ++i )
		{
			final int slice  = i + 1;

			final MovingLeastSquaresTransform mlt = new MovingLeastSquaresTransform();
			mlt.setModel( AffineModel2D.class );
			mlt.setAlpha( 2.0f );
			mlt.setMatches( meshes.get( i ).getVA().keySet() );

			final CoordinateTransformMesh mltMesh = new CoordinateTransformMesh( mlt, p.resolutionOutput, stack.getWidth(), stack.getHeight() );
			final TransformMeshMapping< CoordinateTransformMesh > mltMapping = new TransformMeshMapping< CoordinateTransformMesh >( mltMesh );

			final ImageProcessor source, target;
			if ( p.rgbWithGreenBackground )
			{
				target = new ColorProcessor( width, height );
				for ( int j = width * height - 1; j >=0; --j )
					target.set( j, 0xff00ff00 );
				source = stack.getProcessor( slice ).convertToRGB();
			}
			else
			{
				target = stack.getProcessor( slice ).createProcessor( width, height );
				source = stack.getProcessor( slice );
			}

			if ( p.interpolate )
			{
				mltMapping.mapInterpolated( source, target );
			}
			else
			{
				mltMapping.map( source, target );
			}
			final ImagePlus impTarget = new ImagePlus( "elastic mlt " + i, target );
			if ( p.visualize )
			{
				final Shape shape = mltMesh.illustrateMesh();
				impTarget.setOverlay( shape, IJ.getInstance().getForeground(), new BasicStroke( 1 ) );
			}
			IJ.save( impTarget, p.outputPath + "elastic-" + String.format( "%05d", i ) + ".tif" );
		}

		IJ.log( "Done." );
	}

	static private FloatProcessor createMask( final ImageProcessor source )
	{
		final FloatProcessor mask = new FloatProcessor( source.getWidth(), source.getHeight() );
		final int maskColor = 0x0000ff00;
		final int n = source.getWidth() * source.getHeight();
		final float[] maskPixels = ( float[] )mask.getPixels();
		for ( int i = 0; i < n; ++i )
		{
			final int sourcePixel = source.get( i ) & 0x00ffffff;
			if ( sourcePixel == maskColor )
				maskPixels[ i ] = 0;
			else
				maskPixels[ i ] = 1;
		}
		return mask;
	}

	@Override
	public void keyPressed(final KeyEvent e)
	{
		if (
				( e.getKeyCode() == KeyEvent.VK_F1 ) &&
				( e.getSource() instanceof TextField ) )
		{

		}
	}

	@Override
	public void keyReleased(final KeyEvent e) { }

	@Override
	public void keyTyped(final KeyEvent e) { }



	final static private class Features implements Serializable
	{
		private static final long serialVersionUID = 2668666732018368958L;

		final FloatArray2DSIFT.Param param;
		final ArrayList< Feature > features;
		Features( final FloatArray2DSIFT.Param p, final ArrayList< Feature > features )
		{
			this.param = p;
			this.features = features;
		}
	}

	final static private boolean serializeFeatures(
			final FloatArray2DSIFT.Param param,
			final ArrayList< Feature > fs,
			final String path )
	{
		return serialize( new Features( param, fs ), path );
	}

	final static private ArrayList< Feature > deserializeFeatures( final FloatArray2DSIFT.Param param, final String path )
	{
		final Object o = deserialize( path );
		if ( null == o ) return null;
		final Features fs = (Features) o;
		if ( param.equals( fs.param ) )
			return fs.features;
		return null;
	}

	final static private class PointMatches implements Serializable
	{
		private static final long serialVersionUID = 3718614767497404447L;

		ElasticAlign.Param param;
		ArrayList< PointMatch > pointMatches;
		PointMatches( final ElasticAlign.Param p, final ArrayList< PointMatch > pointMatches )
		{
			this.param = p;
			this.pointMatches = pointMatches;
		}
	}

	final static private boolean serializePointMatches(
			final ElasticAlign.Param param,
			final ArrayList< PointMatch > pms,
			final String path )
	{
		return serialize( new PointMatches( param, pms ), path );
	}

	final static private ArrayList< PointMatch > deserializePointMatches( final ElasticAlign.Param param, final String path )
	{
		final Object o = deserialize( path );
		if ( null == o ) return null;
		final PointMatches pms = (PointMatches) o;
		if ( param.equalSiftPointMatchParams( pms.param ) )
			return pms.pointMatches;
		return null;
	}

	/** Serializes the given object into the path. Returns false on failure. */
	static public boolean serialize(final Object ob, final String path) {
		try {
			// 1 - Check that the parent chain of folders exists, and attempt to create it when not:
			final File fdir = new File(path).getParentFile();
			if (null == fdir) return false;
			fdir.mkdirs();
			if (!fdir.exists()) {
				IJ.log("Could not create folder " + fdir.getAbsolutePath());
				return false;
			}
			// 2 - Serialize the given object:
			final ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(path));
			out.writeObject(ob);
			out.close();
			return true;
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/** Attempts to find a file containing a serialized object. Returns null if no suitable file is found, or an error occurs while deserializing. */
	static public Object deserialize(final String path) {
		try {
			if (!new File(path).exists()) return null;
			final ObjectInputStream in = new ObjectInputStream(new FileInputStream(path));
			final Object ob = in.readObject();
			in.close();
			return ob;
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
