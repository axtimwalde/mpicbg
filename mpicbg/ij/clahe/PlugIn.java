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
package mpicbg.ij.clahe;

import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.Undo;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.process.ByteProcessor;

/**
 * &lsquot;Contrast Limited Adaptive Histogram Equalization&rsquot; as
 * described in
 * 
 * <br />BibTeX:
 * <pre>
 * @article{zuiderveld94,
 *   author    = {Zuiderveld, Karel},
 *   title     = {Contrast limited adaptive histogram equalization},
 *   book      = {Graphics gems IV},
 *   year      = {1994},
 *   isbn      = {0-12-336155-9},
 *   pages     = {474--485},
 *   publisher = {Academic Press Professional, Inc.},
 *   address   = {San Diego, CA, USA},
 * }
 * </pre>
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.3b
 */
public class PlugIn implements ij.plugin.PlugIn
{
	static private int blockRadius = 63;
	static private int bins = 255;
	static private float slope = 3;
	static private ByteProcessor mask = null;
	static private boolean fast = true;
	static private boolean composite = true;
	
	/**
	 * Get setting through a dialog
	 * 
	 * @param imp
	 * @return
	 */
	final static private boolean setup( final ImagePlus imp )
	{
		final ArrayList< Integer > ids = new ArrayList< Integer >();
		final ArrayList< String > titles = new ArrayList< String >();
		
		titles.add( "*None*" );
		ids.add( -1 );
		for ( final int id : WindowManager.getIDList() )
		{
			final ImagePlus impId = WindowManager.getImage( id );
			if ( impId.getWidth() == imp.getWidth() && impId.getHeight() == imp.getHeight() )
			{
				titles.add( impId.getTitle() );
				ids.add( id );
			}
		}		
		
		final GenericDialog gd = new GenericDialog( "CLAHE" );
		gd.addNumericField( "blocksize : ", blockRadius * 2 + 1, 0 );
		gd.addNumericField( "histogram bins : ", bins + 1, 0 );
		gd.addNumericField( "maximum slope : ", slope, 2 );
		gd.addChoice( "mask : ", titles.toArray( new String[ 0 ] ),  titles.get( 0 ) );
		gd.addCheckbox( "fast_(less_accurate)", fast );
		
		if ( imp.getNChannels() > 1 )
			gd.addCheckbox( "process_as_composite", composite );
		
		gd.addHelp( "http://pacific.mpi-cbg.de/wiki/index.php/Enhance_Local_Contrast_(CLAHE)" );
		
		gd.showDialog();
		
		if ( gd.wasCanceled() ) return false;
		
		blockRadius = ( ( int )gd.getNextNumber() - 1 ) / 2;
		bins = ( int )gd.getNextNumber() - 1;
		slope = ( float )gd.getNextNumber();
		final int maskId = ids.get( gd.getNextChoiceIndex() );
		if ( maskId != -1 ) mask = ( ByteProcessor )WindowManager.getImage( maskId ).getProcessor().convertToByte( true );
		else mask = null;
		fast = gd.getNextBoolean();
		if ( imp.isComposite() )
			composite = gd.getNextBoolean();
		
		return true;
	}
	
	
	/**
	 * {@link PlugIn} access
	 * 
	 * @param arg not yet used
	 */
	@Override
	final public void run( final String arg )
	{
		final ImagePlus imp = IJ.getImage();
		synchronized ( imp )
		{
			if ( !imp.isLocked() )
				imp.lock();
			else
			{
				IJ.error( "The image '" + imp.getTitle() + "' is in use currently.\nPlease wait until the process is done and try again." );
				return;
			}
		}
		
		if ( !setup( imp ) )
		{
			imp.unlock();
			return;
		}
		
		Undo.setup( Undo.TRANSFORM, imp );
		
		run( imp );
		imp.unlock();
	}
	
	/**
	 * Process an {@link ImagePlus} with the static parameters.  Create mask
	 * and bounding box from the {@link Roi} of that {@link ImagePlus} and
	 * the selected {@link #mask} if any.
	 * 
	 * @param imp
	 */
	final static public void run( final ImagePlus imp )
	{
		if ( fast )
			FastFlat.getInstance().run( imp, blockRadius, bins, slope, mask, composite );
		else
			Flat.getInstance().run( imp, blockRadius, bins, slope, mask, composite );
	}
}
