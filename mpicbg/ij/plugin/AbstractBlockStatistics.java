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
 */


import ij.IJ;
import ij.ImagePlus;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.AWTEvent;

/**
 * Abstract base class for variance and STD filters.
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1a
 */
abstract public class AbstractBlockStatistics implements ExtendedPlugInFilter, DialogListener
{
	static protected int blockRadiusX = 40, blockRadiusY = 40;
	final static protected int flags = DOES_8G | DOES_16 | DOES_32 | DOES_RGB;
	
	protected mpicbg.ij.integral.BlockStatistics[] bss;
	protected FloatProcessor[] fps;
	protected ImageProcessor pip = null;
	
	abstract protected String dialogTitle();
	abstract protected void process( int i, int brx, int bry );
	
	protected void init( final ImagePlus imp )
	{
		pip = imp.getProcessor();
		if ( imp.getType() == ImagePlus.COLOR_RGB )
		{
			final int[] rgbs = ( int[] )pip.getPixels();
			final float[] rs = new float[ rgbs.length ];
			final float[] gs = new float[ rgbs.length ];
			final float[] bs = new float[ rgbs.length ];
			for ( int i = 0; i < rgbs.length; ++i )
			{
				final int rgb = rgbs[ i ];
				rs[ i ] = ( rgb >> 16 ) & 0xff;
				gs[ i ] = ( rgb >> 8 ) & 0xff;
				bs[ i ] = rgb & 0xff;
			}
			fps = new FloatProcessor[]{
					new FloatProcessor( imp.getWidth(), imp.getHeight(), rs, null ),
					new FloatProcessor( imp.getWidth(), imp.getHeight(), gs, null ),
					new FloatProcessor( imp.getWidth(), imp.getHeight(), bs, null ) };
			bss = new mpicbg.ij.integral.BlockStatistics[]{
					new mpicbg.ij.integral.BlockStatistics( fps[ 0 ] ),
					new mpicbg.ij.integral.BlockStatistics( fps[ 1 ] ),
					new mpicbg.ij.integral.BlockStatistics( fps[ 2 ] ) };
		}
		else
		{
			if ( imp.getType() == ImagePlus.GRAY32 )
				fps = new FloatProcessor[]{ ( FloatProcessor )pip };
			else
				fps = new FloatProcessor[]{ ( FloatProcessor )pip.convertToFloat() };
			
			bss = new mpicbg.ij.integral.BlockStatistics[]{ new mpicbg.ij.integral.BlockStatistics( fps[ 0 ] ) };
		}
	}
	
	protected void toRGB( final int[] rgbs )
	{
		final float[] rs = ( float[] )fps[ 0 ].getPixels();
		final float[] gs = ( float[] )fps[ 1 ].getPixels();
		final float[] bs = ( float[] )fps[ 2 ].getPixels();
		
		for ( int i = 0; i < rgbs.length; ++i )
		{
			final int r = Math.round( rs[ i ] );
			final int g = Math.round( gs[ i ] );
			final int b = Math.round( bs[ i ] );
			
			/* preserves alpha even though ImageJ ignores it */
			rgbs[ i ] = ( rgbs[ i ] & 0xff000000 ) | ( ( ( ( r << 8 ) | g ) << 8 ) | b );
		}
	}
	
	protected void toByte( final byte[] bytes )
	{
		final float[] fs = ( float[] )fps[ 0 ].getPixels();
		for ( int i = 0; i < bytes.length; ++i )
			bytes[ i ] = ( byte )Math.round( fs[ i ] );
	}
	
	protected void toShort( final short[] shorts )
	{
		final float[] fs = ( float[] )fps[ 0 ].getPixels();
		for ( int i = 0; i < shorts.length; ++i )
			shorts[ i ] = ( short )Math.round( fs[ i ] );
	}
	
	public int setup( final String arg, final ImagePlus imp )
	{
		return flags;
	}
	
	public int showDialog( final ImagePlus imp, final String command, final PlugInFilterRunner pfr )
	{
		final GenericDialog gd = new GenericDialog( dialogTitle() );
		gd.addNumericField( "Block_radius_x : ", blockRadiusX, 0, 6, "pixels" );
		gd.addNumericField( "Block_radius_y : ", blockRadiusY, 0, 6, "pixels" );
		gd.addPreviewCheckbox( pfr );
		gd.addDialogListener( this );

		init( imp );
		
		gd.showDialog();
		if ( gd.wasCanceled() )
			return DONE;
		IJ.register( this.getClass() );
        return IJ.setupDialog( imp, flags );
	}
	

    public boolean dialogItemChanged( GenericDialog gd, AWTEvent e )
    {
        blockRadiusX = ( int )gd.getNextNumber();
        blockRadiusY = ( int )gd.getNextNumber();
        
        if ( gd.invalidNumber() )
            return false;
        
        return true;
    }
	
	public void run( final ImageProcessor ip )
	{
		int brx, bry;
		
		/*
		 * While processing a stack, ImageJ re-uses the same ImageProcessor instance
		 * but changes its pixel data using setPixels(Object).  Conversely, it uses
		 * a new ImageProcessor on apply than during preview.  At least the pixels
		 * seem not to be copied so hopefully that will do it for the long run
		 */
		if ( ip.getPixels() != pip.getPixels() )
		{
			init( new ImagePlus( "", ip ) );
		}
		synchronized( this )
		{
			brx = blockRadiusX;
			bry = blockRadiusY;
		}
		for ( int i = 0; i < bss.length; ++i )
			process( i, brx, bry );
		
		if ( FloatProcessor.class.isInstance( ip ) )
			return;
		else if ( ColorProcessor.class.isInstance( ip ) )
		{
			final int[] rgbs = ( int[] )ip.getPixels();
			toRGB( rgbs );
		}
		else if ( ByteProcessor.class.isInstance( ip ) )
		{
			final byte[] bytes = ( byte[] )ip.getPixels();
			toByte( bytes );
		}
		else if ( ShortProcessor.class.isInstance( ip ) )
		{
			final short[] shorts = ( short[] )ip.getPixels();
			toShort( shorts );
		}
	}

	@Override
	public void setNPasses( final int nPasses ) {}
}
