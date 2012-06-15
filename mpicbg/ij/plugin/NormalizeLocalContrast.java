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
    	nlcs[ i ].run( brx, bry, ( float )stds, center, stret );
    }
}
