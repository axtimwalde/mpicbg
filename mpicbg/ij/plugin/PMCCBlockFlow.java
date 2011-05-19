package mpicbg.ij.plugin;
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
import ij.plugin.*;
import ij.gui.*;
import ij.*;
import ij.process.*;

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
public class PMCCBlockFlow implements PlugIn
{
	static protected int blockRadius = 8;
	static protected byte maxDistance = 7;
	static protected boolean showColors = false;
	
	final static protected void colorCircle( ColorProcessor ip )
	{
		final int r1 = Math.min( ip.getWidth(), ip.getHeight() ) / 2;
		final int r2 = r1 / 2;
		
		for ( int y = 0; y < ip.getHeight(); ++y )
		{
			final float dy = y - ip.getHeight() / 2;
			for ( int x = 0; x < ip.getWidth(); ++x )
			{
				final float dx = x - ip.getWidth() / 2;
				final float l = ( float )Math.sqrt( dx * dx + dy * dy );
				
				if ( l > r1 || l < r2 )
					ip.putPixel( x, y, 0 );
				else
					ip.putPixel( x, y, colorVector( dx / l * maxDistance, dy / l * maxDistance ) );
			}
		}
	}
	
	final static private void algebraicToPolarAndColor(
			final int[] ipXPixels,
			final int[] ipYPixels,
			final float[] ipRPixels,
			final float[] ipPhiPixels,
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
			
			ipRPixels[ i ] = ( float )r;
			ipPhiPixels[ i ] = ( float )phi;
			
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
	
	final static private int colorVector( float xs, float ys )
	{
		xs /= maxDistance;
		ys /= maxDistance;
		final double a = Math.sqrt( xs * xs + ys * ys );
		if ( a == 0.0 ) return 0;
		
		double o = ( Math.atan2( xs / a, ys / a ) + Math.PI ) / Math.PI * 3;
		
		final double r, g, b;
		
		if ( o < 3 )
			r = Math.min( 1.0, Math.max( 0.0, 2.0 - o ) ) * a;
		else
			r = Math.min( 1.0, Math.max( 0.0, o - 4.0 ) ) * a;
		
		o += 2;
		if ( o >= 6 ) o -= 6;
		
		if ( o < 3 )
			g = Math.min( 1.0, Math.max( 0.0, 2.0 - o ) ) * a;
		else
			g = Math.min( 1.0, Math.max( 0.0, o - 4.0 ) ) * a;
		
		o += 2;
		if ( o >= 6 ) o -= 6;
		
		if ( o < 3 )
			b = Math.min( 1.0, Math.max( 0.0, 2.0 - o ) ) * a;
		else
			b = Math.min( 1.0, Math.max( 0.0, o - 4.0 ) ) * a;
		
		return ( ( ( ( int )( r * 255 ) << 8 ) | ( int )( g * 255 ) ) << 8 ) | ( int )( b * 255 );
	}
	
	
	final static private void opticFlow(
			final FloatProcessor ip1,
			final FloatProcessor ip2,
			final FloatProcessor r,
			final FloatProcessor amp,
			final FloatProcessor phi,
			final ColorProcessor of )
	{
		final BlockPMCC bc = new BlockPMCC( ip1.getWidth(), ip1.getHeight(), ip1, ip2 );
		//final BlockPMCC bc = new BlockPMCC( ip1, ip2 );
		
		final FloatProcessor ipR = bc.getTargetProcessor();
		final ColorProcessor ipX = new ColorProcessor( ipR.getWidth(), ipR.getHeight() );
		final ColorProcessor ipY = new ColorProcessor( ipR.getWidth(), ipR.getHeight() );
		
		/* init */
		{
			final float[] ipRMaxPixels = ( float[] )r.getPixels();
			for ( int i = 0; i < ipRMaxPixels.length; ++i )
				ipRMaxPixels[ i ] = -1;
		}
		
//		final ImageStack stack = new ImageStack( ipR.getWidth(), ipR.getHeight() );
		
		for ( int yo = -maxDistance; yo <= maxDistance; ++yo )
		{
			for ( int xo = -maxDistance; xo <= maxDistance; ++xo )
			{
				// continue if radius is larger than maxDistance
				if ( yo * yo + xo * xo > maxDistance * maxDistance ) continue;
				
				bc.setOffset( xo, yo );
				bc.rSignedSquare( blockRadius );
				
//				stack.addSlice( xo + " " + yo, ipR.duplicate() );
				
				final float[] ipRPixels = ( float[] )ipR.getPixels();
				final float[] ipRMaxPixels = ( float[] )r.getPixels();
				final int[] ipXPixels = ( int[] )ipX.getPixels();
				final int[] ipYPixels = ( int[] )ipY.getPixels();
				
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
							ipXPixels[ i ] = xo;
							ipYPixels[ i ] = yo;
						}
					}
				}
			}
		}
//		new ImagePlus( "rs", stack ).show();
		
		algebraicToPolarAndColor(
				( int[] )ipX.getPixels(),
				( int[] )ipY.getPixels(),
				( float[] )amp.getPixels(),
				( float[] )phi.getPixels(),
				( int[] )of.getPixels(),
				maxDistance );
	}
	
	final public void run( final String args )
	{
		if ( IJ.versionLessThan( "1.41n" ) ) return;

		final ImagePlus imp = WindowManager.getCurrentImage();
		if ( imp == null )  { IJ.error( "There are no images open" ); return; }
		
		GenericDialog gd = new GenericDialog( "Generate optic flow" );
		gd.addNumericField( "block radius :", blockRadius, 0, 6, "px" );
		gd.addNumericField( "maximal_distance :", maxDistance, 0, 6, "px" );
		gd.addCheckbox( "show_color_map", showColors );
		
		gd.showDialog();
		
		if (gd.wasCanceled()) return;
		
		blockRadius = ( int )gd.getNextNumber();
		maxDistance = ( byte )gd.getNextNumber();
		showColors = gd.getNextBoolean();
		
		if ( showColors )
		{
			ColorProcessor ipColor = new ColorProcessor( imp.getWidth(), imp.getHeight() );
			colorCircle( ipColor );
			ImagePlus impColor = new ImagePlus( "Color", ipColor );
			impColor.show();
		}
		
		
		ImageStack seq = imp.getStack();
		ImageStack seqR = new ImageStack( imp.getWidth(), imp.getHeight(), seq.getSize() - 1 );
		ImageStack seqOpticFlow = new ImageStack( imp.getWidth(), imp.getHeight(), seq.getSize() - 1 );
		ImageStack seqFlowVectors = new ImageStack( imp.getWidth(), imp.getHeight(), 2 * seq.getSize() - 2 );
		
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
			
			final FloatProcessor seqFlowVectorRSlice = new FloatProcessor( imp.getWidth(), imp.getHeight() );
			final FloatProcessor seqFlowVectorPhiSlice = new FloatProcessor( imp.getWidth(), imp.getHeight() );
			final FloatProcessor seqRSlice = new FloatProcessor( imp.getWidth(), imp.getHeight() );
			final ColorProcessor seqOpticFlowSlice = new ColorProcessor( imp.getWidth(), imp.getHeight() );
			
			opticFlow( ip1, ip2, seqRSlice, seqFlowVectorRSlice, seqFlowVectorPhiSlice, seqOpticFlowSlice );
			
			seqR.setPixels( seqRSlice.getPixels(), i );
			seqR.setSliceLabel( "" + i, i );
			seqFlowVectors.setPixels( seqFlowVectorRSlice.getPixels(), 2 * i - 1 );
			seqFlowVectors.setSliceLabel( "r " + i, 2 * i - 1 );
			seqFlowVectors.setPixels( seqFlowVectorPhiSlice.getPixels(), 2 * i );
			seqFlowVectors.setSliceLabel( "phi " + i, 2 * i );
			seqOpticFlow.setPixels( seqOpticFlowSlice.getPixels(), i );
			seqOpticFlow.setSliceLabel( "" + i, i );
			
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
}
