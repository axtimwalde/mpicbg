import mpicbg.ij.Mapping;
import mpicbg.ij.InverseTransformMapping;
import mpicbg.ij.SIFT;
import mpicbg.ij.TransformMeshMapping;
import mpicbg.ij.blockmatching.BlockMatching;
import mpicbg.ij.util.Util;
import mpicbg.imagefeatures.*;
import mpicbg.models.*;

import ij.plugin.*;
import ij.gui.*;
import ij.*;
import ij.process.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.awt.Color;
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
		 * Implemeted transformation models for choice
		 */
		final static public String[] modelStrings = new String[]{ "Translation", "Rigid", "Similarity", "Affine" };
		public int modelIndex = 1;
		
		public int springMeshResolution = 16;
		public float stiffness = 0.1f;
		public float springMeshDamp = 0.6f;
		public float maxStretch = 2000.0f;
		public int maxIterations = 100000;
		public int maxPlateauwidth = 200;
		
		public boolean interpolate = true;
		
		public boolean animate = false;
		
		public boolean setup()
		{
			final GenericDialog gd = new GenericDialog( "Elastically align stack" );
			
			SIFT.addFields( gd, sift );
			
			gd.addNumericField( "closest/next_closest_ratio :", p.rod, 2 );
			
			gd.addMessage( "Geometric Consensus Filter:" );
			gd.addNumericField( "maximal_alignment_error :", p.maxEpsilon, 2, 6, "px" );
			gd.addNumericField( "inlier_ratio :", p.minInlierRatio, 2 );
			gd.addChoice( "approximate_transformation :", Param.modelStrings, Param.modelStrings[ p.modelIndex ] );
			
			gd.addMessage( "Spring Mesh:" );
			gd.addNumericField( "resolution :", springMeshResolution, 0 );
			gd.addNumericField( "stiffness :", stiffness, 2 );
			gd.addNumericField( "maximal_stretch :", maxStretch, 2, 6, "px" );
			
			gd.addMessage( "Output:" );
			gd.addCheckbox( "interpolate", p.interpolate );
			gd.addCheckbox( "animate", p.animate );
			
			gd.showDialog();
			
			if ( gd.wasCanceled() ) return false;
			
			SIFT.readFields( gd, sift );
			
			p.rod = ( float )gd.getNextNumber();
			
			p.maxEpsilon = ( float )gd.getNextNumber();
			p.minInlierRatio = ( float )gd.getNextNumber();
			p.modelIndex = gd.getNextChoiceIndex();
			
			p.springMeshResolution = ( int )gd.getNextNumber();
			p.stiffness = ( float )gd.getNextNumber();
			p.maxStretch = ( float )gd.getNextNumber();
			
			p.interpolate = gd.getNextBoolean();
			p.animate = gd.getNextBoolean();
			
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
		ImageStack stackAligned = new ImageStack( stack.getWidth(), stack.getHeight() );
		
		final List< InvertibleCoordinateTransform > transforms = new ArrayList< InvertibleCoordinateTransform >( stack.getSize() - 1 );
		
		stackAligned.addSlice( null, stack.getProcessor( 1 ).duplicate() );
		ImagePlus impAligned = new ImagePlus( "Aligned 1 of " + stack.getSize(), stackAligned );
		impAligned.show();
		
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
		
		Mapping mapping = new InverseTransformMapping< AbstractAffineModel2D< ? > >( model );
		
		final List< SpringMesh > meshes = new ArrayList< SpringMesh >( stack.getSize() );		
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
			
			stackAligned.addSlice( null, approximatelyAlignedSlice );
			
			impAligned.setStack( "Aligned " + stackAligned.getSize() + " of " + stack.getSize(), stackAligned );
			impAligned.updateAndDraw();
			
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
					512.0f / ip1.getWidth(),
					transforms.get( i - 1 ).createInverse(),
					blockRadius,
					blockRadius,
					searchRadius,
					searchRadius,
					0.9f,
					0.8f,
					5.0f,
					v1,
					pm12,
					new ErrorStatistic( 1 ) );
			
			IJ.log( "> found " + pm12.size() + " correspondences." );
			
			final List< Point > s1 = new ArrayList< Point >();
			PointMatch.sourcePoints( pm12, s1 );
			final ImagePlus imp1 = new ImagePlus( i + " >", ip1 );
			imp1.show();
			imp1.getCanvas().setDisplayList( BlockMatching.illustrateMatches( pm12 ), Color.yellow, null );
			imp1.setRoi( Util.pointsToPointRoi( s1 ) );
			imp1.updateAndDraw();
			
			BlockMatching.matchByMaximalPMCC(
					ip2,
					ip1,
					512.0f / ip1.getWidth(),
					transforms.get( i - 1 ),
					blockRadius,
					blockRadius,
					searchRadius,
					searchRadius,
					0.9f,
					0.8f,
					5.0f,
					v2,
					pm21,
					new ErrorStatistic( 1 ) );
			
			IJ.log( "< found " + pm21.size() + " correspondences." );
			
			final List< Point > s2 = new ArrayList< Point >();
			PointMatch.sourcePoints( pm21, s2 );
			final ImagePlus imp2 = new ImagePlus( i + " <", ip2 );
			imp2.show();IJ.log( "> found " + pm12.size() + " correspondences." );
			
			
			imp2.getCanvas().setDisplayList( BlockMatching.illustrateMatches( pm21 ), Color.yellow, null );
			imp2.setRoi( Util.pointsToPointRoi( s2 ) );
			imp2.updateAndDraw();
			
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
		
		try { SpringMesh.optimizeMeshes( meshes, p.maxEpsilon, p.maxIterations, p.maxPlateauwidth ); }
		catch ( NotEnoughDataPointsException e ) { e.printStackTrace(); }
		
		for ( int i = 1; i <= stack.getSize(); ++i )
		{
			final Mapping meshMapping = new TransformMeshMapping( meshes.get( i - 1 ) );
			if ( p.interpolate )
				meshMapping.mapInterpolated( stack.getProcessor( i ), stackAligned.getProcessor( i ) );
			else
				meshMapping.map( stack.getProcessor( i ), stackAligned.getProcessor( i ) );
			
		}
		
		impAligned.updateAndDraw();
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
