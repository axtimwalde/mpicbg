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
import ij.process.ImageProcessor;

import java.awt.AWTEvent;

/**
 * Apply a mean filter.
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1a
 */
public class Mean implements ExtendedPlugInFilter, DialogListener
{
	static protected int blockRadiusX = 10, blockRadiusY = 10;
	final static protected int flags = DOES_8G | DOES_16 | DOES_32 | DOES_RGB;
	
	protected mpicbg.ij.integral.Mean mean = null;
	protected ImageProcessor pip = null;
	
	public int setup( final String arg, final ImagePlus imp )
	{
		return flags;
	}
	
	public int showDialog( final ImagePlus imp, final String command, final PlugInFilterRunner pfr )
	{
		final GenericDialog gd = new GenericDialog( "Mean" );
		gd.addNumericField( "Block_radius_x : ", blockRadiusX, 0, 6, "pixels" );
		gd.addNumericField( "Block_radius_y : ", blockRadiusY, 0, 6, "pixels" );
		gd.addPreviewCheckbox( pfr );
		gd.addDialogListener( this );

		pip = imp.getProcessor();
		mean = mpicbg.ij.integral.Mean.create( pip );
		
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
			pip = ip;
			mean = mpicbg.ij.integral.Mean.create( pip );
		}

		synchronized( this )
		{
			brx = blockRadiusX;
			bry = blockRadiusY;
		}
		
		mean.mean( brx, bry );
	}

	@Override
	public void setNPasses( final int nPasses ) {}
}
