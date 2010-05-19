import mpicbg.ij.FeatureTransform;
import mpicbg.ij.SIFT;
import mpicbg.ij.visualization.PointVis;
import mpicbg.imagefeatures.*;
import mpicbg.models.*;

import ij.plugin.*;
import ij.process.Blitter;
import ij.process.ColorProcessor;
import ij.gui.*;
import ij.*;

import java.awt.Color;
import java.awt.Rectangle;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;

/**
 * Extract landmark correspondences in two images as PointRoi.
 * 
 * The plugin uses the Scale Invariant Feature Transform (SIFT) by David Lowe
 * \cite{Lowe04} and the Random Sample Consensus (RANSAC) by Fishler and Bolles
 * \citet{FischlerB81} with respect to a transformation model to identify
 * landmark correspondences.
 * 
 * BibTeX:
 * <pre>
 * &#64;article{Lowe04,
 *   author    = {David G. Lowe},
 *   title     = {Distinctive Image Features from Scale-Invariant Keypoints},
 *   journal   = {International Journal of Computer Vision},
 *   year      = {2004},
 *   volume    = {60},
 *   number    = {2},
 *   pages     = {91--110},
 * }
 * &#64;article{FischlerB81,
 *	 author    = {Martin A. Fischler and Robert C. Bolles},
 *   title     = {Random sample consensus: a paradigm for model fitting with applications to image analysis and automated cartography},
 *   journal   = {Communications of the ACM},
 *   volume    = {24},
 *   number    = {6},
 *   year      = {1981},
 *   pages     = {381--395},
 *   publisher = {ACM Press},
 *   address   = {New York, NY, USA},
 *   issn      = {0001-0782},
 *   doi       = {http://doi.acm.org/10.1145/358669.358692},
 * }
 * </pre>
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.4b
 */
public class SIFT_ExtractPointRoi implements PlugIn
{
	final static private DecimalFormat decimalFormat = new DecimalFormat();
	final static private DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
	
	private ImagePlus imp1;
	private ImagePlus imp2;
	
	final private List< Feature > fs1 = new ArrayList< Feature >();
	final private List< Feature > fs2 = new ArrayList< Feature >();;
	
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
		public float minInlierRatio = 0.05f;
		
		/**
		 * Implemeted transformation models for choice
		 */
		final static public String[] modelStrings = new String[]{ "Translation", "Rigid", "Similarity", "Affine" };
		public int modelIndex = 1;
	}
	
	final static private Param p = new Param();
	
	public SIFT_ExtractPointRoi()
	{
		decimalFormatSymbols.setGroupingSeparator( ',' );
		decimalFormatSymbols.setDecimalSeparator( '.' );
		decimalFormat.setDecimalFormatSymbols( decimalFormatSymbols );
		decimalFormat.setMaximumFractionDigits( 3 );
		decimalFormat.setMinimumFractionDigits( 3 );		
	}
	
	public void run( String args )
	{
		// cleanup
		fs1.clear();
		fs2.clear();
		
		if ( IJ.versionLessThan( "1.40" ) ) return;
		
		int[] ids = WindowManager.getIDList();
		if ( ids == null || ids.length < 2 )
		{
			IJ.showMessage( "You should have at least two images open." );
			return;
		}
		
		String[] titles = new String[ ids.length ];
		for ( int i = 0; i < ids.length; ++i )
		{
			titles[ i ] = ( WindowManager.getImage( ids[ i ] ) ).getTitle();
		}
		
		final GenericDialog gd = new GenericDialog( "Extract SIFT Landmark Correspondences" );
		
		gd.addMessage( "Image Selection:" );
		final String current = WindowManager.getCurrentImage().getTitle();
		gd.addChoice( "source_image", titles, current );
		gd.addChoice( "target_image", titles, current.equals( titles[ 0 ] ) ? titles[ 1 ] : titles[ 0 ] );
		
		gd.addMessage( "Scale Invariant Interest Point Detector:" );
		gd.addNumericField( "initial_gaussian_blur :", p.sift.initialSigma, 2, 6, "px" );
		gd.addNumericField( "steps_per_scale_octave :", p.sift.steps, 0 );
		gd.addNumericField( "minimum_image_size :", p.sift.minOctaveSize, 0, 6, "px" );
		gd.addNumericField( "maximum_image_size :", p.sift.maxOctaveSize, 0, 6, "px" );
		
		gd.addMessage( "Feature Descriptor:" );
		gd.addNumericField( "feature_descriptor_size :", p.sift.fdSize, 0 );
		gd.addNumericField( "feature_descriptor_orientation_bins :", p.sift.fdBins, 0 );
		gd.addNumericField( "closest/next_closest_ratio :", p.rod, 2 );
		
		gd.addMessage( "Geometric Consensus Filter:" );
		gd.addNumericField( "maximal_alignment_error :", p.maxEpsilon, 2, 6, "px" );
		gd.addNumericField( "inlier_ratio :", p.minInlierRatio, 2 );
		gd.addChoice( "expected_transformation :", Param.modelStrings, Param.modelStrings[ p.modelIndex ] );
		
		gd.showDialog();
		
		if (gd.wasCanceled()) return;
		
		imp1 = WindowManager.getImage( ids[ gd.getNextChoiceIndex() ] );
		imp2 = WindowManager.getImage( ids[ gd.getNextChoiceIndex() ] );
		
		p.sift.initialSigma = ( float )gd.getNextNumber();
		p.sift.steps = ( int )gd.getNextNumber();
		p.sift.minOctaveSize = ( int )gd.getNextNumber();
		p.sift.maxOctaveSize = ( int )gd.getNextNumber();
		
		p.sift.fdSize = ( int )gd.getNextNumber();
		p.sift.fdBins = ( int )gd.getNextNumber();
		p.rod = ( float )gd.getNextNumber();
		
		p.maxEpsilon = ( float )gd.getNextNumber();
		p.minInlierRatio = ( float )gd.getNextNumber();
		p.modelIndex = gd.getNextChoiceIndex();

		exec(imp1, imp2);
	}

	/** If unsure, just use default parameters by using exec(ImagePlus, ImagePlus, int) method, where only the model is specified. */
	public void exec(final ImagePlus imp1, final ImagePlus imp2,
			 final float initialSigma, final int steps,
			 final int minOctaveSize, final int maxOctaveSize,
			 final int fdSize, final int fdBins,
			 final float rod, final float maxEpsilon,
			 final float minInlierRatio, final int modelIndex) {

		p.sift.initialSigma = initialSigma;
		p.sift.steps = steps;
		p.sift.minOctaveSize = minOctaveSize;
		p.sift.maxOctaveSize = maxOctaveSize;

		p.sift.fdSize = fdSize;
		p.sift.fdBins = fdBins;
		p.rod = rod;

		p.maxEpsilon = maxEpsilon;
		p.minInlierRatio = minInlierRatio;
		p.modelIndex = modelIndex;

		exec( imp1, imp2 );
	}

	/** Execute with default parameters, except the model.
	 *  @param modelIndex: 0=Translation, 1=Rigid, 2=Similarity, 3=Affine */
	public void exec(final ImagePlus imp1, final ImagePlus imp2, final int modelIndex) {
		if ( modelIndex < 0 || modelIndex > 3 ) {
			IJ.log("Invalid model index: " + modelIndex);
			return;
		}
		p.modelIndex = modelIndex;
		exec( imp1, imp2 );
	}

	/** Execute with default parameters (model is Rigid) */
	public void exec(final ImagePlus imp1, final ImagePlus imp2)
	{
		/* <visualization> */
		final ColorProcessor ipBgSrc = ( ColorProcessor )imp1.getProcessor().convertToRGB();
		ipBgSrc.setMinAndMax( -127, 383 );
		
		final ColorProcessor ipBgDst = ( ColorProcessor )imp2.getProcessor().convertToRGB();
		ipBgDst.setMinAndMax( -127, 383 );
		/* </visualization> */
		
		
		FloatArray2DSIFT sift = new FloatArray2DSIFT( p.sift );
		SIFT ijSIFT = new SIFT( sift );
		
		long start_time = System.currentTimeMillis();
		IJ.log( "Processing SIFT ..." );
		ijSIFT.extractFeatures( imp1.getProcessor(), fs1 );
		IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms." );
		IJ.log( fs1.size() + " features extracted." );
		
		start_time = System.currentTimeMillis();
		IJ.log( "Processing SIFT ..." );
		ijSIFT.extractFeatures( imp2.getProcessor(), fs2 );
		IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms." );
		IJ.log( fs2.size() + " features extracted." );
		
		/* <visualization> */
		final ColorProcessor ipFeatures = new ColorProcessor( imp1.getWidth() + imp2.getWidth() + 2, Math.max( imp1.getHeight(), imp2.getHeight() ) );
		ipFeatures.copyBits( ipBgSrc, 0, 0, Blitter.COPY );
		ipFeatures.copyBits( ipBgDst, ipBgSrc.getWidth() + 2, 0, Blitter.COPY );
		
		PointVis.drawFeaturePoints( ipFeatures, fs1, Color.BLACK, 3 );
		PointVis.drawFeaturePoints(
				ipFeatures,
				fs2,
				Color.BLACK,
				3,
				new Rectangle( imp1.getWidth() + 2, 0, imp2.getWidth(), imp2.getHeight() ),
				1 );
		
		final ImagePlus impFeatures = new ImagePlus( imp1.getTitle() + " " + imp2.getTitle() + " Feature Detections", ipFeatures );
		impFeatures.show(); 
		/* </visualization> */
		
		
		start_time = System.currentTimeMillis();
		IJ.log( "Identifying correspondence candidates using brute force ..." );
		final List< PointMatch > candidates = new ArrayList< PointMatch >();
		FeatureTransform.matchFeatures( fs1, fs2, candidates, p.rod );
		IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms." );	
		IJ.log( candidates.size() + " potentially corresponding features identified." );
		
		/* <visualization> */
		final ColorProcessor ipCandidates = ( ColorProcessor )ipFeatures.duplicate();
//		ipCandidates.copyBits( ipBgSrc, 0, 0, Blitter.COPY );
//		ipCandidates.copyBits( ipBgDst, ipBgSrc.getWidth() + 2, 0, Blitter.COPY );
		
		final ArrayList< Point > candidatesPointsSrc = new ArrayList< Point >();
		PointMatch.sourcePoints( candidates, candidatesPointsSrc );
		PointVis.drawLocalPoints( ipCandidates, candidatesPointsSrc, Color.GREEN, 3 );
		
		final ArrayList< Point > candidatesPointsDst = new ArrayList< Point >();
		PointMatch.targetPoints( candidates, candidatesPointsDst );
		PointVis.drawLocalPoints(
				ipCandidates,
				candidatesPointsDst,
				Color.GREEN,
				3,
				new Rectangle( imp1.getWidth() + 2, 0, imp2.getWidth(), imp2.getHeight() ),
				1 );
		
		PointVis.drawLocalPointMatchLines(
				ipCandidates,
				candidates,
				Color.GREEN,
				1,
				new Rectangle( 0, 0, imp1.getWidth(), imp1.getHeight() ),
				new Rectangle( imp1.getWidth() + 2, 0, imp2.getWidth(), imp2.getHeight() ),
				1.0,
				1.0 );
		
		final ImagePlus impCandidates = new ImagePlus( imp1.getTitle() + " " + imp2.getTitle() + " Correspondence Candidates", ipCandidates );
		impCandidates.show(); 
		
		
		/* </visualization> */
		
			
		start_time = System.currentTimeMillis();
		IJ.log( "Filtering correspondence candidates by geometric consensus ..." );
		List< PointMatch > inliers = new ArrayList< PointMatch >();
		
		Model< ? > model;
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
		model.setImpSrc( imp1 );
		model.setImpDst( imp2 );
		
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
		catch ( NotEnoughDataPointsException e )
		{
			modelFound = false;
		}
			
		IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms." );	
		
		if ( modelFound )
		{
			int x1[] = new int[ inliers.size() ];
			int y1[] = new int[ inliers.size() ];
			int x2[] = new int[ inliers.size() ];
			int y2[] = new int[ inliers.size() ];
			
			int i = 0;
			
			for ( PointMatch m : inliers )
			{
				float[] m_p1 = m.getP1().getL(); 
				float[] m_p2 = m.getP2().getL();
				
				x1[ i ] = Math.round( m_p1[ 0 ] );
				y1[ i ] = Math.round( m_p1[ 1 ] );
				x2[ i ] = Math.round( m_p2[ 0 ] );
				y2[ i ] = Math.round( m_p2[ 1 ] );
				
				++i;
			}
		
			PointRoi pr1 = new PointRoi( x1, y1, inliers.size() );
			PointRoi pr2 = new PointRoi( x2, y2, inliers.size() );
			
			imp1.setRoi( pr1 );
			imp2.setRoi( pr2 );
			
			IJ.log( inliers.size() + " corresponding features with an average displacement of " + decimalFormat.format( model.getCost() ) + "px identified." );
			IJ.log( "Estimated transformation model: " + model );
		}
		else
		{
			IJ.log( "No correspondences found." );
		}

	}
}
