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
import mpicbg.models.*;

import ij.plugin.*;
import ij.gui.*;
import ij.*;
import ij.process.*;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;

/**
 * Use two sets of {@link PointRoi landmarks} selected in two images to map
 * one image to the other.
 */
public class Transform_Roi implements PlugIn
{
	final static private DecimalFormat decimalFormat = new DecimalFormat();
	final static private DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
	
	final protected ArrayList< PointMatch > matches = new ArrayList< PointMatch >();
	
	final static private String[] methods = new String[]{ "Static Least Squares", "Moving Least Squares" };
	static private int methodIndex = 0;
	
	static float alpha = 1.0f;
	static int meshResolution = 32;
	
	final static private String[] modelClasses = new String[]{ "Translation", "Rigid", "Similarity", "Affine" };
	static private int modelClassIndex = 1;
	
	static private boolean interpolate = true;
	
	protected ImagePlus source;
	protected ImagePlus template;
	
	public Transform_Roi()
	{
		decimalFormatSymbols.setGroupingSeparator( ',' );
		decimalFormatSymbols.setDecimalSeparator( '.' );
		decimalFormat.setDecimalFormatSymbols( decimalFormatSymbols );
		decimalFormat.setMaximumFractionDigits( 3 );
		decimalFormat.setMinimumFractionDigits( 3 );		
	}
	
	final static protected void transform(
			final CoordinateTransform transform,
			final ImageProcessor source,
			final ImageProcessor target )
	{
		for ( int y = 0; y < target.getHeight(); ++y )
		{
			for ( int x = 0; x < target.getWidth(); ++x )
			{
				float[] t = new float[]{ x, y };
				transform.applyInPlace( t );
				target.putPixel( x, y, source.getPixel( ( int )t[ 0 ], ( int )t[ 1 ] ) );
			}
		}	
	}
	
	final static protected void transformInterpolated(
			final CoordinateTransform transform,
			final ImageProcessor source,
			final ImageProcessor target )
	{
		for ( int y = 0; y < target.getHeight(); ++y )
		{
			for ( int x = 0; x < target.getWidth(); ++x )
			{
				float[] t = new float[]{ x, y };
				transform.applyInPlace( t );
				target.putPixel( x, y, source.getPixelInterpolated( t[ 0 ], t[ 1 ] ) );
			}
		}	
	}
	
	final public void run( String args )
	{
		matches.clear();
		
		if ( !setup() ) return;
		
		ImagePlus target = template.createImagePlus();
		
		ImageProcessor ipSource = source.getProcessor();
		ImageProcessor ipTarget = template.getProcessor().duplicate();
		
		// TODO Implement other models for choice
		Model< ? > model;
		switch ( modelClassIndex )
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
		
		// Now, collect the PointRois from both images and make PointMatches of them.
		PointRoi roiSource = ( PointRoi )source.getRoi();
		PointRoi roiTemplate = ( PointRoi )template.getRoi();
		
		int offsetSourceX = ( int )roiSource.getBoundingRect().getX();
		int offsetSourceY = ( int )roiSource.getBoundingRect().getY();
		int offsetTemplateX = ( int )roiTemplate.getBoundingRect().getX();
		int offsetTemplateY = ( int )roiTemplate.getBoundingRect().getY();
		
		int[] roiSourceX = roiSource.getXCoordinates();
		int[] roiSourceY = roiSource.getYCoordinates();
		int[] roiTemplateX = roiTemplate.getXCoordinates();
		int[] roiTemplateY = roiTemplate.getYCoordinates();
		
		final int numMatches = Math.min( roiSource.getNCoordinates(), roiTemplate.getNCoordinates() );
		
		for ( int i = 0; i < numMatches; ++i )
		{
			final Point pSource = new Point( new float[]{ roiSourceX[ i ] + offsetSourceX, roiSourceY[ i ] + offsetSourceY } );
			final Point pTemplate = new Point( new float[]{ roiTemplateX[ i ] + offsetTemplateX, roiTemplateY[ i ] + offsetTemplateY } );
			matches.add( new PointMatch( pTemplate, pSource ) );
		}
		
		try
		{
			model.fit( matches );
		}
		catch ( NotEnoughDataPointsException e )
		{
			IJ.showMessage( "Not enough landmarks selected to find a transformation model." );
			return;
		}
		catch ( IllDefinedDataPointsException e )
		{
			IJ.showMessage( "The set of landmarks is ill-defined in terms of the desired transformation." );
			return;
		}
		
		if ( interpolate )
			transformInterpolated( model, ipSource, ipTarget );
		else
			transform( model, ipSource, ipTarget );
		
		target.setProcessor( "Transformed" + source.getTitle(), ipTarget );
		target.show();
	}
	
	final protected boolean setup()
	{
		if ( IJ.versionLessThan( "1.40c" ) ) return false;
		
		final int[] ids = WindowManager.getIDList();
		if ( ids == null || ids.length < 2 )
		{
			IJ.showMessage( "You should have at least two images open." );
			return false;
		}
		
		final ArrayList< String > titlesList = new ArrayList< String >();
		String currentTitle = null;
		for ( int i = 0; i < ids.length; ++i )
		{
			final ImagePlus imp = WindowManager.getImage( ids[ i ] );
			final Roi roi = imp.getRoi();
			if ( roi != null && roi.getType() == Roi.POINT )
			{
				titlesList.add( imp.getTitle() );
				if ( imp == WindowManager.getCurrentImage() )
					currentTitle = imp.getTitle();
			}	
		}
		
		if ( titlesList.size() < 2 )
		{
			IJ.showMessage( "You should have at least two images with selected landmark correspondences open." );
			return false;
		}
		final String[] titles = new String[ titlesList.size() ];
		titlesList.toArray( titles );
		
		if ( currentTitle == null )
			currentTitle = titles[ 0 ];
		final GenericDialog gd = new GenericDialog( "Transform" );
		
		gd.addChoice( "source_image", titles, currentTitle );
		gd.addChoice( "template_image", titles, currentTitle.equals( titles[ 0 ] ) ? titles[ 1 ] : titles[ 0 ] );
		gd.addChoice( "transformation_method", methods, methods[ methodIndex ] );
		gd.addNumericField( "alpha", alpha, 2 );
		gd.addNumericField( "mesh_resolution", meshResolution, 0 );
		gd.addChoice( "transformation_class", modelClasses, modelClasses[ modelClassIndex ] );
		gd.addCheckbox( "interpolate", interpolate );
		gd.showDialog();
		
		if ( gd.wasCanceled() ) return false;
		
		source = WindowManager.getImage( ids[ gd.getNextChoiceIndex() ] );
		template = WindowManager.getImage( ids[ gd.getNextChoiceIndex() ] );
		methodIndex = gd.getNextChoiceIndex();
		alpha = ( float )gd.getNextNumber();
		meshResolution = ( int )gd.getNextNumber();
		modelClassIndex = gd.getNextChoiceIndex();
		interpolate = gd.getNextBoolean();
		
		return true;		
	}
}
