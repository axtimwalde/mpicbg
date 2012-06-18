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
package mpicbg.ij.plugin;

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
 * Normalize contrast based on per-pixel mean and STD. 
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
public class NormalizeLocalContrast extends AbstractBlockFilter
{
	static protected double standardDeviations = 3;
	protected double stds;
	static protected boolean center = true;
	protected boolean cent = true;
	static protected boolean stretch = true;	
	protected boolean stret = true;
	
	protected mpicbg.ij.integral.NormalizeLocalContrast[] nlcs;
	
	@Override
	protected String dialogTitle()
	{
		return "Normalize Local Contrast";
	}
	
	@Override
	protected void init( final ImagePlus imp )
	{
		super.init( imp );
		if ( imp.getType() == ImagePlus.COLOR_RGB )
		{
			nlcs = new mpicbg.ij.integral.NormalizeLocalContrast[]{
					new mpicbg.ij.integral.NormalizeLocalContrast( fps[ 0 ] ),
					new mpicbg.ij.integral.NormalizeLocalContrast( fps[ 1 ] ),
					new mpicbg.ij.integral.NormalizeLocalContrast( fps[ 2 ] ) };
		}
		else
			nlcs = new mpicbg.ij.integral.NormalizeLocalContrast[]{ new mpicbg.ij.integral.NormalizeLocalContrast( fps[ 0 ] ) };
	}
	
	@Override
	public int showDialog( final ImagePlus imp, final String command, final PlugInFilterRunner pfr )
	{
		final GenericDialog gd = new GenericDialog( dialogTitle() );
		gd.addNumericField( "Block_radius_x : ", blockRadiusX, 0, 6, "pixels" );
		gd.addNumericField( "Block_radius_y : ", blockRadiusY, 0, 6, "pixels" );
		gd.addNumericField( "Standard_deviations : ", standardDeviations, 2 );
		gd.addCheckbox( "center", center );
		gd.addCheckbox( "stretch", stretch );
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
        center = gd.getNextBoolean();
        stretch = gd.getNextBoolean();
        
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
			cent = center;
			stret = stretch;
		}
    }
    
    @Override
    protected void process( final int i )
    {
    	nlcs[ i ].run( brx, bry, ( float )stds, cent, stret );
    }
    
    
    
    /**
     * Apply local contrast enhancement to and {@link ImageProcessor}.
     * The method performs best when you adjust the {@link ImageProcessor}s
     * min and max range such that a region of interest in the image has
     * optimal contrast and spans the full range.
     * 
     * @param ip
     * @param brx block width
     * @param bry block height
     * @param stds in case that the contrast is to be stretched, how many STDs
     *   wide should it be
     * @param cent local contrast range at the estimated mean (the original
     *   mean is at (max-min)/2), that is mean is mapped to min+(max-min)/2.
     * @param stret stretch contrast such that mean-stds*STD is mapped to min
     *   and mean+stds*STD is mapped to max (if cent is true) otherwise only
     *   the scaling is performed such that max-min = 2*stds*STD
     */
    static public void run(
    		final ImageProcessor ip,
    		final int brx,
    		final int bry,
    		final float stds,
    		final boolean cent,
    		final boolean stret )
    {
    	final NormalizeLocalContrast nlc = new NormalizeLocalContrast();
    	
    	nlc.init( new ImagePlus( "", ip ) );
    	
    	for ( int i = 0; i < nlc.nlcs.length; ++i )
    		nlc.nlcs[ i ].run( brx, bry, stds, cent, stret );
    	
    	if ( FloatProcessor.class.isInstance( ip ) )
			return;
		else if ( ColorProcessor.class.isInstance( ip ) )
		{
			final int[] rgbs = ( int[] )ip.getPixels();
			nlc.toRGB( rgbs );
		}
		else if ( ByteProcessor.class.isInstance( ip ) )
		{
			final byte[] bytes = ( byte[] )ip.getPixels();
			nlc.toByte( bytes );
		}
		else if ( ShortProcessor.class.isInstance( ip ) )
		{
			final short[] shorts = ( short[] )ip.getPixels();
			nlc.toShort( shorts );
		}
    }
}
