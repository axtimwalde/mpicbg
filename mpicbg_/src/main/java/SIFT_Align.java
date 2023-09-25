import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.TextField;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import mpicbg.ij.InverseTransformMapping;
import mpicbg.ij.Mapping;
import mpicbg.ij.SIFT;
import mpicbg.imagefeatures.Feature;
import mpicbg.imagefeatures.Filter;
import mpicbg.imagefeatures.FloatArray2D;
import mpicbg.imagefeatures.FloatArray2DSIFT;
import mpicbg.imagefeatures.ImageArrayConverter;
import mpicbg.models.AbstractAffineModel2D;
import mpicbg.models.AffineModel2D;
import mpicbg.models.PointMatch;
import mpicbg.models.RigidModel2D;
import mpicbg.models.SimilarityModel2D;
import mpicbg.models.TranslationModel2D;

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
 * @author Stephan Saalfeld &lt;saalfeld@mpi-cbg.de&gt;
 * @version 0.4b
 */
public class SIFT_Align implements PlugIn, KeyListener
{
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

		public boolean interpolate = true;

		public boolean showInfo = false;
		
		public boolean showMatrix = false;
	}

	final static Param p = new Param();

	/**
	 * downscale a grey scale float image using gaussian blur
	 */
	final static private ImageProcessor downScale( final ImageProcessor ip, final double s )
	{
		final FloatArray2D g = new FloatArray2D( ip.getWidth(), ip.getHeight() );
		ImageArrayConverter.imageProcessorToFloatArray2D( ip, g );

		final float sigma = ( float )Math.sqrt( 0.25 * 0.25 / s / s - 0.25 );
		final float[] kernel = Filter.createGaussianKernel( sigma, true );

		final FloatArray2D h = Filter.convolveSeparable( g, kernel, kernel );

		final FloatProcessor fp = new FloatProcessor( ip.getWidth(), ip.getHeight() );

		ImageArrayConverter.floatArray2DToFloatProcessor( h, fp );
		return ip.resize( ( int )( s * ip.getWidth() ) );
	}

	@Override
    final public void run( final String args )
	{
		fs1.clear();
		fs2.clear();

		if ( IJ.versionLessThan( "1.41n" ) ) return;

		final ImagePlus imp = WindowManager.getCurrentImage();
		if ( imp == null )  { System.err.println( "There are no images open" ); return; }

		final GenericDialog gd = new GenericDialog( "Align stack" );
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

		gd.addMessage( "Output:" );
		gd.addCheckbox( "interpolate", p.interpolate );
		gd.addCheckbox( "show_info", p.showInfo );
		gd.addCheckbox( "show_transformation_matrix", p.showMatrix );
		
		gd.showDialog();

		if (gd.wasCanceled()) return;

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

		p.interpolate = gd.getNextBoolean();
		p.showInfo = gd.getNextBoolean();
		p.showMatrix = gd.getNextBoolean();

		final ImageStack stack = imp.getStack();
		final ImageStack stackAligned = new ImageStack( stack.getWidth(), stack.getHeight() );

		final float vis_scale = 256.0f / imp.getWidth();
		ImageStack stackInfo = null;
		ImagePlus impInfo = null;

		if ( p.showInfo )
			stackInfo = new ImageStack(
					Math.round( vis_scale * stack.getWidth() ),
					Math.round( vis_scale * stack.getHeight() ) );

		final ImageProcessor firstSlice = stack.getProcessor( 1 );
		stackAligned.addSlice( stack.getSliceLabel(1), firstSlice.duplicate() );
		stackAligned.getProcessor( 1 ).setMinAndMax( firstSlice.getMin(), firstSlice.getMax() );
		final ImagePlus impAligned = new ImagePlus( "Aligned 1 of " + stack.getSize(), stackAligned );
		impAligned.show();

		ImageProcessor ip1;
		ImageProcessor ip2 = stack.getProcessor( 1 );
		ImageProcessor ip3 = null;
		ImageProcessor ip4 = null;

		final FloatArray2DSIFT sift = new FloatArray2DSIFT( p.sift );
		final SIFT ijSIFT = new SIFT( sift );

		long start_time = System.currentTimeMillis();
		IJ.log( "Processing SIFT ..." );
		ijSIFT.extractFeatures( ip2, fs2 );
		IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms." );
		IJ.log( fs2.size() + " features extracted." );

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
		final Mapping mapping = new InverseTransformMapping< AbstractAffineModel2D< ? > >( model );

		for ( int i = 1; i < stack.getSize(); ++i )
		{
			ip1 = ip2;
			ip2 = stack.getProcessor( i + 1 );

			fs1.clear();
			fs1.addAll( fs2 );
			fs2.clear();

			start_time = System.currentTimeMillis();
			IJ.log( "Processing SIFT ..." );
			ijSIFT.extractFeatures( ip2, fs2 );
			IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms." );
			IJ.log( fs2.size() + " features extracted." );

			start_time = System.currentTimeMillis();
			System.out.print( "identifying correspondences using brute force ..." );
			final Vector< PointMatch > candidates =
				FloatArray2DSIFT.createMatches( fs2, fs1, 1.5f, null, Float.MAX_VALUE, p.rod );
			System.out.println( " took " + ( System.currentTimeMillis() - start_time ) + "ms" );

			IJ.log( candidates.size() + " potentially corresponding features identified" );

			/**
			 * draw all correspondence candidates
			 */
			if (p.showInfo )
			{
				ip2 = downScale( ip2, vis_scale );

				ip3 = ip1.convertToRGB().duplicate();
				ip4 = ip2.convertToRGB().duplicate();
				ip3.setColor( Color.red );
				ip4.setColor( Color.red );

				ip3.setLineWidth( 2 );
				ip4.setLineWidth( 2 );
				for ( final PointMatch m : candidates )
				{
					final double[] m_p1 = m.getP1().getL();
					final double[] m_p2 = m.getP2().getL();

					ip3.drawDot( ( int )Math.round( vis_scale * m_p2[ 0 ] ), ( int )Math.round( vis_scale * m_p2[ 1 ] ) );
					ip4.drawDot( ( int )Math.round( vis_scale * m_p1[ 0 ] ), ( int )Math.round( vis_scale * m_p1[ 1 ] ) );
				}
			}

			final Vector< PointMatch > inliers = new Vector< PointMatch >();

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
			catch ( final Exception e )
			{
				modelFound = false;
				System.err.println( e.getMessage() );
			}
			if ( modelFound )
			{
				if ( p.showInfo )
				{
					ip3.setColor( Color.green );
					ip4.setColor( Color.green );
					ip3.setLineWidth( 2 );
					ip4.setLineWidth( 2 );
					for ( final PointMatch m : inliers )
					{
						final double[] m_p1 = m.getP1().getL();
						final double[] m_p2 = m.getP2().getL();

						ip3.drawDot( ( int )Math.round( vis_scale * m_p2[ 0 ] ), ( int )Math.round( vis_scale * m_p2[ 1 ] ) );
						ip4.drawDot( ( int )Math.round( vis_scale * m_p1[ 0 ] ), ( int )Math.round( vis_scale * m_p1[ 1 ] ) );
					}
				}

				/**
				 * append the estimated transformation model
				 *
				 */
				model.concatenate( currentModel );

				if ( p.showMatrix )
				{
					IJ.log("Transformation Matrix: " + currentModel.createAffine() );
				}
			
			}

//			ImageProcessor alignedSlice = stack.getProcessor( i + 1 ).duplicate();
			final ImageProcessor originalSlice = stack.getProcessor( i + 1 );
			originalSlice.setInterpolationMethod( ImageProcessor.BILINEAR );
			final ImageProcessor alignedSlice = originalSlice.createProcessor( stack.getWidth(), stack.getHeight() );
			alignedSlice.setMinAndMax( originalSlice.getMin(), originalSlice.getMax() );

			if ( p.interpolate )
				mapping.mapInterpolated( originalSlice, alignedSlice );
			else
				mapping.map( originalSlice, alignedSlice );

			String sliceLabel = stack.getSliceLabel( i + 1 );
			stackAligned.addSlice( sliceLabel, alignedSlice );
			if ( p.showInfo )
			{
				ImageProcessor tmp;
				tmp = ip3.createProcessor( stackInfo.getWidth(), stackInfo.getHeight() );
				tmp.insert( ip3, 0, 0 );
				stackInfo.addSlice( sliceLabel, tmp ); // fixing silly 1 pixel size missmatches
				tmp = ip4.createProcessor( stackInfo.getWidth(), stackInfo.getHeight() );
				tmp.insert( ip4, 0, 0 );
				stackInfo.addSlice( sliceLabel, tmp );
				if ( i == 1 )
				{
					impInfo = new ImagePlus( "Alignment info", stackInfo );
					impInfo.show();
				}
				impInfo.setStack( "Alignment info", stackInfo );
				final int currentSlice = impInfo.getSlice();
				impInfo.setSlice( stackInfo.getSize() );
				impInfo.setSlice( currentSlice );
				impInfo.updateAndDraw();
			}
			impAligned.setStack( "Aligned " + stackAligned.getSize() + " of " + stack.getSize(), stackAligned );
			final int currentSlice = impAligned.getSlice();
			impAligned.setSlice( stack.getSize() );
			impAligned.setSlice( currentSlice );
			impAligned.updateAndDraw();
		}

		IJ.log( "Done." );
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
}
