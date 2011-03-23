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
 */
package mpicbg.ij.integral;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.awt.Rectangle;

/**
 * 
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
final public class InteractiveScale extends AbstractInteractiveBlockFilter
{
	private IntegralImage integral;
	private ImageProcessor ipCopy;
	private boolean useIntegral = true;
	
	@Override
	final protected void init()
	{
		switch( imp.getType() )
		{
		case ImagePlus.GRAY32:
			integral = new DoubleIntegralImage( imp.getProcessor() );
			break;
		case ImagePlus.GRAY8:
		case ImagePlus.GRAY16:
			integral = new LongIntegralImage( imp.getProcessor() );
			break;
		case ImagePlus.COLOR_RGB:
			integral = new LongRGBIntegralImage( ( ColorProcessor )imp.getProcessor() );
			break;
		default:
			IJ.error( "Type not yet supported." );
			return;
		}
		
		ipCopy = imp.getProcessor().duplicate();
		
		final Roi roi;
		if ( imp.getRoi() != null )
			roi = imp.getRoi();
		else
		{
			roi = new Roi( imp.getWidth() / 4, imp.getHeight() / 4, imp.getWidth() / 2, imp.getHeight() / 2 );
			imp.setRoi( roi );
		}
	}
	
	@Override
	final protected void draw()
	{
		final ImageProcessor ip = imp.getProcessor();
		final int w = imp.getWidth() - 1;
		final int h = imp.getHeight() - 1;
		
		final Roi roi = imp.getRoi();
		final Rectangle rect;
		if ( roi != null )
			rect = roi.getBounds();
		else
			rect = new Rectangle( imp.getWidth(), imp.getHeight() );
		
		/* clear */
		for ( int i = imp.getWidth() * rect.y - 1; i >= 0; --i )
			ip.set( i, 0 );
		final int n = imp.getWidth() * imp.getHeight();
		for ( int i = imp.getWidth() * ( rect.y + rect.height ) - 1; i < n; ++i )
			ip.set( i, 0 );
		final int m = rect.y * imp.getWidth();
		final int l = m + rect.height * imp.getWidth();
		for ( int y = m; y < l; y += imp.getWidth() )
		{
			for ( int x = 0; x < rect.x; ++x )
				ip.set( y + x, 0 );
			for ( int x = rect.x + rect.width; x <= w; ++x )
				ip.set( y + x, 0 );
		}
		
		/* render */
		final double pixelWidth = ( double )imp.getWidth() / rect.width;
		final double pixelHeight = ( double )imp.getHeight() / rect.height;
		if ( useIntegral )
		{
			for ( int y = 0; y < rect.height; ++y )
			{
				final int yi = imp.getWidth() * Math.min( h, Math.max( 0, y + rect.y ) );
				final double yMinDouble = y * pixelHeight;
				final int yMin = Math.min( h, Math.max( -1, ( int )Math.round( yMinDouble ) - 1 ) );
				final int yMax = Math.max( -1, Math.min( h, ( int )Math.round( yMinDouble + pixelHeight - 1 ) ) );
				final int bh = yMax - yMin;
				for ( int x = 0; x < rect.width; ++x )
				{
					final int xi = Math.min( w, Math.max( 0, x + rect.x ) );
					final double xMinDouble = x * pixelWidth;
					final int xMin = Math.min( w, Math.max( -1, ( int )Math.round( xMinDouble ) - 1 ) );
					final int xMax = Math.min( w, Math.max( -1, ( int )Math.round( xMinDouble + pixelWidth - 1 ) ) );
					final float scale = 1.0f / ( xMax - xMin ) / bh;
					ip.set( yi + xi, integral.getScaledSum( xMin, yMin, xMax, yMax, scale ) );
				}
			}
		}
		else
		{
			for ( int y = 0; y < rect.height; ++y )
			{
				final int ys = imp.getWidth() * ( int )Math.round( y * pixelHeight );
				final int yi = imp.getWidth() * Math.min( h, Math.max( 0, y + rect.y ) );
				for ( int x = 0; x < rect.width; ++x )
				{
					final int xs = ( int )Math.round( x * pixelWidth );
					final int xi = Math.min( w, Math.max( 0, x + rect.x ) );
					ip.set( yi + xi, ipCopy.get( ys + xs ) );
				}
			}
		}
	}
	
	@Override
	final protected void showHelp()
	{
		IJ.showMessage(
				"Interactive Mean Smooth",
				"Click and drag to change the size of the smoothing kernel." + NL +
				"U - Toggle integral sampling" + NL +
				"ENTER - Apply" + NL +
				"ESC - Cancel" );
	}
}
