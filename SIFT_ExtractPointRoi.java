import mpicbg.imagefeatures.*;
import mpicbg.models.*;

import ij.plugin.*;
import ij.gui.*;
import ij.*;
import ij.process.*;

import java.util.Collections;
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
 * @version 0.1b
 */
public class SIFT_ExtractPointRoi implements PlugIn, KeyListener
{
	// steps
	private static int steps = 3;
	// initial sigma
	private static float initial_sigma = 1.6f;
	// feature descriptor size
	private static int fdsize = 4;
	// feature descriptor orientation bins
	private static int fdbins = 8;
	// closest/next closest neighbour distance ratio
	private static float rod = 0.92f;
	// size restrictions for scale octaves, use octaves < max_size and > min_size only
	private static int min_size = 64;
	private static int max_size = 1024;
	// maximal allowed alignment error in px
	private static float max_epsilon = 100.0f;
	private static float min_inlier_ratio = 0.05f;
	
	/**
	 * Set true to double the size of the image by linear interpolation to
	 * ( with * 2 + 1 ) * ( height * 2 + 1 ).  Thus we can start identifying
	 * DoG extrema with $\sigma = INITIAL_SIGMA / 2$ like proposed by
	 * \citet{Lowe04}.
	 * 
	 * This is useful for images scmaller than 1000px per side only. 
	 */ 
	private static boolean upscale = false;
	private static float scale = 1.0f;
	private static int method = 1;
	
	public void run( String args )
	{
		if ( IJ.versionLessThan( "1.37i" ) ) return;
		
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
		
		String[] methods = new String[]{ "Translation", "Rigid" };
		
		GenericDialog gd = new GenericDialog( "Extract Landmark Correspondences" );
		String current = WindowManager.getCurrentImage().getTitle();
		gd.addChoice( "source_image", titles, current );
		gd.addChoice( "target_image", titles, current.equals( titles[ 0 ] ) ? titles[ 1 ] : titles[ 0 ] );
		
		gd.addMessage( "SIFT Parameters:" );
		gd.addNumericField( "steps_per_scale_octave :", steps, 0 );
		gd.addNumericField( "initial_gaussian_blur :", initial_sigma, 2 );
		gd.addNumericField( "feature_descriptor_size :", fdsize, 0 );
		gd.addNumericField( "feature_descriptor_orientation_bins :", fdbins, 0 );
		gd.addNumericField( "minimum_image_size :", min_size, 0 );
		gd.addNumericField( "maximum_image_size :", max_size, 0 );
		gd.addNumericField( "closest/next_closest_ratio :", rod, 2 );
		gd.addNumericField( "maximal_alignment_error :", max_epsilon, 2 );
		gd.addNumericField( "inlier_ratio :", min_inlier_ratio, 2 );
		gd.addCheckbox( "upscale_image_first", upscale );
		gd.addChoice( "transformation_class", methods, methods[ 1 ] );
		gd.showDialog();
		
		if (gd.wasCanceled()) return;
		
		ImagePlus imp1 = WindowManager.getImage( ids[ gd.getNextChoiceIndex() ] );
		ImagePlus imp2 = WindowManager.getImage( ids[ gd.getNextChoiceIndex() ] );
		
		steps = ( int )gd.getNextNumber();
		initial_sigma = ( float )gd.getNextNumber();
		fdsize = ( int )gd.getNextNumber();
		fdbins = ( int )gd.getNextNumber();
		min_size = ( int )gd.getNextNumber();
		max_size = ( int )gd.getNextNumber();
		rod = ( float )gd.getNextNumber();
		max_epsilon = ( float )gd.getNextNumber();
		min_inlier_ratio = ( float )gd.getNextNumber();
		upscale = gd.getNextBoolean();
		if ( upscale ) scale = 2.0f;
		else scale = 1.0f;
		method = gd.getNextChoiceIndex();
		
		ImageProcessor ip1 = imp1.getProcessor().convertToFloat();
		ImageProcessor ip2 = imp2.getProcessor().convertToFloat();
		
		Vector< Feature > fs1;
		Vector< Feature > fs2;

		FloatArray2DSIFT sift = new FloatArray2DSIFT( fdsize, fdbins );
		
		FloatArray2D fa1 = ImageArrayConverter.ImageToFloatArray2D( ip1 );
		Filter.enhance( fa1, 1.0f );
		FloatArray2D fa2 = ImageArrayConverter.ImageToFloatArray2D( ip2 );
		Filter.enhance( fa2, 1.0f );
		
		float[] initial_kernel;
		
		if ( upscale )
		{
			FloatArray2D fat = new FloatArray2D( fa1.width * 2 - 1, fa1.height * 2 - 1 ); 
			FloatArray2DScaleOctave.upsample( fa1, fat );
			fa1 = fat;
			fat = new FloatArray2D( fa2.width * 2 - 1, fa2.height * 2 - 1 ); 
			FloatArray2DScaleOctave.upsample( fa2, fat );
			fa2 = fat;
			initial_kernel = Filter.createGaussianKernel( ( float )Math.sqrt( initial_sigma * initial_sigma - 1.0 ), true );
		}
		else
			initial_kernel = Filter.createGaussianKernel( ( float )Math.sqrt( initial_sigma * initial_sigma - 0.25 ), true );
			
		fa1 = Filter.convolveSeparable( fa1, initial_kernel, initial_kernel );
		fa2 = Filter.convolveSeparable( fa2, initial_kernel, initial_kernel );
		
		
		long start_time = System.currentTimeMillis();
		IJ.log( "Processing SIFT ..." );
		sift.init( fa1, steps, initial_sigma, min_size, max_size );
		fs1 = sift.run( max_size );
		Collections.sort( fs1 );
		IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms." );
		IJ.log( fs1.size() + " features extracted." );
		
		start_time = System.currentTimeMillis();
		IJ.log( "Processing SIFT ..." );
		sift.init( fa2, steps, initial_sigma, min_size, max_size );
		fs2 = sift.run( max_size);
		Collections.sort( fs2 );
		IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms." );
		IJ.log( fs2.size() + " features extracted." );
			
		start_time = System.currentTimeMillis();
		IJ.log( "Identifying correspondence candidates using brute force ..." );
		Vector< PointMatch > candidates = 
				//FloatArray2DSIFT.createMatches( fs1, fs2, 100.0f, null, Float.MAX_VALUE );
				FloatArray2DSIFT.createMatches( fs1, fs2, rod );
		IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms." );	
		IJ.log( candidates.size() + " potentially corresponding features identified." );
			
		start_time = System.currentTimeMillis();
		IJ.log( "Filtering correspondence candidates by geometric consensus ..." );
		// filter false positives
		Vector< PointMatch > inliers = new Vector< PointMatch >();
		
		// TODO Implement other models for choice
		Model model = null;
		Class< ? extends Model > modelClass = null;
		switch ( method )
		{
		case 0:
			modelClass = TranslationModel2D.class;
			break;
		case 1:
			modelClass = RigidModel2D.class;
			break;
		}
		
		//IJ.showMessage( modelClass.getCanonicalName() );
		
		try
		{
			model = Model.filterRansac(
					modelClass,
					candidates,
					inliers,
					1000,
					max_epsilon,
					min_inlier_ratio );
		}
		catch ( Exception e )
		{
			IJ.error( e.getMessage() );
		}
			
		IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms." );	
		
		if ( model != null )
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
			
			IJ.log( inliers.size() + " corresponding features with an average displacement of " + model.getError() + " identified." );
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
