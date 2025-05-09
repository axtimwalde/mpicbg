/*-
 * #%L
 * MPICBG plugin for Fiji.
 * %%
 * Copyright (C) 2008 - 2025 Stephan Saalfeld et. al.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
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
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.AWTEvent;

/**
 * Remove saturated pixels by diffusing the neighbors in.
 *
 * @author Stephan Saalfeld &lt;saalfeld@mpi-cbg.de&gt;
 * @version 0.1a
 */
public class RemoveOutliers extends AbstractBlockFilter
{
	static protected double standardDeviations = 3;
	protected double stds;
	
	protected mpicbg.ij.integral.RemoveOutliers[] rmos;
	
	@Override
	protected String dialogTitle()
	{
		return "Remove Outliers";
	}
	
	@Override
	protected void init( final ImagePlus imp )
	{
		super.init( imp );
		if ( imp.getType() == ImagePlus.COLOR_RGB )
		{
			rmos = new mpicbg.ij.integral.RemoveOutliers[]{
					new mpicbg.ij.integral.RemoveOutliers( fps[ 0 ] ),
					new mpicbg.ij.integral.RemoveOutliers( fps[ 1 ] ),
					new mpicbg.ij.integral.RemoveOutliers( fps[ 2 ] ) };
		}
		else
			rmos = new mpicbg.ij.integral.RemoveOutliers[]{ new mpicbg.ij.integral.RemoveOutliers( fps[ 0 ] ) };
	}
	
	@Override
	public int showDialog( final ImagePlus imp, final String command, final PlugInFilterRunner pfr )
	{
		final GenericDialog gd = new GenericDialog( dialogTitle() );
		gd.addNumericField( "Block_radius_x : ", blockRadiusX, 0, 6, "pixels" );
		gd.addNumericField( "Block_radius_y : ", blockRadiusY, 0, 6, "pixels" );
		gd.addNumericField( "Standard_deviations : ", standardDeviations, 2 );
		gd.addPreviewCheckbox( pfr );
		gd.addDialogListener( this );

		init( imp );
		
		gd.showDialog();
		if ( gd.wasCanceled() )
			return DONE;
		IJ.register( this.getClass() );
        return IJ.setupDialog( imp, flags );
	}
	

    @Override
	public boolean dialogItemChanged( final GenericDialog gd, final AWTEvent e )
    {
        blockRadiusX = ( int )gd.getNextNumber();
        blockRadiusY = ( int )gd.getNextNumber();
        standardDeviations = gd.getNextNumber();
        
        if ( gd.invalidNumber() )
            return false;
        
        return true;
    }
    
    @Override
    protected void copyParameters()
    {
    	synchronized( this )
		{
			brx = blockRadiusX;
			bry = blockRadiusY;
			stds = standardDeviations;
		}
    }
    
    @Override
    protected void process( final int i )
    {
    	rmos[ i ].removeOutliers( brx, bry, ( float )stds );
    }
    
    
    /**
     * Remove outlier pixels from an {@link ImageProcessor}.
     * 
     * @param ip
     * @param brx block width
     * @param bry block height
     * @param stds how many STDs consititute the threshold for an outlier
     */
    static public void run(
    		final ImageProcessor ip,
    		final int brx,
    		final int bry,
    		final float stds )
    {
    	final RemoveOutliers rmo = new RemoveOutliers();
    	
    	rmo.init( new ImagePlus( "", ip ) );
    	
    	for ( int i = 0; i < rmo.rmos.length; ++i )
    		rmo.rmos[ i ].removeOutliers( brx, bry, stds );
    	
    	if ( FloatProcessor.class.isInstance( ip ) )
			return;
		else if ( ColorProcessor.class.isInstance( ip ) )
		{
			final int[] rgbs = ( int[] )ip.getPixels();
			rmo.toRGB( rgbs );
		}
		else if ( ByteProcessor.class.isInstance( ip ) )
		{
			final byte[] bytes = ( byte[] )ip.getPixels();
			rmo.toByte( bytes );
		}
		else if ( ShortProcessor.class.isInstance( ip ) )
		{
			final short[] shorts = ( short[] )ip.getPixels();
			rmo.toShort( shorts );
		}
    }
}
