
import mpicbg.ij.SIFT;
import mpicbg.ij.TransformMeshMapping;
import mpicbg.ij.blockmatching.BlockMatching;
import mpicbg.imagefeatures.*;
import mpicbg.models.*;
import mpicbg.util.Util;

import ij.plugin.*;
import ij.gui.*;
import ij.*;
import ij.process.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.awt.TextField;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1a
 */
public class Elastic_Align implements PlugIn, KeyListener
{
	static private class Param implements Serializable
	{
		private static final long serialVersionUID = 1288115190817093499L;

		final public FloatArray2DSIFT.Param sift = new FloatArray2DSIFT.Param();
		
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
		public float minInlierRatio = 0.1f;
		
		/**
		 * Minimal absolute number of inliers
		 */
		public int minNumInliers = 7;
		
		/**
		 * Implemeted transformation models for choice
		 */
		final static public String[] modelStrings = new String[]{ "Translation", "Rigid", "Similarity", "Affine" };
		public int modelIndex = 1;
		
		public float minR = 0.7f;
		public float maxCurvatureR = 5f;
		public float rodR = 0.8f;
		
		public int springMeshResolution = 16;
		public float stiffness = 0.1f;
		public float springMeshDamp = 0.6f;
		public float maxStretch = 2000.0f;
		public int maxIterations = 1000;
		public int maxPlateauwidth = 200;
		
		public boolean interpolate = true;
		
		public boolean animate = false;
		
		public boolean setup()
		{
			final GenericDialog gd = new GenericDialog( "Elastically align stack: SIFT parameters" );
			
			SIFT.addFields( gd, sift );
			
			gd.addNumericField( "closest/next_closest_ratio :", p.rod, 2 );
			
			gd.addMessage( "Geometric Consensus Filter:" );
			gd.addNumericField( "maximal_alignment_error :", p.maxEpsilon, 2, 6, "px" );
			gd.addNumericField( "minimal_inlier_ratio :", p.minInlierRatio, 2 );
			gd.addNumericField( "minimal_number_of_inliers :", p.minNumInliers, 0 );
			gd.addChoice( "approximate_transformation :", Param.modelStrings, Param.modelStrings[ p.modelIndex ] );
			
			gd.showDialog();
			
			if ( gd.wasCanceled() ) return false;
			
			SIFT.readFields( gd, sift );
			
			p.rod = ( float )gd.getNextNumber();
			
			p.maxEpsilon = ( float )gd.getNextNumber();
			p.minInlierRatio = ( float )gd.getNextNumber();
			p.minNumInliers = ( int )gd.getNextNumber();
			p.modelIndex = gd.getNextChoiceIndex();
			
			
			final GenericDialog gdBlockMatching = new GenericDialog( "Elastically align stack: Block Matching parameters" );
			gdBlockMatching.addMessage( "Block Matching:" );
			gdBlockMatching.addNumericField( "minimal R :", minR, 2 );
			gdBlockMatching.addNumericField( "maximal curvature factor :", maxCurvatureR, 2 );
			gdBlockMatching.addNumericField( "closest/next_closest_ratio :", p.rodR, 2 );
			
			gdBlockMatching.addMessage( "Spring Mesh:" );
			gdBlockMatching.addNumericField( "resolution :", springMeshResolution, 0 );
			gdBlockMatching.addNumericField( "stiffness :", stiffness, 2 );
			gdBlockMatching.addNumericField( "maximal_stretch :", maxStretch, 2, 6, "px" );
			gdBlockMatching.addNumericField( "maximal_iterations :", maxIterations, 0 );
			gdBlockMatching.addNumericField( "maximal_plateau_width :", maxPlateauwidth, 0 );
			
			gdBlockMatching.addMessage( "Output:" );
			gdBlockMatching.addCheckbox( "interpolate", p.interpolate );
			gdBlockMatching.addCheckbox( "animate", p.animate );
			
			gdBlockMatching.showDialog();
			
			if ( gdBlockMatching.wasCanceled() ) return false;
			
			
			p.minR = ( float )gdBlockMatching.getNextNumber();
			p.maxCurvatureR = ( float )gdBlockMatching.getNextNumber();
			p.rodR = ( float )gdBlockMatching.getNextNumber();
			
			p.springMeshResolution = ( int )gdBlockMatching.getNextNumber();
			p.stiffness = ( float )gdBlockMatching.getNextNumber();
			p.maxStretch = ( float )gdBlockMatching.getNextNumber();
			p.maxIterations = ( int )gdBlockMatching.getNextNumber();
			p.maxPlateauwidth = ( int )gdBlockMatching.getNextNumber();
			
			p.interpolate = gdBlockMatching.getNextBoolean();
			p.animate = gdBlockMatching.getNextBoolean();
			
			return true;
		}
		
		public boolean equalSiftPointMatchParams( final Param p )
		{
			return sift.equals( p.sift )
			    && maxEpsilon == p.maxEpsilon
			    && minInlierRatio == p.minInlierRatio
			    && minNumInliers == p.minNumInliers
			    && modelIndex == p.modelIndex;
		}
		
		/** Test if parameters for extracting blockmatching PointMatches are the same. */
		public boolean equalBlockmatchingParams( final Param p )
		{
			return sift.maxOctaveSize == p.sift.maxOctaveSize
			    && minR == p.minR
			    && maxCurvatureR == p.maxCurvatureR
			    && rodR == p.rodR;
		}
	}
	
	final static Param p = new Param(); 

	final public void run( final String args )
	{
		if ( IJ.versionLessThan( "1.41n" ) ) return;
		try {
			run();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	final public void run() throws Exception
	{

		final ImagePlus imp = WindowManager.getCurrentImage();
		if ( imp == null )  { System.err.println( "There are no images open" ); return; }
		
		if ( !p.setup() ) return;
		
		final ImageStack stack = imp.getStack();
		
		final ExecutorService exec = Executors.newFixedThreadPool( Runtime.getRuntime().availableProcessors() );
		final ArrayList<Future<?>> tasks = new ArrayList<Future<?>>();
		
		
		// TODO make it choosable
		final String base_path = System.getProperty( "user.dir" ) + "/";
		
		// Extract all features and store them in disk.

		final AtomicInteger counter = new AtomicInteger( 0 );
		
		for ( int i = 1; i <= stack.getSize(); i++ )
		{
			final int slice = i;
			tasks.add( exec.submit( new Callable<Object> () {
				public Object call() {
					IJ.showProgress( counter.getAndIncrement(), stack.getSize() );
					// Extract features
					final String path = base_path + stack.getSliceLabel( slice ) + ".features";
					ArrayList< Feature > fs = deserializeFeatures( p.sift, path );
					if ( null == fs )
					{
						final FloatArray2DSIFT sift = new FloatArray2DSIFT( p.sift );
						final SIFT ijSIFT = new SIFT( sift );
						fs = new ArrayList< Feature >();
						ijSIFT.extractFeatures( stack.getProcessor( slice ), fs );
						// Store features to disk
						if ( ! serializeFeatures( p.sift, fs, path ) )
						{
							IJ.log( "FAILED to store serialized features for " + stack.getSliceLabel( slice ) );
						}
					}
					IJ.log( fs.size() + " features extracted for slice " + stack.getSliceLabel ( slice ) );
					
					return null;
				}
			} ) );
		}

		// Wait until all are complete
		for ( Future<?> fu : tasks ) {
			fu.get();
		}
		tasks.clear();
		
		// Extract transformation model for each consecutive pair of slices
		final InvertibleCoordinateTransform[] transforms = new InvertibleCoordinateTransform[ stack.getSize() - 1 ];

		final SpringMesh[] meshes = new SpringMesh[ stack.getSize() ];
		// A SpringMesh for the first slice
		meshes[ 0 ] = new SpringMesh( p.springMeshResolution, stack.getWidth(), stack.getHeight(), p.stiffness, p.maxStretch, p.springMeshDamp );
		
		counter.set( 0 );
		
		for ( int i = 2; i <= stack.getSize(); i++ )
		{
			final int slice = i;
			tasks.add( exec.submit( new Callable<Object> () {
				public Object call() {
					IJ.showProgress( counter.getAndIncrement(), stack.getSize() - 1 );
					
					String path = base_path + stack.getSliceLabel( slice ) + ".pointmatches";
					ArrayList< PointMatch > candidates = deserializePointMatches( p, path );
					
					if ( null == candidates )
					{
						ArrayList< Feature > fs1 = deserializeFeatures( p.sift, base_path + stack.getSliceLabel( slice - 1 ) + ".features" );
						ArrayList< Feature > fs2 = deserializeFeatures( p.sift, base_path + stack.getSliceLabel( slice ) + ".features" );
						candidates = new ArrayList< PointMatch >( FloatArray2DSIFT.createMatches( fs2, fs1, p.rod ) );
						if ( ! serializePointMatches( p, candidates, path ) )
						{
							IJ.log( "Could not store point matches!" );
						}
					}

					AbstractAffineModel2D<?> model;
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
						return null;
					}
					
					final ArrayList< PointMatch > inliers = new ArrayList< PointMatch >();
					
					boolean modelFound;
					try
					{
						modelFound = model.filterRansac(
								candidates,
								inliers,
								1000,
								p.maxEpsilon,
								p.minInlierRatio );
					}
					catch ( Exception e )
					{
						modelFound = false;
						System.err.println( e.getMessage() );
					}

					if ( modelFound )
					{
						transforms[ slice - 2 ] = model;   // -2: there's one transform less
						//model.concatenate( currentModel );
					}
					// else the model is left null

					meshes[ slice - 1 ] = new SpringMesh( p.springMeshResolution, stack.getWidth(), stack.getHeight(), p.stiffness, p.maxStretch, p.springMeshDamp );
					
					
					/*
					ImageProcessor ip = ...
					final ImageProcessor approximatelyAlignedSlice =
						ip.createProcessor( ip.getWidth(), ip.getHeight() );
				if ( p.interpolate )
					mapping.mapInterpolated( ip, approximatelyAlignedSlice );
				else
					mapping.map( ip, approximatelyAlignedSlice );
				
				IJ.save( new ImagePlus( "linear " + i, approximatelyAlignedSlice ), "linear-" + String.format( "%04d", i ) + ".tif" );
					
					InverseTransformMapping< AbstractAffineModel2D< ? > > mapping = new InverseTransformMapping< AbstractAffineModel2D< ? > >( model );
				*/
				
					return null;
				}
			} ) );
		}
		
		// Wait until all are complete
		for ( Future<?> fu : tasks ) {
			fu.get();
		}
		tasks.clear();
		
		/* Elastic alignment */
		final int blockRadius = Math.max( 32, stack.getWidth() / p.springMeshResolution / 2 );
		
		/** TODO set this something more than the largest error by the approximate model */
		final int searchRadius = Math.round( p.maxEpsilon );
		
		for ( int i = 1; i < stack.getSize(); ++i )
		{
			final int slice = i;
			tasks.add( exec.submit( new Callable< Object>() {
				public Object call() {
					final SpringMesh m1 = meshes[ slice - 1 ];
					final SpringMesh m2 = meshes[ slice ];

					String path12 = base_path + stack.getSliceLabel( slice ) + "--" + stack.getSliceLabel( slice + 1 ) + ".blockmatches";
					String path21 = base_path + stack.getSliceLabel( slice + 1 ) + "--" + stack.getSliceLabel( slice )  + ".blockmatches";
					ArrayList< PointMatch > pm12 = deserializeBlockMatches( p, path12 );
					ArrayList< PointMatch > pm21 = deserializeBlockMatches( p, path21 );

					if (null == pm12 || null == pm21) {
						pm12 = new ArrayList< PointMatch >();
						pm21 = new ArrayList< PointMatch >();

						final Collection< Vertex > v1 = m1.getVertices();
						final Collection< Vertex > v2 = m2.getVertices();

						final FloatProcessor ip1 = ( FloatProcessor )stack.getProcessor( slice ).convertToFloat().duplicate();
						final FloatProcessor ip2 = ( FloatProcessor )stack.getProcessor( slice + 1 ).convertToFloat().duplicate();


						BlockMatching.matchByMaximalPMCC(
								ip1,
								ip2,
								Math.min( 1.0f, ( float )p.sift.maxOctaveSize / ip1.getWidth() ),
								transforms[ slice - 1 ].createInverse(),
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

						IJ.log( "> found " + pm12.size() + " correspondences." );

						/* <visualisation> */
						//			final List< Point > s1 = new ArrayList< Point >();
						//			PointMatch.sourcePoints( pm12, s1 );
						//			final ImagePlus imp1 = new ImagePlus( i + " >", ip1 );
						//			imp1.show();
						//			imp1.setOverlay( BlockMatching.illustrateMatches( pm12 ), Color.yellow, null );
						//			imp1.setRoi( Util.pointsToPointRoi( s1 ) );
						//			imp1.updateAndDraw();
						/* </visualisation> */

						BlockMatching.matchByMaximalPMCC(
								ip2,
								ip1,
								Math.min( 1.0f, ( float )p.sift.maxOctaveSize / ip1.getWidth() ),
								transforms[ slice - 1 ],
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

						IJ.log( "< found " + pm21.size() + " correspondences." );
					}
					
					serializePointMatches( p, pm12, path12 );
					serializePointMatches( p, pm21, path21 );

					/* <visualisation> */
					//			final List< Point > s2 = new ArrayList< Point >();
					//			PointMatch.sourcePoints( pm21, s2 );
					//			final ImagePlus imp2 = new ImagePlus( i + " <", ip2 );
					//			imp2.show();
					//			imp2.setOverlay( BlockMatching.illustrateMatches( pm21 ), Color.yellow, null );
					//			imp2.setRoi( Util.pointsToPointRoi( s2 ) );
					//			imp2.updateAndDraw();
					/* </visualisation> */

					synchronized ( m2 ) {
						for ( final PointMatch pm : pm12 )
						{
							final Vertex p1 = ( Vertex )pm.getP1();
							final Vertex p2 = new Vertex( pm.getP2() );
							p1.addSpring( p2, new Spring( 0, 1 ) );
							m2.addPassiveVertex( p2 );
						}
					}
					
					synchronized ( m1 ) {
						for ( final PointMatch pm : pm21 )
						{
							final Vertex p1 = ( Vertex )pm.getP1();
							final Vertex p2 = new Vertex( pm.getP2() );
							p1.addSpring( p2, new Spring( 0, 1 ) );
							m1.addPassiveVertex( p2 );
						}
					}
					
					return null;
				}
			} ) );
		}
		
		// Wait until all are complete
		for ( Future<?> fu : tasks ) {
			fu.get();
		}
		tasks.clear();
		
		/* initialize meshes */
		/* TODO this is accumulative and thus not perfect, change to analytical concatenation later */
		for ( int i = 1; i < stack.getSize(); ++i )
		{
			final CoordinateTransformList< CoordinateTransform > ctl = new CoordinateTransformList< CoordinateTransform >();
			for ( int j = i - 1; j >= 0; --j )
				ctl.add( transforms[ j ] );
			meshes[ i ].init( ctl );
		}

		/* optimize */
		try {
			long t0 = System.currentTimeMillis();
			IJ.log("Optimizing spring meshes...");
			
			SpringMesh.optimizeMeshes( Arrays.asList( meshes ), p.maxEpsilon, p.maxIterations, p.maxPlateauwidth );

			IJ.log("Done optimizing spring meshes. Took " + (System.currentTimeMillis() - t0) + " ms");
			
		} catch ( NotEnoughDataPointsException e ) { e.printStackTrace(); }
		
		/* calculate bounding box */
		final float[] min = new float[ 2 ];
		final float[] max = new float[ 2 ];
		for ( final SpringMesh mesh : meshes )
		{
			final float[] meshMin = new float[ 2 ];
			final float[] meshMax = new float[ 2 ];
			
			mesh.bounds( meshMin, meshMax );
			
			Util.min( min, meshMin );
			Util.max( max, meshMax );
		}
		
		/* translate relative to bounding box */
		for ( final SpringMesh mesh : meshes )
		{
			for ( final Vertex vertex : mesh.getVertices() )
			{
				final float[] w = vertex.getW();
				w[ 0 ] -= min[ 0 ];
				w[ 1 ] -= min[ 1 ];
			}
			mesh.updateAffines();
			mesh.updatePassiveVertices();
		}
		
		//final ImageStack stackAlignedMeshes = new ImageStack( ( int )Math.ceil( max[ 0 ] - min[ 0 ] ), ( int )Math.ceil( max[ 1 ] - min[ 1 ] ) );
		final int width = ( int )Math.ceil( max[ 0 ] - min[ 0 ] );
		final int height = ( int )Math.ceil( max[ 1 ] - min[ 1 ] );
		for ( int i = 1; i <= stack.getSize(); ++i )
		{
			final TransformMeshMapping< SpringMesh > meshMapping = new TransformMeshMapping< SpringMesh >( meshes[ i - 1 ] );
			final ImageProcessor ip = stack.getProcessor( i ).createProcessor( width, height );
			if ( p.interpolate )
				meshMapping.mapInterpolated( stack.getProcessor( i ), ip );
			else
				meshMapping.map( stack.getProcessor( i ), ip );
			IJ.save( new ImagePlus( "elastic " + i, ip ), "elastic-" + String.format( "%04d", i ) + ".tif" );
			
			//stackAlignedMeshes.addSlice( "" + i, ip );
		}
		
		IJ.log( "Done." );
	}

	public void keyPressed(KeyEvent e)
	{
		if (
				( e.getKeyCode() == KeyEvent.VK_F1 ) &&
				( e.getSource() instanceof TextField ) )
		{
			
		}
	}

	public void keyReleased(KeyEvent e) { }

	public void keyTyped(KeyEvent e) { }
	
	
	
	final static private class Features implements Serializable
	{
		private static final long serialVersionUID = 2689219384710526198L;
		
		final FloatArray2DSIFT.Param p;
		final ArrayList< Feature > features;
		Features( final FloatArray2DSIFT.Param p, final ArrayList< Feature > features )
		{
			this.p = p;
			this.features = features;
		}
	}
	
	final static private boolean serializeFeatures(
			final FloatArray2DSIFT.Param p,
			final ArrayList< Feature > fs,
			final String path )
	{
		return serialize( new Features( p, fs ), path );
	}

	final static private ArrayList< Feature > deserializeFeatures( final FloatArray2DSIFT.Param p, final String path )
	{
		Object o = deserialize( path );
		if ( null == o ) return null;
		Features fs = (Features) o;
		if ( p.equals( fs.p ) )
			return fs.features;
		return null;
	}
	
	final static private class PointMatches implements Serializable
	{
		private static final long serialVersionUID = -2564147268101223484L;
		
		Elastic_Align.Param p;
		ArrayList< PointMatch > pointMatches;
		PointMatches( final Elastic_Align.Param p, final ArrayList< PointMatch > pointMatches )
		{
			this.p = p;
			this.pointMatches = pointMatches;
		}
	}
	
	final static private boolean serializePointMatches(
			final Elastic_Align.Param p,
			final ArrayList< PointMatch > pms,
			final String path )
	{
		return serialize( new PointMatches( p, pms ), path );
	}
	
	final static private ArrayList< PointMatch > deserializePointMatches( final Elastic_Align.Param p, final String path )
	{
		Object o = deserialize( path );
		if ( null == o ) return null;
		PointMatches pms = (PointMatches) o;
		if ( p.equalSiftPointMatchParams( pms.p ) )
			return pms.pointMatches;
		return null;
	}
	
	final static private ArrayList< PointMatch > deserializeBlockMatches( final Elastic_Align.Param p, final String path )
	{
		Object o = deserialize( path );
		if ( null == o ) return null;
		PointMatches pms = (PointMatches) o;
		if ( p.equalBlockmatchingParams( pms.p ))
			return pms.pointMatches;
		return null;
	}

	/** Serializes the given object into the path. Returns false on failure. */
	static public boolean serialize(final Object ob, final String path) {
		try {
			// 1 - Check that the parent chain of folders exists, and attempt to create it when not:
			File fdir = new File(path).getParentFile();
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
		} catch (Exception e) {
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
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
