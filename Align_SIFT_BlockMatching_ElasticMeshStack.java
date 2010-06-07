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
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.gui.*;

import mpicbg.ij.Mapping;
import mpicbg.ij.SIFT;
import mpicbg.ij.TransformMeshMapping;
import mpicbg.ij.blockmatching.BlockMatching;
import mpicbg.imagefeatures.Feature;
import mpicbg.imagefeatures.FloatArray2DSIFT;
import mpicbg.models.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Align_SIFT_BlockMatching_ElasticMeshStack implements PlugIn
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
		public Class< ? extends AbstractAffineModel2D< ? > > expectedModelClass;
		public int desiredModelIndex = 1;
		public Class< ? extends AbstractAffineModel2D< ? > > desiredModelClass;
		public int localModelIndex = 1;
		public Class< ? extends AbstractAffineModel2D< ? > > localModelClass;
		
		
		public float alpha = 2.0f;
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
			
			final GenericDialog gd = new GenericDialog( "Align stack elastically using SIFT and Blockmatchig" );
			
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
			switch ( p.expectedModelIndex )
			{
			case 0:
				expectedModelClass = TranslationModel2D.class;
				break;
			case 1:
				expectedModelClass = RigidModel2D.class;
				break;
			case 2:
				expectedModelClass = SimilarityModel2D.class;
				break;
			case 3:
				expectedModelClass = AffineModel2D.class;
				break;
			default:
				IJ.error( "Invalid model selected." );
				return false;
			}
			
			desiredModelIndex = gd.getNextChoiceIndex();
			switch ( p.desiredModelIndex )
			{
			case 0:
				desiredModelClass = TranslationModel2D.class;
				break;
			case 1:
				desiredModelClass = RigidModel2D.class;
				break;
			case 2:
				desiredModelClass = SimilarityModel2D.class;
				break;
			case 3:
				desiredModelClass = AffineModel2D.class;
				break;
			default:
				IJ.error( "Invalid model selected." );
				return false;
			}
			
			alpha = ( float )gd.getNextNumber();
			meshResolution = ( int )gd.getNextNumber();
			localModelIndex = gd.getNextChoiceIndex();
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
				return false;
			}
			
			return true;
		}
	}
	final static private Param p = new Param();
	
	protected TransformMeshMapping mapping;
	
	final private static void findMatches(
			final Param p,
			final ImageProcessor ipSource,
			final ImageProcessor ipTarget,
			final Model< ? > initialModel,
			final Collection< PointMatch > sourceMatches )
	{
		CoordinateTransform ict = initialModel;
		final Collection< Point > sourcePoints = new ArrayList< Point >();
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
			
			final int blockRadius = Math.max( 16, ipSource.getWidth() / n / 2 );
			final int searchRadius = ( int )( sourceMatches.size() >= mlst.getModel().getMinNumMatches() ? Math.min( p.maxEpsilon + 0.5f, blockRadius ) : p.maxEpsilon );
			
			/* block match forward */
			sourcePoints.clear();
			sourceMatches.clear();
			
			final TransformMesh mesh = new TransformMesh( n, ipSource.getWidth(), ipSource.getHeight() );
			PointMatch.sourcePoints( mesh.getVA().keySet(), sourcePoints );
			BlockMatching.matchByMaximalPMCC(
					( FloatProcessor )( ipSource instanceof FloatProcessor ? ipSource.duplicate() : ipSource.convertToFloat() ),
					( FloatProcessor )( ipTarget instanceof FloatProcessor ? ipTarget.duplicate() : ipTarget.convertToFloat() ),
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
					sourceMatches,
					new ErrorStatistic( 1 ) );
			
			if  ( sourceMatches.size() >= mlst.getModel().getMinNumMatches() )
			{	
				mlst.setAlpha( p.alpha );
				try
				{
					mlst.setMatches( sourceMatches );
					ict = mlst;
				}
				catch ( Exception e ) {}
			}
		}
	}
	
	public void run( String arg )
    {
		if ( IJ.versionLessThan( "1.41n" ) ) return;
		
		final ElasticMeshStack meshes = new ElasticMeshStack();
		final CoordinateTransformList models = new CoordinateTransformList();
		
		List< Feature > features1;
		List< Feature > features2;
		ElasticMovingLeastSquaresMesh< ? > m1;
		ElasticMovingLeastSquaresMesh< ? > m2;
		
		final ImagePlus imp = WindowManager.getCurrentImage();
		if ( imp == null )  { System.err.println( "You should have a stack open" ); return; }
		
		if ( !p.setup() ) return;		
		
		final ImageStack stack = imp.getStack();
		final ImageStack stackAligned = new ImageStack( stack.getWidth(), stack.getHeight() );
		for ( int i = 1; i <= stack.getSize(); ++i )
			stackAligned.addSlice( "", stack.getProcessor( i ).duplicate() );
		
		final ImagePlus impAligned = new ImagePlus( imp.getTitle() + " aligned", stackAligned );
		impAligned.show();
			
		final FloatArray2DSIFT sift = new FloatArray2DSIFT( p.sift );
		SIFT ijSIFT = new SIFT( sift );
		
		features2 = new ArrayList< Feature >();
		long start_time = System.currentTimeMillis();
		IJ.log( "processing SIFT ..." );
		ijSIFT.extractFeatures( stack.getProcessor( 1 ), features2 );
		IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms" );	
		IJ.log( features2.size() + " features identified and processed" );
		
		m2 = new ElasticMovingLeastSquaresMesh(
				p.localModelClass,
				p.meshResolution,
				imp.getWidth(),
				imp.getHeight(),
				p.alpha );
		meshes.addMesh( m2 );
			
		for ( int i = 1; i < stack.getSize(); ++i )
		{
			features1 = features2;
			m1 = m2;
				
			features2 = new ArrayList< Feature >();
			start_time = System.currentTimeMillis();
			IJ.log( "processing SIFT ..." );
			ijSIFT.extractFeatures( stack.getProcessor( i + 1 ), features2 );
			IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms" );
				
			IJ.log( features2.size() + " features identified and processed");
				
			m2 = new ElasticMovingLeastSquaresMesh(
					p.localModelClass,
					p.meshResolution,
					imp.getWidth(),
					imp.getHeight(),
					p.alpha );
			
			start_time = System.currentTimeMillis();
			IJ.log( "identifying correspondences using brute force ..." );
			List< PointMatch > candidates = 
					FloatArray2DSIFT.createMatches( features2, features1, p.rod );
			IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms" );
				
			IJ.log( candidates.size() + " potentially corresponding features identified" );
				
			final List< PointMatch > inliers = new ArrayList< PointMatch >();
			
			final AbstractAffineModel2D< ? > expectedModel;
			try
			{
				expectedModel = p.expectedModelClass.newInstance();
			}
			catch ( Exception e )
			{
				e.printStackTrace();
				return;
			}
			
			boolean modelFound;
			try
			{
				modelFound = expectedModel.filterRansac(
						candidates,
						inliers,
						1000,
						p.maxEpsilon,
						p.minInlierRatio );
			}
			catch ( Exception e )
			{
				modelFound = false;
			}
			
			if ( modelFound )
			{	
				IJ.log( inliers.size() + " corresponding features with an average displacement of " + ElasticMeshStack.decimalFormat.format( PointMatch.meanDistance( inliers ) ) + "px identified." );
				
				AbstractAffineModel2D< ? > desiredModel;
				try
				{
					desiredModel = p.desiredModelClass.newInstance();
					desiredModel.fit( inliers );
				}
				catch ( Exception e )
				{
					try
					{
						desiredModel = p.expectedModelClass.newInstance();
					}
					catch ( Exception f )
					{
						f.printStackTrace();
						return;
					}
				}
				
				final Collection< PointMatch > sourceMatches = new ArrayList< PointMatch >();
				final Collection< PointMatch > targetMatches = new ArrayList< PointMatch >();
				
				findMatches( p, stack.getProcessor( i ), stack.getProcessor( i + 1 ), desiredModel.createInverse(), sourceMatches );
				findMatches( p, stack.getProcessor( i + 1 ), stack.getProcessor( i ), desiredModel, targetMatches );
				
				for ( final PointMatch pm : sourceMatches )
				{
					final float[] sourceLocation = pm.getP1().getL();
					final float[] targetLocation = pm.getP2().getL();
					final Tile< ? > t = m1.findClosest( sourceLocation );
					final Tile< ? > o = m2.findClosest( targetLocation );
					
					t.addMatch( new PointMatch( pm.getP1(), pm.getP2(), pm.getWeights() ) );
					o.addMatch( new PointMatch( pm.getP2(), pm.getP1(), pm.getWeights() ) );
					//m1.addMatchWeightedByDistance( new PointMatch( pm.getP2(), pm.getP1(), 0.01f ), alpha );
					//m2.addMatchWeightedByDistance( new PointMatch( pm.getP1(), pm.getP2(), 0.01f ), alpha );
					
					t.addConnectedTile( o );
					o.addConnectedTile( t );
				}
				
				for ( final PointMatch pm : targetMatches )
				{
					final float[] targetLocation = pm.getP1().getL();
					final float[] sourceLocation = pm.getP2().getL();
					final Tile< ? > t = m1.findClosest( sourceLocation );
					final Tile< ? > o = m2.findClosest( targetLocation );
					
					t.addMatch( new PointMatch( pm.getP2(), pm.getP1(), pm.getWeights() ) );
					o.addMatch( new PointMatch( pm.getP1(), pm.getP2(), pm.getWeights() ) );
					//m1.addMatchWeightedByDistance( new PointMatch( pm.getP2(), pm.getP1(), 0.01f ), alpha );
					//m2.addMatchWeightedByDistance( new PointMatch( pm.getP1(), pm.getP2(), 0.01f ), alpha );
					
					t.addConnectedTile( o );
					o.addConnectedTile( t );
				}
				
				models.add( desiredModel );
			}
			else
			{
				try
				{
					models.add( p.desiredModelClass.newInstance() );
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
		optimize( meshes, impAligned, stack, stackAligned );
		apply( meshes, impAligned, stack, stackAligned );
    }
	
	
	final static private void optimize(
			final ElasticMeshStack meshes,
			final ImagePlus impAligned,
			final ImageStack stack,
			final ImageStack stackAligned )
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
	
	final static private void apply(
			final ElasticMeshStack meshes,
			final ImagePlus impAligned,
			final ImageStack stack,
			final ImageStack stackAligned )
	{
		final ArrayList< ElasticMovingLeastSquaresMesh< ? > > meshStack = meshes.meshes;
		for ( int i = 0; i < stack.getSize(); ++ i )
		{
			ElasticMovingLeastSquaresMesh mesh = meshStack.get( i );
			final Mapping< ? > mapping = new TransformMeshMapping( mesh );
			mapping.mapInterpolated( stack.getProcessor( i + 1 ), stackAligned.getProcessor( i + 1 ) );
		}
		impAligned.updateAndDraw();
	}
}
