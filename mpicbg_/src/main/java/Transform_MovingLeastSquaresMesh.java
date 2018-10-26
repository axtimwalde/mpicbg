import ij.gui.GenericDialog;

import java.awt.Color;

import mpicbg.ij.InteractiveMapping;
import mpicbg.ij.TransformMeshMapping;
import mpicbg.models.AbstractModel;
import mpicbg.models.AffineModel2D;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.InvertibleCoordinateTransform;
import mpicbg.models.MovingLeastSquaresMesh;
import mpicbg.models.NoninvertibleModelException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import mpicbg.models.RigidModel2D;
import mpicbg.models.SimilarityModel2D;
import mpicbg.models.TranslationModel2D;

/**
 * Smooth image deformation using landmark based deformation by means
 * of Moving Least Squares as described by \citet{SchaeferAl06} inspired by the
 * implementation of Johannes Schindelin.
 *
 * BibTeX:
 * <pre>
 * &#64;article{SchaeferAl06,
 *   author    = {Scott Schaefer and Travis McPhail and Joe Warren},
 *   title     = {Image deformation using moving least squares},
 *   journal   = {ACM Transactions on Graphics},
 *   volume    = {25},
 *   number    = {3},
 *   month     = {July},
 *   year      = {2006},
 *   issn      = {0730-0301},
 *   pages     = {533--540},
 *   publisher = {ACM},
 *   address   = {New York, NY, USA},
 * }
 * </pre>
 *
 * @author Stephan Saalfeld &lt;saalfeld@mpi-cbg.de&gt;
 * @version 0.2b
 */
public class Transform_MovingLeastSquaresMesh extends InteractiveMapping
{
	public static final String NL = System.getProperty( "line.separator" );
	public final static String man =
		"Add some control points with your mouse" + NL +
		"and drag them to deform the image." + NL + " " + NL +
		"ENTER - Apply the deformation." + NL +
		"ESC - Return to the original image." + NL +
		"U - Toggle mesh display.";

	/**
	 * number of vertices in horizontal direction
	 */
	private static int numX = 32;

	/**
	 * alpha [0 smooth, 1 less smooth ;)]
	 */
	private static float alpha = 1.0f;

	/**
	 * local transformation model
	 */
	final static private String[] methods = new String[]{ "Translation", "Rigid", "Similarity", "Affine" };
	static private int method = 1;

	protected MovingLeastSquaresMesh< ? extends AbstractModel< ? > > mesh;

	@Override
	final protected void createMapping()
	{
		mapping = new TransformMeshMapping< MovingLeastSquaresMesh< ? > >( mesh );
	}

	@Override
	final protected void updateMapping() throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		mesh.updateModels();
		mesh.updateAffines();
		updateIllustration();
	}

	@Override
	final protected void addHandle( final int x, final int y )
	{
		final double[] l = new double[]{ x, y };
		synchronized ( mesh )
		{
			final InvertibleCoordinateTransform ict = ( InvertibleCoordinateTransform )mesh.findClosest( l ).getModel();
			try
			{
				ict.applyInverseInPlace( l );
				final Point here = new Point( l );
				final Point there = new Point( l );
				hooks.add( here );
				here.apply( ict );
				mesh.addMatchWeightedByDistance( new PointMatch( there, here, 10f ), alpha );
			}
			catch ( final NoninvertibleModelException e ){ e.printStackTrace(); }
		}
	}

	@Override
	final protected void updateHandles( final int x, final int y )
	{
		final double[] l = hooks.get( targetIndex ).getW();

		l[ 0 ] = x;
		l[ 1 ] = y;
	}

	@Override
	final public void init()
	{
		final GenericDialog gd = new GenericDialog( "Moving Least Squares Transform" );
		gd.addNumericField( "Vertices_per_row :", numX, 0 );
		gd.addNumericField( "Alpha :", alpha, 2 );
		gd.addChoice( "Local_transformation :", methods, methods[ method ] );
		gd.addCheckbox( "_Interactive_preview", showPreview );
		gd.addMessage( man );
		gd.showDialog();

		if (gd.wasCanceled()) return;

		numX = ( int )gd.getNextNumber();
		alpha = ( float )gd.getNextNumber();

		method = gd.getNextChoiceIndex();

		showPreview = gd.getNextBoolean();

		switch ( method )
		{
		case 0:
			mesh = new MovingLeastSquaresMesh< TranslationModel2D >( TranslationModel2D.class, numX, imp.getWidth(), imp.getHeight() );
			break;
		case 1:
			mesh = new MovingLeastSquaresMesh< RigidModel2D >( RigidModel2D.class, numX, imp.getWidth(), imp.getHeight() );
			break;
		case 2:
			mesh = new MovingLeastSquaresMesh< SimilarityModel2D >( SimilarityModel2D.class, numX, imp.getWidth(), imp.getHeight() );
			break;
		case 3:
			mesh = new MovingLeastSquaresMesh< AffineModel2D >( AffineModel2D.class, numX, imp.getWidth(), imp.getHeight() );
			break;
		default:
			return;
		}
    }

	@Override
	final protected void updateIllustration()
	{
		if ( showIllustration )
			imp.setOverlay( mesh.illustrateMesh(), Color.white, null );
		else
			imp.setOverlay( null );
	}
}
