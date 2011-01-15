import mpicbg.ij.InverseTransformMapping;
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
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.awt.TextField;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1a
 */
public class Elastic_Align implements PlugIn, KeyListener
{
	static private class Param
	{	
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
		
	}
	
	final static Param p = new Param(); 

	final public void run( final String args )
	{
		if ( IJ.versionLessThan( "1.41n" ) ) return;

		final ImagePlus imp = WindowManager.getCurrentImage();
		if ( imp == null )  { System.err.println( "There are no images open" ); return; }
		
		if ( !p.setup() ) return;
		
		ImageStack stack = imp.getStack();
		//ImageStack stackAligned = new ImageStack( stack.getWidth(), stack.getHeight() );
		
		final ArrayList< InvertibleCoordinateTransform > transforms = new ArrayList< InvertibleCoordinateTransform >( stack.getSize() - 1 );
		
		//stackAligned.addSlice( null, stack.getProcessor( 1 ).duplicate() );
		//ImagePlus impAligned = new ImagePlus( "Aligned 1 of " + stack.getSize(), stackAligned );
		//impAligned.show();
		
		final List< Feature > fs1 = new ArrayList< Feature >(); 
		final List< Feature > fs2 = new ArrayList< Feature >();
		
		FloatArray2DSIFT sift = new FloatArray2DSIFT( p.sift );
		SIFT ijSIFT = new SIFT( sift );
		
		long start_time = System.currentTimeMillis();
		IJ.log( "Processing SIFT ..." );
		ijSIFT.extractFeatures( stack.getProcessor( 1 ), fs2 );
		IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms." );
		IJ.log( fs2.size() + " features extracted." );

		AbstractAffineModel2D model;
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
		
		InverseTransformMapping< AbstractAffineModel2D< ? > > mapping = new InverseTransformMapping< AbstractAffineModel2D< ? > >( model );
		
		final ArrayList< SpringMesh > meshes = new ArrayList< SpringMesh >( stack.getSize() );		
		meshes.add( new SpringMesh( p.springMeshResolution, stack.getWidth(), stack.getHeight(), p.stiffness, p.maxStretch, p.springMeshDamp ) ); 
		
		/* Linear alignment */
		for ( int i = 2; i <= stack.getSize(); ++i )
		{
			final ImageProcessor ip = stack.getProcessor( i );
			
			fs1.clear();
			fs1.addAll( fs2 );
			fs2.clear();
			
			start_time = System.currentTimeMillis();
			IJ.log( "Processing SIFT ..." );
			ijSIFT.extractFeatures( ip, fs2 );
			IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms." );
			IJ.log( fs2.size() + " features extracted." );
			
			start_time = System.currentTimeMillis();
			System.out.print( "identifying correspondences using brute force ..." );
			List< PointMatch > candidates = 
				FloatArray2DSIFT.createMatches( fs2, fs1, p.rod );
			System.out.println( " took " + ( System.currentTimeMillis() - start_time ) + "ms" );
			
			IJ.log( candidates.size() + " potentially corresponding features identified" );

			final List< PointMatch > inliers = new Vector< PointMatch >();
			
			AbstractAffineModel2D< ? > currentModel;
			switch ( p.modelIndex )
			{
			case 0:
				currentModel = new TranslationModel2D();
				break;
			case 1:
				currentModel = new RigidModel2D();
				break;
			case 2:
				currentModel = new SimilarityModel2D();
				break;
			case 3:
				currentModel = new AffineModel2D();
				break;
			default:
				return;
			}
			
			boolean modelFound;
			try
			{
				modelFound = currentModel.filterRansac(
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
				transforms.add( currentModel );
				model.concatenate( currentModel );
			}
			else
				transforms.add( null );
			
			final ImageProcessor approximatelyAlignedSlice =
					ip.createProcessor( ip.getWidth(), ip.getHeight() );
			if ( p.interpolate )
				mapping.mapInterpolated( ip, approximatelyAlignedSlice );
			else
				mapping.map( ip, approximatelyAlignedSlice );
			
			IJ.save( new ImagePlus( "linear " + i, approximatelyAlignedSlice ), "linear-" + String.format( "%04d", i ) + ".tif" );
			
//			stackAligned.addSlice( null, approximatelyAlignedSlice );
//			
//			impAligned.setStack( "Aligned " + stackAligned.getSize() + " of " + stack.getSize(), stackAligned );
//			impAligned.updateAndDraw();
			
			final SpringMesh m2 = new SpringMesh( p.springMeshResolution, stack.getWidth(), stack.getHeight(), p.stiffness, p.maxStretch, p.springMeshDamp );
			//m2.init( model );
			meshes.add( m2 );
		}
		
		/* Elastic alignment */
		final int blockRadius = Math.max( 32, stack.getWidth() / p.springMeshResolution / 2 );
		
		/** TODO set this something more than the largest error by the approximate model */
		final int searchRadius = Math.round( p.maxEpsilon );
		
		for ( int i = 1; i < stack.getSize(); ++i )
		{
			final SpringMesh m1 = meshes.get( i - 1 );
			final SpringMesh m2 = meshes.get( i );
			final Collection< Vertex > v1 = m1.getVertices();
			final Collection< Vertex > v2 = m2.getVertices();
			
			final FloatProcessor ip1 = ( FloatProcessor )stack.getProcessor( i ).convertToFloat().duplicate();
			final FloatProcessor ip2 = ( FloatProcessor )stack.getProcessor( i + 1 ).convertToFloat().duplicate();
			
			final List< PointMatch > pm12 = new ArrayList< PointMatch >();
			final List< PointMatch > pm21 = new ArrayList< PointMatch >();
			
			BlockMatching.matchByMaximalPMCC(
					ip1,
					ip2,
					Math.min( 1.0f, ( float )p.sift.maxOctaveSize / ip1.getWidth() ),
					transforms.get( i - 1 ).createInverse(),
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
					transforms.get( i - 1 ),
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
			
			/* <visualisation> */
//			final List< Point > s2 = new ArrayList< Point >();
//			PointMatch.sourcePoints( pm21, s2 );
//			final ImagePlus imp2 = new ImagePlus( i + " <", ip2 );
//			imp2.show();
//			imp2.setOverlay( BlockMatching.illustrateMatches( pm21 ), Color.yellow, null );
//			imp2.setRoi( Util.pointsToPointRoi( s2 ) );
//			imp2.updateAndDraw();
			/* </visualisation> */
			
			for ( final PointMatch pm : pm12 )
			{
				final Vertex p1 = ( Vertex )pm.getP1();
				final Vertex p2 = new Vertex( pm.getP2() );
				p1.addSpring( p2, new Spring( 0, 1 ) );
				m2.addPassiveVertex( p2 );
			}
			
			for ( final PointMatch pm : pm21 )
			{
				final Vertex p1 = ( Vertex )pm.getP1();
				final Vertex p2 = new Vertex( pm.getP2() );
				p1.addSpring( p2, new Spring( 0, 1 ) );
				m1.addPassiveVertex( p2 );
			}
		}
		
		/* initialize meshes */
		/* TODO this is accumulative and thus not perfect, change to analytical concatenation later */
		for ( int i = 1; i < stack.getSize(); ++i )
		{
			final CoordinateTransformList< CoordinateTransform > ctl = new CoordinateTransformList< CoordinateTransform >();
			for ( int j = i - 1; j >= 0; --j )
				ctl.add( transforms.get( j ) );
			meshes.get( i ).init( ctl );
		}
		
		/* optimize */
		try { SpringMesh.optimizeMeshes( meshes, p.maxEpsilon, p.maxIterations, p.maxPlateauwidth ); }
		catch ( NotEnoughDataPointsException e ) { e.printStackTrace(); }
		
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
			final TransformMeshMapping< SpringMesh > meshMapping = new TransformMeshMapping< SpringMesh >( meshes.get( i - 1 ) );
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
}
