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
import ij.gui.*;

import mpicbg.ij.InteractiveMapping;
import mpicbg.ij.TransformMeshMapping;
import mpicbg.models.*;

import java.awt.Color;

/**
 * Smooth image deformation using landmark based deformation by means
 * of Moving Least Squares as described by \citet{SchaeferAl06} inspired by the
 * implementation of Johannes Schindelin.
 * 
 * BibTeX:
 * <pre>
 * @article{SchaeferAl06,
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
 */
public class Transform_MovingLeastSquaresMesh extends InteractiveMapping
{
	public static final String NL = System.getProperty( "line.separator" );
	public final static String man =
		"Add some control points with your mouse" + NL +
		"and drag them to deform the image." + NL + " " + NL +
		"ENTER - Apply the deformation." + NL +
		"ESC - Return to the original image." + NL +
		"Y - Toggle mesh display.";
	
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
	
	protected MovingLeastSquaresMesh< ? extends AbstractAffineModel2D > mesh;
	
	@Override
	final protected void createMapping()
	{
		mapping = new TransformMeshMapping( mesh );
	}
	
	@Override
	final protected void updateMapping() throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		mesh.updateModels();
		mesh.updateAffines();
		updateIllustration();
	}
	
	@Override
	final protected void addHandle( int x, int y )
	{
		float[] l = new float[]{ x, y };
		synchronized ( mesh )
		{
			InvertibleModel m = ( InvertibleModel )mesh.findClosest( l ).getModel();
			try
			{
				m.applyInverseInPlace( l );
				Point here = new Point( l );
				Point there = new Point( l );
				hooks.add( here );
				here.apply( m );
				mesh.addMatchWeightedByDistance( new PointMatch( there, here, 10f ), alpha );
			}
			catch ( NoninvertibleModelException e ){ e.printStackTrace(); }
		}	
	}
	
	@Override
	final protected void updateHandles( int x, int y )
	{
		float[] l = hooks.get( targetIndex ).getW();
	
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
			imp.getCanvas().setDisplayList( mesh.illustrateMesh(), Color.white, null );
		else
			imp.getCanvas().setDisplayList( null );
	}
}
