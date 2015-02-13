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
package mpicbg.ij.visualization;

import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;

import mpicbg.imagefeatures.Feature;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import mpicbg.util.Util;

/**
 *
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1a
 */
public class PointVis
{
	final static public void drawLocalPoints(
			final ImageProcessor ip,
			final Collection< ? extends Point > points,
			final Color color,
			final int width,
			final Rectangle srcRect,
			final double magnification )
	{
		final int oldLineWidth = ip.getLineWidth();

		ip.setColor( color );
		ip.setLineWidth( width );

		for ( final Point p : points )
		{
			final double[] l = p.getL();
			final double x = ( l[ 0 ] + srcRect.x ) * magnification;
			final double y = ( l[ 1 ] + srcRect.y ) * magnification;

			ip.drawDot( Util.round( x ), Util.round( y ) );
		}

		ip.setLineWidth( oldLineWidth );
	}

	final static public void drawLocalPoints(
			final ImageProcessor ip,
			final Collection< ? extends Point > points,
			final Color color,
			final int width )
	{
		drawLocalPoints( ip, points, color, width, new Rectangle( 0, 0, ip.getWidth(), ip.getHeight() ), 1.0 );
	}



	final static public void drawWorldPoints(
			final ImageProcessor ip,
			final Collection< ? extends Point > points,
			final Color color,
			final int width,
			final Rectangle srcRect,
			final double magnification )
	{
		final int oldLineWidth = ip.getLineWidth();

		ip.setColor( color );
		ip.setLineWidth( width );

		for ( final Point p : points )
		{
			final double[] w = p.getW();
			final double x = ( w[ 0 ] + srcRect.x ) * magnification;
			final double y = ( w[ 1 ] + srcRect.y ) * magnification;

			ip.drawDot( Util.round( x ), Util.round( y ) );
		}

		ip.setLineWidth( oldLineWidth );
	}

	final static public void drawWorldPoints(
			final ImageProcessor ip,
			final Collection< ? extends Point > points,
			final Color color,
			final int width )
	{
		drawWorldPoints( ip, points, color, width, new Rectangle( 0, 0, ip.getWidth(), ip.getHeight() ), 1.0 );
	}


	final static public void drawLocalPointMatchLines(
			final ImageProcessor ip,
			final Collection< ? extends PointMatch > pointMatches,
			final Color color,
			final int width,
			final Rectangle srcRect,
			final Rectangle dstRect,
			final double srcMagnification,
			final double dstMagnification )
	{
		final int oldLineWidth = ip.getLineWidth();

		ip.setColor( color );
		ip.setLineWidth( width );

		for ( final PointMatch pm : pointMatches )
		{
			final double[] l1 = pm.getP1().getL();
			final double x1 = ( l1[ 0 ] + srcRect.x ) * srcMagnification;
			final double y1 = ( l1[ 1 ] + srcRect.y ) * srcMagnification;

			final double[] l2 = pm.getP2().getL();
			final double x2 = ( l2[ 0 ] + dstRect.x ) * dstMagnification;
			final double y2 = ( l2[ 1 ] + dstRect.y ) * dstMagnification;

			ip.drawLine( Util.round( x1 ), Util.round( y1 ), Util.round( x2 ), Util.round( y2 ) );
		}

		ip.setLineWidth( oldLineWidth );
	}

	final static public void drawWorldPointMatchLines(
			final ImageProcessor ip,
			final Collection< ? extends PointMatch > pointMatches,
			final Color color,
			final int width,
			final Rectangle srcRect,
			final Rectangle dstRect,
			final double srcMagnification,
			final double dstMagnification )
	{
		final int oldLineWidth = ip.getLineWidth();

		ip.setColor( color );
		ip.setLineWidth( width );

		for ( final PointMatch pm : pointMatches )
		{
			final double[] w1 = pm.getP1().getW();
			final double x1 = ( w1[ 0 ] + srcRect.x ) * srcMagnification;
			final double y1 = ( w1[ 1 ] + srcRect.y ) * srcMagnification;

			final double[] w2 = pm.getP2().getW();
			final double x2 = ( w2[ 0 ] + dstRect.x ) * dstMagnification;
			final double y2 = ( w2[ 1 ] + dstRect.y ) * dstMagnification;

			ip.drawLine( Util.round( x1 ), Util.round( y1 ), Util.round( x2 ), Util.round( y2 ) );
		}

		ip.setLineWidth( oldLineWidth );
	}


	final static public void drawEpsilonCoordinates(
			final ImageProcessor ip,
			final Color color,
			final int width )
	{
		final int oldLineWidth = ip.getLineWidth();

		ip.setColor( color );
		ip.setLineWidth( width );

		final int xCenter = ip.getWidth() / 2;
		final int yCenter = ip.getHeight() / 2;

		final int arrowLength = 6;
		final int arrowRadius = 2;

		final int maxX = Util.round( 0.1 * ip.getWidth() / 2 / 1.1 );
		final int maxY = Util.round( 0.1 * ip.getHeight() / 2 / 1.1 );

		ip.drawLine( 0, yCenter, ip.getWidth() - 1, yCenter );
		ip.drawLine( xCenter, 0, xCenter, ip.getHeight() );

		for ( int i = -arrowRadius * width; i <= arrowRadius * width; ++i )
		{
			ip.drawLine( 0, yCenter, arrowLength * width, yCenter + i );
			ip.drawLine( ip.getWidth() - 1, yCenter, ip.getWidth() - 1 - arrowLength * width, yCenter + i );
			ip.drawLine( xCenter, 0, xCenter + i, arrowLength * width );
			ip.drawLine( xCenter, ip.getHeight() - 1, xCenter + i, ip.getHeight() - 1 - arrowLength * width );
		}

		ip.drawLine( maxX, yCenter + 2, maxX, yCenter );
		ip.drawLine( ip.getWidth() - 1 - maxX, yCenter + 2, ip.getWidth() - 1 - maxX, yCenter );

		ip.drawLine( xCenter - 2, maxY, xCenter, maxY );
		ip.drawLine( xCenter - 2, ip.getHeight() - 1 - maxY, xCenter, ip.getHeight() - 1 - maxY );

		ip.setAntialiasedText( true );
		ip.setFont( new Font( "Sans", Font.PLAIN, 14 ) );

		ip.drawString( "-\u03B5", maxX - 6, yCenter + 20 );
		ip.drawString( "\u03B5", ip.getWidth() - 1 - maxX - 3, yCenter + 20 );

		ip.drawString( "\u03B5", xCenter - 14, maxY + 6 );
		ip.drawString( "-\u03B5", xCenter - 19, ip.getHeight() - 1 - maxY + 6 );


		ip.setLineWidth( oldLineWidth );
	}

	public static void drawFeaturePoints(
			final ImageProcessor ip,
			final Iterable< ? extends Feature > features,
			final Color color,
			final int width,
			final Rectangle srcRect,
			final double magnification )
	{
		final ArrayList< Point > points = new ArrayList< Point >();
		for ( final Feature f : features )
			points.add( new Point( f.location.clone() ) );

		drawLocalPoints( ip, points, color, width, srcRect, magnification );
	}

	final static public void drawFeaturePoints(
			final ImageProcessor ip,
			final Collection< ? extends Feature > features,
			final Color color,
			final int width )
	{
		drawFeaturePoints( ip, features, color, width, new Rectangle( 0, 0, ip.getWidth(), ip.getHeight() ), 1.0 );
	}

}
