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
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de> and Pavel Tomancak <tomancak@mpi-cbg.de>
 * @version 0.1b
 */
import ij.plugin.*;
import ij.plugin.filter.GaussianBlur;
import ij.gui.*;
import ij.*;
import ij.process.*;

import java.awt.TextField;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * Transfer an image sequence into optic flow.
 * 
 */ 
public class Optic_Flow implements PlugIn, KeyListener
{
	static protected float sigma = 4;
	static protected byte maxDistance = 7;
	static protected boolean showColors = false;
	
	final static protected GaussianBlur filter = new GaussianBlur();
	
	/**
	 * Return an unsigned integer that bounces in a ping pong manner in the
	 * range [0 ... mod - 1]
	 *
	 * @param a the value to be flipped
	 * @param range the size of the range
	 * @return a flipped in range like a ping pong ball
	 */
	final static protected int pingPong( int a, final int mod )
	{
		final int p = 2 * mod;
		if ( a < 0 ) a = p + a % p;
		if ( a >= p ) a = a % p;
		if ( a >= mod ) a = mod - a % mod - 1;
		return a;
	}
	
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
	
	final static protected int colorVector( float xs, float ys )
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
		
		return ( ( ( int )( r * 255 ) << 8 ) + ( int )( g * 255 ) << 8 ) + ( int )( b * 255 );
	}
	
	final static protected void subtractShifted(
			final FloatProcessor a,
			final FloatProcessor b,
			final FloatProcessor c,
			final int xo,
			final int yo )
	{
		final float[] af = ( float[] )a.getPixels();
		final float[] bf = ( float[] )b.getPixels();
		final float[] cf = ( float[] )c.getPixels();
		
		final int w = a.getWidth();
		final int h = a.getHeight();
		
		for ( int y = 0; y < h; ++y )
		{
			int yb = y + yo;
			if ( yb < 0 || yb >= h )
				yb = pingPong( yb, h );
			final int yAdd = y * w;
			final int ybAdd = yb * w;
			
			for ( int x = 0; x < a.getWidth(); ++x )
			{
				int xb = x + xo;
				if ( xb < 0 || xb >= w )
					xb = pingPong( xb, w );
				
				final int i = yAdd + x;
				final float d = bf[ ybAdd + xb ] - af[ i ];
				cf[ i ] = d * d;
			}
		}
	}
	
	final static ImageProcessor createOpticFlow( final FloatProcessor ip1, final FloatProcessor ip2 )
	{
		final ImageProcessor of = new ColorProcessor( ip1.getWidth(), ip1.getHeight() );
		
		final ByteProcessor ipX = new ByteProcessor( ip1.getWidth(), ip1.getHeight() );
		final ByteProcessor ipY = new ByteProcessor( ip1.getWidth(), ip1.getHeight() );
		final FloatProcessor ipD = new FloatProcessor( ip1.getWidth(), ip1.getHeight() );
		final FloatProcessor ipDMin = new FloatProcessor( ip1.getWidth(), ip1.getHeight() );
		
		final float[] ipDMinInitPixels = ( float[] )ipDMin.getPixels();
		for ( int i = 0; i < ipDMinInitPixels.length; ++i )
			ipDMinInitPixels[ i ] = Float.MAX_VALUE;
		
		for ( byte yo = ( byte )-maxDistance; yo <= maxDistance; ++yo ) // HAHAHA!
		{
			for ( byte xo = ( byte )-maxDistance; xo <= maxDistance; ++xo )
			{
				// continue if radius is larger than maxDistance
				if ( yo * yo + xo * xo > maxDistance * maxDistance ) continue;
				
				subtractShifted( ip1, ip2, ipD, xo, yo );
				
				// blur in order to compare small regions instead of single pixels 
				filter.blurFloat( ipD, sigma, sigma, 0.002 );
				
				final float[] ipDPixels = ( float[] )ipD.getPixels();
				final float[] ipDMinPixels = ( float[] )ipDMin.getPixels();
				final byte[] ipXPixels = ( byte[] )ipX.getPixels();
				final byte[] ipYPixels = ( byte[] )ipY.getPixels();
				
				// update the translation fields
				for ( int i = 0; i < ipDPixels.length; ++i )
				{
					if ( ipDPixels[ i ] < ipDMinPixels[ i ] )
					{
						ipDMinPixels[ i ] = ipDPixels[ i ];
						ipXPixels[ i ] = xo;
						ipYPixels[ i ] = yo;
					}
				}
			}
		}
		final byte[] ipXPixels = ( byte[] )ipX.getPixels();
		final byte[] ipYPixels = ( byte[] )ipY.getPixels();
		final int[] ipOfPixels = ( int[] )of.getPixels();
		
		for ( int i = 0; i < ipOfPixels.length; ++i )
			ipOfPixels[ i ] = colorVector( ipXPixels[ i ], ipYPixels[ i ] );
		
		return of;
	}
	
	final public void run( final String args )
	{
		if ( IJ.versionLessThan( "1.41n" ) ) return;

		final ImagePlus imp = WindowManager.getCurrentImage();
		if ( imp == null )  { IJ.error( "There are no images open" ); return; }
		
		GenericDialog gd = new GenericDialog( "Generate optic flow" );
		gd.addNumericField( "sigma :", sigma, 2, 6, "px" );
		gd.addNumericField( "maximal_distance :", maxDistance, 0, 6, "px" );
		gd.addCheckbox( "show_color_map", showColors );
		
		gd.showDialog();
		
		if (gd.wasCanceled()) return;
		
		sigma = ( float )gd.getNextNumber();
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
		ImageStack seqOpticFlow = new ImageStack( seq.getWidth(), seq.getHeight() );
		
		FloatProcessor ip1;
		FloatProcessor ip2 = ( FloatProcessor )seq.getProcessor( 1 ).convertToFloat();
		
		ImagePlus impOpticFlow = null;
		
		for ( int i = 1; i < seq.getSize(); ++i )
		{
			ip1 = ip2;
			ip2 = ( FloatProcessor )seq.getProcessor( i + 1 ).convertToFloat();
			
			IJ.log( "Processing slice " + i );
			
			seqOpticFlow.addSlice( "" + i, createOpticFlow( ip1, ip2 ) );
			
			if ( seqOpticFlow.getSize() == 1 )
			{
				impOpticFlow = new ImagePlus( imp.getTitle() + " optic flow", seqOpticFlow );
				impOpticFlow.show();
			}
			else
				impOpticFlow.setStack( null, seqOpticFlow );
			
			IJ.showProgress( i, seq.getSize() );
			impOpticFlow.setSlice( i );
			imp.setSlice( i + 1 );
		}
	}

	public void keyPressed(KeyEvent e)
	{
		if (
				( e.getKeyCode() == KeyEvent.VK_F1 ) &&
				( e.getSource() instanceof TextField ) )
		{
		}
	}

	public void keyReleased(KeyEvent e) { }

	public void keyTyped(KeyEvent e) { }
}
