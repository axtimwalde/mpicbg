import mpicbg.ij.InverseTransformMapping;
import mpicbg.ij.Mapping;
import mpicbg.ij.TransformMeshMapping;
import mpicbg.ij.util.Util;
import mpicbg.models.Affine2D;
import mpicbg.models.AffineModel2D;
import mpicbg.models.CoordinateTransform;
import mpicbg.models.CoordinateTransformMesh;
import mpicbg.models.HomographyModel2D;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.InverseCoordinateTransform;
import mpicbg.models.Model;
import mpicbg.models.MovingLeastSquaresTransform;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import mpicbg.models.RigidModel2D;
import mpicbg.models.SimilarityModel2D;
import mpicbg.models.TranslationModel2D;

import ij.plugin.*;
import ij.gui.*;
import ij.*;
import ij.process.*;

import java.awt.geom.AffineTransform;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Use two sets of {@link PointRoi landmarks} selected in two images to map
 * one image to the other.
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.2b
 */
public class Transform_Roi implements PlugIn
{
	final static private DecimalFormat decimalFormat = new DecimalFormat();
	final static private DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
	
	final static private String[] methods = new String[]{ "Least Squares", "Moving Least Squares (non-linear)" };
	static private int methodIndex = 0;
	
	static float alpha = 1.0f;
	static int meshResolution = 32;
	
	final static private String[] modelClasses = new String[]{ "Translation", "Rigid", "Similarity", "Affine", "Perspective" };
	static private int modelClassIndex = 1;
	
	static private boolean interpolate = true;
	
	protected ImagePlus source;
	protected ImagePlus template;
	private boolean showMatrix;
	
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
		final ArrayList< PointMatch > matches = new ArrayList< PointMatch >();
		
		if ( !setup() ) return;
		
		final ImagePlus target = template.createImagePlus();
		
		final ImageProcessor ipSource = source.getProcessor();
		final ImageProcessor ipTarget = source.getProcessor().createProcessor( template.getWidth(), template.getHeight() );
		
		/* Collect the PointRois from both images and make PointMatches of them. */
		final List< Point > sourcePoints = Util.pointRoiToPoints( ( PointRoi )source.getRoi() );
		final List< Point > templatePoints = Util.pointRoiToPoints( ( PointRoi )template.getRoi() );
		
		final int numMatches = Math.min( sourcePoints.size(), templatePoints.size() );
		
		for ( int i = 0; i < numMatches; ++i )
			matches.add( new PointMatch( sourcePoints.get( i ), templatePoints.get( i ) ) );
		
		final Mapping< ? > mapping;
		
		if ( methodIndex == 0 )
		{
			/* TODO Implement other models for choice */
			Model< ? > model;
			InverseCoordinateTransform ict;
			switch ( modelClassIndex )
			{
			case 0:
				final TranslationModel2D t = new TranslationModel2D();
				model = t;
				ict = t;
				break;
			case 1:
				final RigidModel2D r = new RigidModel2D();
				model = r;
				ict = r;
				break;
			case 2:
				final SimilarityModel2D s = new SimilarityModel2D();
				model = s;
				ict = s;
				break;
			case 3:
				final AffineModel2D a = new AffineModel2D();
				model = a;
				ict = a;
				break;
			case 4:
				final HomographyModel2D h = new HomographyModel2D();
				model = h;
				ict = h;
				break;
			default:
				return;
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

			if (showMatrix) {
				final AffineTransform transformation = ((Affine2D<?>)model).createAffine();
				final double[] flatmatrix = new double[6];
				transformation.getMatrix(flatmatrix);
				IJ.log("Matrix: " + Arrays.toString(flatmatrix));
			}

			mapping = new InverseTransformMapping< InverseCoordinateTransform >( ict );
		}
		else
		{
			final MovingLeastSquaresTransform t = new MovingLeastSquaresTransform();
			/* TODO Implement other models for choice */
			try
			{
				switch ( modelClassIndex )
				{
				case 0:
					t.setModel( TranslationModel2D.class );
					break;
				case 1:
					t.setModel( RigidModel2D.class );
					break;
				case 2:
					t.setModel( SimilarityModel2D.class );
					break;
				case 3:
					t.setModel( AffineModel2D.class );
					break;
				case 4:
					IJ.error( "Perspective transformation is not yet supported for Moving Least Squares.  Using Affine instead." );
					//t.setModel( HomographyModel2D.class );
					t.setModel( AffineModel2D.class );
					break;
				default:
					return;
				}
			}
			catch ( Exception e ) { return; }
			t.setAlpha( alpha );
			
			try
			{
				t.setMatches( matches );
				mapping = new TransformMeshMapping< CoordinateTransformMesh >( new CoordinateTransformMesh( t, meshResolution, source.getWidth(), source.getHeight() ) );
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
			if (showMatrix) {
				IJ.log("Cannot show matrix for non-linear transformation");
			}
		}
		
		if ( interpolate )
		{
			ipSource.setInterpolationMethod( ImageProcessor.BILINEAR );
			mapping.mapInterpolated( ipSource, ipTarget );
		}
		else
			mapping.map( ipSource, ipTarget );
		
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
		final ArrayList< Integer > idsList = new ArrayList< Integer >();
		String currentTitle = null;
		for ( int i = 0; i < ids.length; ++i )
		{
			final ImagePlus imp = WindowManager.getImage( ids[ i ] );
			final Roi roi = imp.getRoi();
			if ( roi != null && roi.getType() == Roi.POINT )
			{
				titlesList.add( imp.getTitle() );
				idsList.add( ids[ i ] );
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
		gd.addCheckbox("show_matrix", false);
		gd.showDialog();
		
		if ( gd.wasCanceled() ) return false;
		
		source = WindowManager.getImage( idsList.get( gd.getNextChoiceIndex() ) );
		template = WindowManager.getImage( idsList.get( gd.getNextChoiceIndex() ) );
		methodIndex = gd.getNextChoiceIndex();
		alpha = ( float )gd.getNextNumber();
		meshResolution = ( int )gd.getNextNumber();
		modelClassIndex = gd.getNextChoiceIndex();
		interpolate = gd.getNextBoolean();
		showMatrix = gd.getNextBoolean();
		
		return true;		
	}
}
