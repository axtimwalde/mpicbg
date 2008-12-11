import mpicbg.ij.Mapping;
import mpicbg.ij.InverseTransformMapping;
import mpicbg.imagefeatures.*;
import mpicbg.models.*;

import ij.plugin.*;
import ij.gui.*;
import ij.*;
import ij.process.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.awt.Color;
import java.awt.TextField;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * Align a stack consecutively using automatically extracted robust landmark
 * correspondences.
 * 
 * The plugin uses the Scale Invariant Feature Transform (SIFT) by David Lowe
 * \cite{Lowe04} and the Random Sample Consensus (RANSAC) by Fishler and Bolles
 * \citet{FischlerB81} to identify landmark correspondences.
 * 
 * It identifies a rigid transformation for the second of two slices that maps
 * the correspondences of the second optimally to those of the first.
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
 * @version 0.4b
 */
public class SIFT_Align implements PlugIn, KeyListener
{
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
		
		public boolean interpolate = true;
		
		public boolean showInfo = false;
	}
	
	final static Param p = new Param(); 

	/**
	 * downscale a grey scale float image using gaussian blur
	 */
	final static private ImageProcessor downScale( ImageProcessor ip, float s )
	{
		final FloatArray2D g = new FloatArray2D( ip.getWidth(), ip.getHeight() );
		ImageArrayConverter.imageProcessorToFloatArray2D( ip, g );

		float sigma = ( float )Math.sqrt( 0.25 * 0.25 / s / s - 0.25 );
		float[] kernel = Filter.createGaussianKernel( sigma, true );
		
		final FloatArray2D h = Filter.convolveSeparable( g, kernel, kernel );
		
		final FloatProcessor fp = new FloatProcessor( ip.getWidth(), ip.getHeight() );

		ImageArrayConverter.floatArray2DToFloatProcessor( h, fp );
		return ip.resize( ( int )( s * ip.getWidth() ) );
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
	
	final public void run( final String args )
	{
		fs1.clear();
		fs2.clear();
		
		if ( IJ.versionLessThan( "1.41n" ) ) return;

		final ImagePlus imp = WindowManager.getCurrentImage();
		if ( imp == null )  { System.err.println( "There are no images open" ); return; }
		
		GenericDialog gd = new GenericDialog( "Align stack" );
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
		
		gd.addMessage( "Output:" );
		gd.addCheckbox( "interpolate", p.interpolate );
		gd.addCheckbox( "show_info", p.showInfo );
		
		gd.showDialog();
		
		if (gd.wasCanceled()) return;
		
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
		
		p.interpolate = gd.getNextBoolean();
		p.showInfo = gd.getNextBoolean();
		
		ImageStack stack = imp.getStack();
		ImageStack stackAligned = new ImageStack( stack.getWidth(), stack.getHeight() );
		
		float vis_scale = 256.0f / imp.getWidth();
		ImageStack stackInfo = null;
		ImagePlus impInfo = null;
		
		if ( p.showInfo )
			stackInfo = new ImageStack(
					Math.round( vis_scale * stack.getWidth() ),
					Math.round( vis_scale * stack.getHeight() ) );
		
		stackAligned.addSlice( null, stack.getProcessor( 1 ) );
		ImagePlus impAligned = new ImagePlus( "Aligned 1 of " + stack.getSize(), stackAligned );
		impAligned.show();
		
		ImageProcessor ip1;
		ImageProcessor ip2 = stack.getProcessor( 1 );
		ImageProcessor ip3 = null;
		
		FloatArray2DSIFT sift = new FloatArray2DSIFT( p.fdSize, p.fdBins );
		extractFeatures( ip2, fs2, sift, p );
		
		// downscale ip2 for visualisation purposes
		if ( p.showInfo )
			ip2 = downScale( ip2, vis_scale );
		
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
		Mapping mapping = new InverseTransformMapping( model );
		
		for ( int i = 1; i < stack.getSize(); ++i )
		{
			ip1 = ip2;
			ip2 = stack.getProcessor( i + 1 );
			
			fs1.clear();
			fs1.addAll( fs2 );
			fs2.clear();
			
			extractFeatures( ip2, fs2, sift, p );
			
			long start_time = System.currentTimeMillis();
			System.out.print( "identifying correspondences using brute force ..." );
			Vector< PointMatch > candidates = 
				FloatArray2DSIFT.createMatches( fs2, fs1, 1.5f, null, Float.MAX_VALUE, p.rod );
			System.out.println( " took " + ( System.currentTimeMillis() - start_time ) + "ms" );
			
			IJ.log( candidates.size() + " potentially corresponding features identified" );
			
			/**
			 * draw all correspondence candidates
			 */
			if (p.showInfo )
			{
				ip2 = downScale( ip2, vis_scale );
			
				ip1 = ip1.convertToRGB();
				ip3 = ip2.convertToRGB();
				ip1.setColor( Color.red );
				ip3.setColor( Color.red );

				ip1.setLineWidth( 2 );
				ip3.setLineWidth( 2 );
				for ( PointMatch m : candidates )
				{
					float[] m_p1 = m.getP1().getL(); 
					float[] m_p2 = m.getP2().getL(); 
					
					ip1.drawDot( ( int )Math.round( vis_scale / scale * m_p2[ 0 ] ), ( int )Math.round( vis_scale / scale * m_p2[ 1 ] ) );
					ip3.drawDot( ( int )Math.round( vis_scale / scale * m_p1[ 0 ] ), ( int )Math.round( vis_scale / scale * m_p1[ 1 ] ) );
				}
			}

			Vector< PointMatch > inliers = new Vector< PointMatch >();
			
			// TODO Implement other models for choice
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
				if ( p.showInfo )
				{
					ip1.setColor( Color.green );
					ip3.setColor( Color.green );
					ip1.setLineWidth( 2 );
					ip3.setLineWidth( 2 );
					for ( PointMatch m : inliers )
					{
						float[] m_p1 = m.getP1().getL(); 
						float[] m_p2 = m.getP2().getL(); 
						
						ip1.drawDot( ( int )Math.round( vis_scale / scale * m_p2[ 0 ] ), ( int )Math.round( vis_scale / scale * m_p2[ 1 ] ) );
						ip3.drawDot( ( int )Math.round( vis_scale / scale * m_p1[ 0 ] ), ( int )Math.round( vis_scale / scale * m_p1[ 1 ] ) );
					}
				}

				/**
				 * append the estimated transformation model
				 * 
				 */
				model.concatenate( currentModel );
			}
			
			ImageProcessor alignedSlice = stack.getProcessor( i + 1 ).duplicate();
			if ( p.interpolate )
				mapping.mapInterpolated( stack.getProcessor( i + 1 ), alignedSlice );
			else
				mapping.map( stack.getProcessor( i + 1 ), alignedSlice );
			
			stackAligned.addSlice( null, alignedSlice );
			if ( p.showInfo )
			{
				ImageProcessor tmp;
				tmp = ip1.createProcessor( stackInfo.getWidth(), stackInfo.getHeight() );
				tmp.insert( ip1, 0, 0 );
				stackInfo.addSlice( null, tmp ); // fixing silly 1 pixel size missmatches
				tmp = ip3.createProcessor( stackInfo.getWidth(), stackInfo.getHeight() );
				tmp.insert( ip3, 0, 0 );
				stackInfo.addSlice( null, tmp );
				if ( i == 1 )
				{
					impInfo = new ImagePlus( "Alignment info", stackInfo );
					impInfo.show();
				}
				impInfo.setStack( "Alignment info", stackInfo );
				impInfo.updateAndDraw();
			}
			impAligned.setStack( "Aligned " + stackAligned.getSize() + " of " + stack.getSize(), stackAligned );
			impAligned.updateAndDraw();
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
