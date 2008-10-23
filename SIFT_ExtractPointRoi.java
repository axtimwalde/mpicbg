import mpicbg.imagefeatures.*;
import mpicbg.models.*;

import ij.plugin.*;
import ij.gui.*;
import ij.*;
import ij.process.*;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.awt.TextField;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

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
 * NOTE:
 * The SIFT-method is protected by U.S. Patent 6,711,293: "Method and
 * apparatus for identifying scale invariant features in an image and use of
 * same for locating an object in an image" by the University of British
 * Columbia.  That is, for commercial applications the permission of the author
 * is required.
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.3b
 */
public class SIFT_ExtractPointRoi implements PlugIn, KeyListener
{
	final static private DecimalFormat decimalFormat = new DecimalFormat();
	final static private DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
	
	private ImagePlus imp1;
	private ImagePlus imp2;
	
	final private List< Feature > fs1 = new ArrayList< Feature >();
	final private List< Feature > fs2 = new ArrayList< Feature >();;
	
	static private class Param
	{	
		/**
		 * Steps per Scale Octave 
		 */
		public int steps = 3;
		
		/**
		 * Initial sigma of each Scale Octave
		 */
		public float initialSigma = 1.6f;
		
		/**
		 * Feature descriptor size
		 *    How many samples per row and column
		 */
		public int fdSize = 4;
		
		/**
		 * Feature descriptor orientation bins
		 *    How many bins per local histogram
		 */
		public int fdBins = 8;
		
		/**
		 * Closest/next closest neighbour distance ratio
		 */
		public float rod = 0.92f;
		
		/**
		 * Size limits for scale octaves in px:
		 * 
		 * minOctaveSize < octave < maxOctaveSize
		 */
		public int minOctaveSize = 64;
		public int maxOctaveSize = 1024;
		
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
		
		/**
		 * Set true to double the size of the image by linear interpolation to
		 * ( with * 2 + 1 ) * ( height * 2 + 1 ).  Thus we can start identifying
		 * DoG extrema with $\sigma = INITIAL_SIGMA / 2$ like proposed by
		 * \citet{Lowe04}.
		 * 
		 * This is useful for images scmaller than 1000px per side only. 
		 */ 
		public boolean upscale = false;
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
	
	final protected void extractFeatures(
			final ImageProcessor ip,
			final List< Feature > fs,
			final FloatArray2DSIFT sift,
			final Param p )
	{
		FloatArray2D fa = new FloatArray2D( ip.getWidth(), ip.getHeight() );
		ImageArrayConverter.imageProcessorToFloatArray2D( ip, fa );
		Filter.enhance( fa, 1.0f );
		
		final float[] initialKernel;
		
		if ( p.upscale )
		{
			final FloatArray2D fat = new FloatArray2D( fa.width * 2 - 1, fa.height * 2 - 1 ); 
			FloatArray2DScaleOctave.upsample( fa, fat );
			fa = fat;
			initialKernel = Filter.createGaussianKernel( ( float )Math.sqrt( p.initialSigma * p.initialSigma - 1.0 ), true );
		}
		else
			initialKernel = Filter.createGaussianKernel( ( float )Math.sqrt( p.initialSigma * p.initialSigma - 0.25 ), true );
			
		fa = Filter.convolveSeparable( fa, initialKernel, initialKernel );
		
		
		long start_time = System.currentTimeMillis();
		IJ.log( "Processing SIFT ..." );
		sift.init( fa, p.steps, p.initialSigma, p.minOctaveSize, p.maxOctaveSize );
		fs.addAll( sift.run( p.maxOctaveSize ) );
		Collections.sort( fs );
		IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms." );
		IJ.log( fs.size() + " features extracted." );
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
		gd.addNumericField( "initial_gaussian_blur :", p.initialSigma, 2, 6, "px" );
		gd.addNumericField( "steps_per_scale_octave :", p.steps, 0 );
		gd.addNumericField( "minimum_image_size :", p.minOctaveSize, 0, 6, "px" );
		gd.addNumericField( "maximum_image_size :", p.maxOctaveSize, 0, 6, "px" );
		gd.addCheckbox( "upscale_image_first", p.upscale );
		
		gd.addMessage( "Feature Descriptor:" );
		gd.addNumericField( "feature_descriptor_size :", p.fdSize, 0 );
		gd.addNumericField( "feature_descriptor_orientation_bins :", p.fdBins, 0 );
		gd.addNumericField( "closest/next_closest_ratio :", p.rod, 2 );
		
		gd.addMessage( "Geometric Consensus Filter:" );
		gd.addNumericField( "maximal_alignment_error :", p.maxEpsilon, 2, 6, "px" );
		gd.addNumericField( "inlier_ratio :", p.minInlierRatio, 2 );
		gd.addChoice( "expected_transformation :", Param.modelStrings, Param.modelStrings[ p.modelIndex ] );
		
		gd.showDialog();
		
		if (gd.wasCanceled()) return;
		
		imp1 = WindowManager.getImage( ids[ gd.getNextChoiceIndex() ] );
		imp2 = WindowManager.getImage( ids[ gd.getNextChoiceIndex() ] );
		
		p.initialSigma = ( float )gd.getNextNumber();
		p.steps = ( int )gd.getNextNumber();
		p.minOctaveSize = ( int )gd.getNextNumber();
		p.maxOctaveSize = ( int )gd.getNextNumber();
		p.upscale = gd.getNextBoolean();
		
		float scale = 1.0f;
		if ( p.upscale ) scale = 2.0f;
		
		p.fdSize = ( int )gd.getNextNumber();
		p.fdBins = ( int )gd.getNextNumber();
		p.rod = ( float )gd.getNextNumber();
		
		p.maxEpsilon = ( float )gd.getNextNumber();
		p.minInlierRatio = ( float )gd.getNextNumber();
		p.modelIndex = gd.getNextChoiceIndex();
		
		FloatArray2DSIFT sift = new FloatArray2DSIFT( p.fdSize, p.fdBins );
		extractFeatures( imp1.getProcessor(), fs1, sift, p );
		extractFeatures( imp2.getProcessor(), fs2, sift, p );
		
		
		long start_time = System.currentTimeMillis();
		IJ.log( "Identifying correspondence candidates using brute force ..." );
		Vector< PointMatch > candidates = 
				FloatArray2DSIFT.createMatches( fs1, fs2, p.rod );
		IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms." );	
		IJ.log( candidates.size() + " potentially corresponding features identified." );
			
		start_time = System.currentTimeMillis();
		IJ.log( "Filtering correspondence candidates by geometric consensus ..." );
		Vector< PointMatch > inliers = new Vector< PointMatch >();
		
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
				
				x1[ i ] = ( int )( m_p1[ 0 ] / scale );
				y1[ i ] = ( int )( m_p1[ 1 ] / scale );
				x2[ i ] = ( int )( m_p2[ 0 ] / scale );
				y2[ i ] = ( int )( m_p2[ 1 ] / scale );
				
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
