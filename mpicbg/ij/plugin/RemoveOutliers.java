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
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.AWTEvent;

/**
 * Remove saturated pixels by diffusing the neighbors in.
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1a
 */
public class RemoveOutliers implements ExtendedPlugInFilter, DialogListener
{
	static protected int blockRadiusX = 40, blockRadiusY = 40;
	static protected double standardDeviations = 3;
	final static protected int flags = DOES_32 | DOES_STACKS;
	
	protected mpicbg.ij.integral.RemoveOutliers rmo;
	
	public int setup( final String arg, final ImagePlus imp )
	{
		return flags;
	}
	
	public int showDialog( final ImagePlus imp, final String command, final PlugInFilterRunner pfr )
	{
		final GenericDialog gd = new GenericDialog( "Remove Outliers" );
		gd.addNumericField( "Block_radius_x : ", blockRadiusX, 0, 6, "pixels" );
		gd.addNumericField( "Block_radius_y : ", blockRadiusY, 0, 6, "pixels" );
		gd.addNumericField( "Standard_deviations : ", standardDeviations, 2 );
		gd.addPreviewCheckbox( pfr );
		gd.addDialogListener( this );

		/* initialize summed area tables for statistical outlier removal */
		rmo = new mpicbg.ij.integral.RemoveOutliers( ( FloatProcessor )imp.getProcessor() );
		
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
        standardDeviations = gd.getNextNumber();
        
        if ( gd.invalidNumber() )
            return false;
        
        
        
        return true;
    }
	
	public void run( final ImageProcessor ip )
	{
		int brx, bry;
		double stds;
		if ( rmo == null )
		{
			try
			{
				rmo = new mpicbg.ij.integral.RemoveOutliers( ( FloatProcessor )ip );
			}
			catch ( final ClassCastException e )
			{
				IJ.error( "Processor type not yet supported." );
				e.printStackTrace();
				return;
			}
		}
		synchronized( this )
		{
			brx = blockRadiusX;
			bry = blockRadiusY;
			stds = standardDeviations;
		}
		rmo.removeOutliers( brx, bry, ( float )stds );
	}

	@Override
	public void setNPasses( final int nPasses ) {}
}
