import mpicbg.imagefeatures.*;
import mpicbg.models.*;

import ij.plugin.*;
import ij.gui.*;
import ij.*;
import ij.process.*;

import java.util.Collections;
import java.util.Vector;
import java.util.HashMap;
import java.util.ArrayList;
import java.awt.geom.GeneralPath;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.TextField;
import java.awt.Event;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Extract landmark correspondences in two images as PointRoi.
 * 
 * This plugin extracts Multi-Scale Oriented Patches \cite{BrownAl05}
 * and the Random Sample Consensus (RANSAC) by Fishler and Bolles
 * \citet{FischlerB81} with respect to a transformation model to identify
 * landmark correspondences.
 * 
 * BibTeX:
 * <pre>
 * &#64;inproceedings{BrownAl05,
 *   author    = {Matthew Brown and Richard Szeliski and Simon Winder},
 *   title     = {Multi-Image Matching Using Multi-Scale Oriented Patches},
 *   booktitle = {CVPR '05: Proceedings of the 2005 IEEE Computer Society Conference on Computer Vision and Pattern Recognition (CVPR'05) - Volume 1},
 *   year      = {2005},
 *   isbn      = {0-7695-2372-2},
 *   pages     = {510--517},
 *   publisher = {IEEE Computer Society},
 *   address   = {Washington, DC, USA},
 *   doi       = {http://dx.doi.org/10.1109/CVPR.2005.235},
 *   url       = {http://www.cs.ubc.ca/~mbrown/papers/cvpr05.pdf},
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
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 */
public class MOPS_ExtractPointRoi implements PlugIn, MouseListener, KeyListener, ImageListener
{
	// steps
	private static int steps = 3;
	// initial sigma
	private static float initial_sigma = 1.6f;
	// feature descriptor size
	private static int fdsize = 16;
	// size restrictions for scale octaves, use octaves < max_size and > min_size only
	private static int min_size = 64;
	private static int max_size = 1024;
	// maximal allowed alignment error in px
	private static float max_epsilon = 25.0f;
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
	
	private ImagePlus imp1;
	private ImagePlus imp2;
	
	private ImagePlus impFeature1;
	private ImagePlus impFeature2;
	
	private Vector< Feature > fs1;
	private Vector< Feature > fs2;
	final private HashMap< Point, Feature > m1 = new HashMap< Point, Feature >();
	final private HashMap< Point, Feature > m2 = new HashMap< Point, Feature >();
	final private ArrayList< Feature > i1 = new ArrayList< Feature >();
	final private ArrayList< Feature > i2 = new ArrayList< Feature >();
	
	public void run( String args )
	{
		// cleanup
		impFeature1 = null;
		impFeature2 = null;
		m1.clear();
		m2.clear();
		i1.clear();
		i2.clear();
		
		if ( IJ.versionLessThan( "1.40c" ) ) return;
		
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
		
		gd.addMessage( "MOPS Parameters:" );
		gd.addNumericField( "steps_per_scale_octave :", steps, 0 );
		gd.addNumericField( "initial_gaussian_blur :", initial_sigma, 2 );
		gd.addNumericField( "feature_descriptor_width :", fdsize, 0 );
		gd.addNumericField( "minimum_image_size :", min_size, 0 );
		gd.addNumericField( "maximum_image_size :", max_size, 0 );
		gd.addNumericField( "maximal_alignment_error :", max_epsilon, 2 );
		gd.addNumericField( "inlier_ratio :", min_inlier_ratio, 2 );
		gd.addCheckbox( "upscale_image_first", upscale );
		gd.addChoice( "transformation_class", methods, methods[ 1 ] );
		gd.showDialog();
		
		if (gd.wasCanceled()) return;
		
		Toolbar.getInstance().setTool( Toolbar.getInstance().addTool( "Select_a_Feature" ) );
		
		imp1 = WindowManager.getImage( ids[ gd.getNextChoiceIndex() ] );
		imp2 = WindowManager.getImage( ids[ gd.getNextChoiceIndex() ] );	
		
		steps = ( int )gd.getNextNumber();
		initial_sigma = ( float )gd.getNextNumber();
		fdsize = ( int )gd.getNextNumber();
		min_size = ( int )gd.getNextNumber();
		max_size = ( int )gd.getNextNumber();
		max_epsilon = ( float )gd.getNextNumber();
		min_inlier_ratio = ( float )gd.getNextNumber();
		upscale = gd.getNextBoolean();
		if ( upscale ) scale = 2.0f;
		else scale = 1.0f;
		method = gd.getNextChoiceIndex();
		
		ImageProcessor ip1 = imp1.getProcessor().convertToFloat();
		ImageProcessor ip2 = imp2.getProcessor().convertToFloat();
		
		FloatArray2DMOPS mops = new FloatArray2DMOPS( fdsize );
		
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
		IJ.log( "Processing MOPS ..." );
		mops.init( fa1, steps, initial_sigma, min_size, max_size );
		fs1 = mops.run( max_size );
		Collections.sort( fs1 );
		IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms." );
		IJ.log( fs1.size() + " features extracted." );
		
		start_time = System.currentTimeMillis();
		IJ.log( "Processing MOPS ..." );
		mops.init( fa2, steps, initial_sigma, min_size, max_size );
		fs2 = mops.run( max_size);
		Collections.sort( fs2 );
		IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms." );
		IJ.log( fs2.size() + " features extracted." );
			
		start_time = System.currentTimeMillis();
		IJ.log( "Identifying correspondence candidates using brute force ..." );
		Vector< PointMatch > candidates = 
				FloatArray2DMOPS.createMatches( fs1, fs2, m1, m2 );
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
			System.err.println( e.getMessage() );
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
				
				i1.add( m1.get( m.getP1() ) );
				i2.add( m2.get( m.getP2() ) );
				
				++i;
			}
			
			// unscale the location of all features for visualization purposes
			for ( Feature f : i1 )
			{
				f.location[ 0 ] /= scale;
				f.location[ 1 ] /= scale;
				f.scale /= scale;
			}
			for ( Feature f : i2 )
			{
				f.location[ 0 ] /= scale;
				f.location[ 1 ] /= scale;
				f.scale /= scale;
			}
		
		
			PointRoi pr1 = new PointRoi( x1, y1, inliers.size() );
			PointRoi pr2 = new PointRoi( x2, y2, inliers.size() );
			
			imp1.setRoi( pr1 );
			imp2.setRoi( pr2 );
			
			IJ.log( inliers.size() + " corresponding features with a maximal displacement of " + model.getError() + " identified." );
			
			imp1.getCanvas().addMouseListener( this );
			imp2.getCanvas().addMouseListener( this );
			imp1.getCanvas().addKeyListener( this );
			imp2.getCanvas().addKeyListener( this );
			ImagePlus.addImageListener( this );
		}
		else
		{
			IJ.log( "No correspondences found." );
		}
	}
	
	
	/**
	 * Create a Shape that illustrates the descriptor patch.
	 * 
	 * @param f the feature to be illustrated
	 * @return the illustration
	 */
	public static Shape createFeatureDescriptorShape( Feature f )
	{
		GeneralPath path = new GeneralPath();
		
		int w = ( int )Math.sqrt( f.descriptor.length );
		double scale = f.scale * ( double )w * 4.0  / 2.0;
		double sin = Math.sin( f.orientation );
		double cos = Math.cos( f.orientation );

		double fx = f.location[ 0 ];
		double fy = f.location[ 1 ];
		double pd = 4.0 / scale;

		
		path.moveTo(
				fx + ( -cos + sin ) * scale,
				fy + ( -sin - cos ) * scale );
		path.lineTo(
				fx + ( sin + cos ) * scale,
				fy + ( sin - cos ) * scale );
		path.lineTo(
				fx + ( cos - sin ) * scale,
				fy + ( sin + cos ) * scale );
		path.lineTo(
				fx - ( sin + cos ) * scale,
				fy - ( sin - cos ) * scale );
		path.closePath();
		
		// Mark the upper left corner with a little arrow
		path.moveTo(
				fx + ( ( 1.0 + pd ) * cos - ( 1.0 + pd ) * sin ) * scale,
				fy + ( ( 1.0 + pd ) * sin + ( 1.0 + pd ) * cos ) * scale );
		path.lineTo(
				fx + ( ( 1.0 + 4 * pd ) * cos - ( 1.0 + 2.5 * pd ) * sin ) * scale,
				fy + ( ( 1.0 + 4 * pd ) * sin + ( 1.0 + 2.5 * pd ) * cos ) * scale );
		path.lineTo(
				fx + ( ( 1.0 + 2.75 * pd ) * cos - ( 1.0 + 2.75 * pd ) * sin ) * scale,
				fy + ( ( 1.0 + 2.75 * pd ) * sin + ( 1.0 + 2.75 * pd ) * cos ) * scale );
		path.lineTo(
				fx + ( ( 1.0 + 2.5 * pd ) * cos - ( 1.0 + 4 * pd ) * sin ) * scale,
				fy + ( ( 1.0 + 2.5 * pd ) * sin + ( 1.0 + 4 * pd ) * cos ) * scale );
		path.closePath();
		
		for ( int y = 1; y < w; ++y )
		{
			double dy = 1.0 - y * 2.0 / w;
			path.moveTo(
					fx + ( -cos + dy * sin ) * scale,
					fy + ( -sin - dy * cos ) * scale );
			path.lineTo(
					fx + ( cos + dy * sin ) * scale,
					fy + ( sin - dy * cos ) * scale );
		}
	    for ( int x = 1; x < w; ++x )
	    {
	    	double dx = 1.0 - x * 2.0 / w;
	    	path.moveTo(
	    			fx + ( dx * cos + sin ) * scale,
	    			fy + ( dx * sin - cos ) * scale );
	    	path.lineTo(
	    			fx + ( dx * cos - sin ) * scale,
	    			fy + ( dx * sin + cos ) * scale );
	    }
	    
	    return path;
	}
	
	public static void drawFeatureDescriptor( FloatProcessor fp, Feature f )
	{
		fp.setMinAndMax( 0.0, 1.0 );
		int w = ( int )Math.sqrt( f.descriptor.length );
		for ( int y = 0; y < w; ++ y )
			for ( int x = 0; x < w; ++x )
				fp.setf( x, y, f.descriptor[ y * w + x ] );
	}
	
	public static ImagePlus createFeatureDescriptorImage( String title, Feature f )
	{
		int w = ( int )Math.sqrt( f.descriptor.length );
		FloatProcessor fp = new FloatProcessor( w, w );
		drawFeatureDescriptor( fp, f );
		return new ImagePlus( title, fp );
	}
	
	public void imageClosed( ImagePlus imp )
	{
		if ( imp == imp1 && impFeature1 != null )
			impFeature1.close();
		else if ( imp == imp2 && impFeature2 != null )
			impFeature2.close();
	}
	
	public void imageOpened( ImagePlus imp ){}
	public void imageUpdated( ImagePlus imp ){}
	
	public void keyPressed( KeyEvent e)
	{
		if ( e.getKeyCode() == KeyEvent.VK_ESCAPE )
		{
			if ( imp1 != null )
			{
				imp1.getCanvas().removeMouseListener( this );
				imp1.getCanvas().removeKeyListener( this );
				imp1.getCanvas().setDisplayList( null );
				imp1.setRoi( ( Roi )null );
			}
			if ( impFeature1 != null ) impFeature1.close();
			if ( imp2 != null )
			{
				imp2.getCanvas().removeMouseListener( this );
				imp2.getCanvas().removeKeyListener( this );
				imp2.getCanvas().setDisplayList( null );
				imp2.setRoi( ( Roi )null );
			}
			if ( impFeature2 != null ) impFeature2.close();
		}
		else if (
				( e.getKeyCode() == KeyEvent.VK_F1 ) &&
				( e.getSource() instanceof TextField ) ){}
	}

	public void keyReleased( KeyEvent e ){}

	public void keyTyped( KeyEvent e ){}
	
	public void mousePressed( MouseEvent e )
	{
		if ( e.getButton() == MouseEvent.BUTTON1 )
		{
			ImageWindow win = WindowManager.getCurrentWindow();
			int x = win.getCanvas().offScreenX( e.getX() );
			int y = win.getCanvas().offScreenY( e.getY() );
			
			Feature f1;
			Feature f2;
			
			ArrayList< Feature > fl;
			Feature target = null;
			double target_d = Double.MAX_VALUE;
			if ( win.getImagePlus() == imp1 )
				fl = i1;
			else
				fl = i2;
		
			for ( Feature f : fl )
			{
				double dx = win.getCanvas().getMagnification() * ( f.location[ 0 ] - x );
				double dy = win.getCanvas().getMagnification() * ( f.location[ 1 ] - y );
				double d =  dx * dx + dy * dy;
				if ( d < 64.0 && d < target_d )
				{
					target = f;
					target_d = d;
				}
			}
			
			if ( target != null )
			{
				if ( imp1 != null && imp1.isVisible() )
				{
					f1 = i1.get( fl.indexOf( target ) );
					//imp1.getCanvas().setDisplayList( createFeatureShape( f1 ), Roi.getColor(), null );
					imp1.getCanvas().setDisplayList( createFeatureDescriptorShape( f1 ), Roi.getColor(), null );
					if ( impFeature1 == null || !impFeature1.isVisible() )
					{
						impFeature1 = createFeatureDescriptorImage( "Feature " + imp1.getTitle(), f1 );
						impFeature1.updateAndDraw();
						impFeature1.show();
						impFeature1.getWindow().setLocationAndSize(
								impFeature1.getWindow().getX(),
								impFeature1.getWindow().getY(),
								fdsize * 16, fdsize * 16 );
					}
					else
					{
						drawFeatureDescriptor( ( FloatProcessor )impFeature1.getProcessor().convertToFloat(), f1 );
						impFeature1.updateAndDraw();
						impFeature1.show();
					}
				}
				
				if ( imp2 != null && imp2.isVisible() )
				{
					f2 = i2.get( fl.indexOf( target ) );
					//imp2.getCanvas().setDisplayList( createFeatureShape( f2 ), Roi.getColor(), null );
					imp2.getCanvas().setDisplayList( createFeatureDescriptorShape( f2 ), Roi.getColor(), null );
					if ( impFeature2 == null || !impFeature2.isVisible() )
					{
						impFeature2 = createFeatureDescriptorImage( "Feature " + imp2.getTitle(), f2 );
						impFeature2.updateAndDraw();
						impFeature2.show();
						impFeature2.getWindow().setLocationAndSize(
								impFeature2.getWindow().getX(),
								impFeature2.getWindow().getY(),
								fdsize * 16, fdsize * 16 );
					}
					else
					{
						drawFeatureDescriptor( ( FloatProcessor )impFeature2.getProcessor().convertToFloat(), f2 );
						impFeature2.updateAndDraw();
						impFeature2.show();
					}
				}
			}
			else
			{
				if ( imp1 != null && imp1.isVisible() )
					imp1.getCanvas().setDisplayList( null );
				if ( imp2 != null && imp2.isVisible() )
					imp2.getCanvas().setDisplayList( null );
			}
		}
		
		//IJ.log( "Mouse pressed: " + x + ", " + y + " " + modifiers( e.getModifiers() ) );
	}
	
	public void mouseReleased( MouseEvent e ){}
	public void mouseExited(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {}	
	public void mouseEntered(MouseEvent e) {}
	public void mouseMoved(MouseEvent e) {}

	
	public static String modifiers( int flags )
	{
		String s = " [ ";
		if ( flags == 0 )
			return "";
		if ( ( flags & Event.SHIFT_MASK ) != 0 )
			s += "Shift ";
		if ( ( flags & Event.CTRL_MASK ) != 0 )
			s += "Control ";
		if ( ( flags & Event.META_MASK ) != 0 )
			s += "Meta (right button) ";
		if ( ( flags & Event.ALT_MASK ) != 0 )
			s += "Alt ";
		s += "]";
		if ( s.equals( " [ ]" ) )
			s = " [no modifiers]";
		return s;
	}


}
