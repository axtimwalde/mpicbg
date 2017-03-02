package mpicbg.ij.plugin;
import java.util.ArrayList;

import ij.CompositeImage;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.io.Opener;
import ij.plugin.PlugIn;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import mpicbg.ij.InverseTransformMapping;
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
 */
import mpicbg.ij.integral.BlockPMCC;
import mpicbg.ij.integral.IntegralImage;
import mpicbg.models.InverseCoordinateTransformMap2D;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import mpicbg.models.TranslationModel2D;

/**
 * <h1>Transfer an image sequence into an optic flow field<h1>
 *
 * <p>Flow fields are calculated for each pair <em>(t,t+1)</em> of the sequence
 * independently.  The motion vector for each pixel in image t is estimated by
 * searching the most similar looking pixel in image <em>t+1</em>.  The
 * similarity measure is Pearson Product-Moment Correlation Coefficient of all
 * pixels in a local vicinity.  The local vicinity is defined by a block and is
 * calculated using an {@link IntegralImage}.  Both the size of the block and
 * the search radius are parameters of the method.</p>
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1a
 */
public class PMCCScaleSpaceBlockFlow implements PlugIn
{
	static protected int blockRadius = 8;
	static protected int maxDistance = 7;
	static protected boolean showColors = false;

	final static protected void algebraicToColor(
			final short[] ipXPixels,
			final short[] ipYPixels,
			final int[] ipColorPixels,
			final double max )
	{
		final int n = ipXPixels.length;
		for ( int i = 0; i < n; ++i )
		{
			final double x = ipXPixels[ i ] / max;
			final double y = ipYPixels[ i ] / max;

			final double r = Math.sqrt( x * x + y * y );
			final double phi = Math.atan2( x / r, y / r );

			if ( r == 0.0 )
				ipColorPixels[ i ] = 0;
			else
			{
				final double red, green, blue;

				double o = ( phi + Math.PI ) / Math.PI * 3;

				if ( o < 3 )
					red = Math.min( 1.0, Math.max( 0.0, 2.0 - o ) ) * r;
				else
					red = Math.min( 1.0, Math.max( 0.0, o - 4.0 ) ) * r;

				o += 2;
				if ( o >= 6 ) o -= 6;

				if ( o < 3 )
					green = Math.min( 1.0, Math.max( 0.0, 2.0 - o ) ) * r;
				else
					green = Math.min( 1.0, Math.max( 0.0, o - 4.0 ) ) * r;

				o += 2;
				if ( o >= 6 ) o -= 6;

				if ( o < 3 )
					blue = Math.min( 1.0, Math.max( 0.0, 2.0 - o ) ) * r;
				else
					blue = Math.min( 1.0, Math.max( 0.0, o - 4.0 ) ) * r;

				ipColorPixels[ i ] =  ( ( ( ( int )( red * 255 ) << 8 ) | ( int )( green * 255 ) ) << 8 ) | ( int )( blue * 255 );
			}
		}
	}

	static public void opticFlow(
			final FloatProcessor ip1,
			final FloatProcessor ip2,
			final ImageStack r,
			final ImageStack shiftVectors,
			final ImageStack of,
			final double scaleFactor )
	{
		final BlockPMCC bc = new BlockPMCC( ip1.getWidth(), ip1.getHeight(), ip1, ip2 );
		//final BlockPMCC bc = new BlockPMCC( ip1, ip2 );

		final FloatProcessor ipR = bc.getTargetProcessor();
		final float[] ipRPixels = ( float[] )ipR.getPixels();

		final ArrayList< Double > radiusList = new ArrayList< Double >();

		for ( double radius = 1; radius < r.getWidth() / 4; radius *= scaleFactor )
		{
			radiusList.add( radius );

			final FloatProcessor ipRMax = new FloatProcessor( ipR.getWidth(), ipR.getHeight() );
			final float[] ipRMaxPixels = ( float[] )ipRMax.getPixels();
			{
				for ( int i = 0; i < ipRMaxPixels.length; ++i )
					ipRMaxPixels[ i ] = -1;
			}
			final ShortProcessor ipX = new ShortProcessor( ipR.getWidth(), ipR.getHeight() );
			final ShortProcessor ipY = new ShortProcessor( ipR.getWidth(), ipR.getHeight() );
			final ColorProcessor cp = new ColorProcessor( ipR.getWidth(), ipR.getHeight() );

			r.addSlice( ipRMax );
			shiftVectors.addSlice( ipX );
			shiftVectors.addSlice( ipY );
			of.addSlice( cp );
		}

		/* assemble into typed arrays for quicker access */
		final float[][] rArrays = new float[ r.getSize() ][];
		final short[][] xShiftArrays = new short[ rArrays.length ][];
		final short[][] yShiftArrays = new short[ rArrays.length ][];
		final int[][] ofArrays = new int[ rArrays.length ][];
		final int[] radii = new int[ rArrays.length ];
		for ( int i = 0; i < radii.length; ++i )
		{
			rArrays[ i ] = ( float[] )r.getImageArray()[ i ];
			xShiftArrays[ i ] = ( short[] )shiftVectors.getImageArray()[ i << 1 ];
			yShiftArrays[ i ] = ( short[] )shiftVectors.getImageArray()[ ( i << 1 ) | 1 ];
			ofArrays[ i ] = ( int[] )of.getImageArray()[ i ];

			radii[ i ] = ( int )Math.round( radiusList.get( i ) );
		}

		for ( int yo = -maxDistance; yo <= maxDistance; ++yo )
		{
			for ( int xo = -maxDistance; xo <= maxDistance; ++xo )
			{
				// continue if radius is larger than maxDistance
				if ( yo * yo + xo * xo > maxDistance * maxDistance ) continue;

				IJ.log( String.format( "(%d, %d)", xo, yo ) );

				bc.setOffset( xo, yo );

				for ( int ri = 0; ri < radii.length; ++ri )
				{
					final int blockRadius = radii[ ri ];

					bc.rSignedSquare( blockRadius );

					final float[] ipRMaxPixels = rArrays[ ri ];
					final short[] ipXPixels = xShiftArrays[ ri ];
					final short[] ipYPixels = yShiftArrays[ ri ];

					// update the translation fields
					final int h = ipR.getHeight() - maxDistance;
					final int width = ipR.getWidth();
					final int w = width - maxDistance;
					for ( int y = maxDistance; y < h; ++y )
					{
						final int row = y * width;
						final int rowR;
						if ( yo < 0 )
							rowR = row;
						else
							rowR = ( y - yo ) * width;
						for ( int x = maxDistance; x < w; ++x )
						{
							final int i = row + x;
							final int iR;
							if ( xo < 0 )
								iR = rowR + x;
							else
								iR = rowR + ( x - xo );

							final float ipRPixel = ipRPixels[ iR ];
							final float ipRMaxPixel = ipRMaxPixels[ i ];

							if ( ipRPixel > ipRMaxPixel )
							{
								ipRMaxPixels[ i ] = ipRPixel;
								ipXPixels[ i ] = ( short )xo;
								ipYPixels[ i ] = ( short )yo;
							}
						}
					}
				}
			}
		}

		for ( int i = 0; i < radii.length; ++i )
			algebraicToColor(
					xShiftArrays[ i ],
					yShiftArrays[ i ],
					ofArrays[ i ],
					maxDistance );
	}


	public final static void filterOpticFlowScaleSpace(
			final ImageStack shiftVectors,
			final ShortProcessor shiftX,
			final ShortProcessor shiftY,
			final ColorProcessor of,
			final ShortProcessor inlierCounts ) throws NotEnoughDataPointsException
	{
		/* assemble into typed arrays for quicker access */
		/* TODO This is still inefficient because scale dimension is fastest but should be slowest
		 * this is true here and in opticFlow, i.e. scale should be interleaved (size of array
		 * becomes concern, use ImgLib2).
		 */
		final short[][] xShiftArrays = new short[ shiftVectors.size() / 2 ][];
		final short[][] yShiftArrays = new short[ xShiftArrays.length ][];
		for ( int i = 0; i < xShiftArrays.length; ++i )
		{
			xShiftArrays[ i ] = ( short[] )shiftVectors.getImageArray()[ i << 1 ];
			yShiftArrays[ i ] = ( short[] )shiftVectors.getImageArray()[ ( i << 1 ) | 1 ];
		}

		final int n = shiftVectors.getWidth() * shiftVectors.getHeight();
		final int m = xShiftArrays.length;

		final ArrayList< PointMatch > pq = new ArrayList< PointMatch >();
		for ( int i = 0; i < m; ++i )
			pq.add(
					new PointMatch(
							new Point( new double[]{ 0.0, 0.0 } ),
							new Point( new double[]{ 0.0, 0.0 } ) ) );

		final TranslationModel2D model = new TranslationModel2D();
		final ArrayList< PointMatch > pqInliers = new ArrayList< PointMatch >();
		final double[] translation = new double[ 6 ];

		for ( int i = 0; i < n; ++i )
		{
			for ( int j = 0; j < m; ++j )
			{
				final double[] q = pq.get( j ).getP2().getW();
				q[ 0 ] = xShiftArrays[ j ][ i ];
				q[ 1 ] = yShiftArrays[ j ][ i ];
			}
			model.ransac( pq, pqInliers, 1000, 0.5, 0 );
			model.toArray( translation );
			shiftX.set( i, ( short )Math.round( translation[ 4 ] * 4 ) );
			shiftY.set( i, ( short )Math.round( translation[ 5 ] * 4 ) );

			inlierCounts.set( i, pqInliers.size() );

			if ( i / inlierCounts.getWidth() * inlierCounts.getWidth() == i )
				IJ.log( "row " + i / inlierCounts.getWidth() );

		}

		algebraicToColor(
				( short[] )shiftX.getPixels(),
				( short[] )shiftY.getPixels(),
				(int[] )of.getPixels(),
				maxDistance * 4 );

		new ImagePlus( "ransacced", of ).show();
		new ImagePlus( "inlierCounts", inlierCounts ).show();
	}

	final ImageProcessor map(
			final ImageProcessor source,
			final ShortProcessor shiftX,
			final ShortProcessor shiftY )
	{
		final float[][] mapField = new float[ shiftX.getHeight() ][ shiftX.getWidth() * 2 ];
		final short[] shiftXPixels = ( short[] )shiftX.getPixels();
		final short[] shiftYPixels = ( short[] )shiftY.getPixels();
		for ( int y = 0; y < shiftX.getHeight(); ++y )
		{
			final int offset = y * shiftX.getWidth();
			for ( int x = 0; x < shiftX.getWidth(); ++x )
			{
				mapField[ y ][ 2 * x ] = shiftXPixels[ offset + x ] + x;
				mapField[ y ][ 2 * x + 1 ] = shiftYPixels[ offset + x ] + y;
			}
		}


		final InverseCoordinateTransformMap2D map = new InverseCoordinateTransformMap2D( mapField );
		final InverseTransformMapping< InverseCoordinateTransformMap2D > mapping = new InverseTransformMapping< InverseCoordinateTransformMap2D >( map );
		final ImageProcessor target = source.createProcessor( source.getWidth(), source.getHeight() );
		source.setInterpolationMethod( ImageProcessor.BILINEAR );
		mapping.mapInterpolated( source, target );

		return target;
	}



	@Override
	final public void run( final String args )
	{
		if ( IJ.versionLessThan( "1.41n" ) ) return;

		final ImagePlus imp = WindowManager.getCurrentImage();
		if ( imp == null )  { IJ.error( "There are no images open" ); return; }

		final GenericDialog gd = new GenericDialog( "Generate optic flow" );
		gd.addNumericField( "maximal_distance :", maxDistance, 0, 6, "px" );
		gd.addCheckbox( "show_color_map", showColors );

		gd.showDialog();

		if (gd.wasCanceled()) return;

		maxDistance = ( int )gd.getNextNumber();
		showColors = gd.getNextBoolean();

		if ( showColors )
		{
			final ColorProcessor ipColor = new ColorProcessor( maxDistance * 2 + 1, maxDistance * 2 + 1 );
			PMCCBlockFlow.colorCircle( ipColor, maxDistance );
			final ImagePlus impColor = new ImagePlus( "Color", ipColor );
			impColor.show();
		}


		final ImageStack seq = imp.getStack();
		final ImageStack seqR = new ImageStack( imp.getWidth(), imp.getHeight() );
		final ImageStack seqOpticFlow = new ImageStack( imp.getWidth(), imp.getHeight() );
		final ImageStack seqFlowVectors = new ImageStack( imp.getWidth(), imp.getHeight() );

		FloatProcessor ip1;
		FloatProcessor ip2 = ( FloatProcessor )seq.getProcessor( 1 ).convertToFloat();

		ImagePlus impR = null;
		ImagePlus impOpticFlow = null;
		CompositeImage impFlowVectors = null;

		for ( int i = 1; i < seq.getSize(); ++i )
		{
			ip1 = ip2;
			ip2 = ( FloatProcessor )seq.getProcessor( i + 1 ).convertToFloat();

			IJ.log( "Processing slice " + i );

			opticFlow(
					ip1,
					ip2,
					seqR,
					seqFlowVectors,
					seqOpticFlow,
					1.1 );

			final ImageStack mappedImages = new ImageStack( imp.getWidth(), imp.getHeight() );
			for ( int s = 0; s < seqFlowVectors.size(); s += 2 )
			{
				final ImageProcessor target =
						map(
								ip2,
								seqFlowVectors.getProcessor( s + 1 ).convertToShortProcessor(),
								seqFlowVectors.getProcessor( s + 2 ).convertToShortProcessor() );
				mappedImages.addSlice( target );
			}
			new ImagePlus( "", mappedImages ).show();


			final ShortProcessor shiftX = new ShortProcessor( imp.getWidth(), imp.getHeight() );
			final ShortProcessor shiftY = new ShortProcessor( imp.getWidth(), imp.getHeight() );
			final ColorProcessor of = new ColorProcessor( imp.getWidth(), imp.getHeight() );
			final ShortProcessor inlierCounts = new ShortProcessor( imp.getWidth(), imp.getHeight() );
			try
			{
				filterOpticFlowScaleSpace(
						seqFlowVectors,
						shiftX,
						shiftY,
						of,
						inlierCounts );

				map( ip1, shiftX, shiftY );
			}
			catch ( final NotEnoughDataPointsException e )
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if ( i == 1 )
			{
				impR = new ImagePlus( imp.getTitle() + " R^2", seqR );
				impR.setOpenAsHyperStack( true );
				impR.setCalibration( imp.getCalibration() );
				impR.setDimensions( 1, 1, seq.getSize() - 1 );
				impR.show();

				impOpticFlow = new ImagePlus( imp.getTitle() + " optic flow", seqOpticFlow );
				impOpticFlow.setOpenAsHyperStack( true );
				impOpticFlow.setCalibration( imp.getCalibration() );
				impOpticFlow.setDimensions( 1, 1, seq.getSize() - 1 );
				impOpticFlow.show();

				final ImagePlus notYetComposite = new ImagePlus( imp.getTitle() + " flow vectors", seqFlowVectors );
				notYetComposite.setOpenAsHyperStack( true );
				notYetComposite.setCalibration( imp.getCalibration() );
				notYetComposite.setDimensions( 2, 1, seq.getSize() - 1 );

				impFlowVectors = new CompositeImage( notYetComposite, CompositeImage.GRAYSCALE );
				impFlowVectors.setOpenAsHyperStack( true );
				impFlowVectors.setDimensions( 2, 1, seq.getSize() - 1 );
				impFlowVectors.show();

				impFlowVectors.setPosition( 1, 1, 1 );
				impFlowVectors.setDisplayRange( 0, 1 );
				impFlowVectors.setPosition( 2, 1, 1 );
				impFlowVectors.setDisplayRange( -Math.PI, Math.PI );
			}

			IJ.showProgress( i, seq.getSize() );
			impR.setSlice( i );
			impOpticFlow.setSlice( i );
			impFlowVectors.setPosition( 1, 1, i );
			imp.setSlice( i + 1 );
		}
	}

	public final static void main( final String... args )
	{
		new ImageJ();
		final ImagePlus imp = new Opener().openImage( "/home/saalfeld/tmp/scheffer/flow/stack.0.05.tif" );
		imp.show();
		new PMCCScaleSpaceBlockFlow().run("");
	}
}
