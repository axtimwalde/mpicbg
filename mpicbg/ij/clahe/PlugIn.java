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

import mpicbg.util.Util;

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
 * @version 0.1b
 */
public class PlugIn implements ij.plugin.PlugIn
{
	static private int blockRadius = 63;
	static private int bins = 255;
	static private float slope = 3;
	static private ByteProcessor mask = null;
	final static private String[] projections = new String[]{
			"Flat",
			"Equirectangular" };
	final static public int FLAT = 0;
	final static public int EQUIRECTANGULAR = 1;
	static private int projection;
	
	/* equirectangular projection */
	static private float minLambda = 0;
	static private float minPhi = 0;
	static private float hfov = 2 * ( float )Math.PI;
	static private float vfov = ( float )Math.PI;
	
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
		gd.addChoice( "projection : ", projections,  projections[ projection ] );
        
		gd.addHelp( "http://pacific.mpi-cbg.de/wiki/index.php/Enhance_Local_Contrast_(CLAHE)" );
		
		gd.showDialog();
		
		if ( gd.wasCanceled() ) return false;
		
		blockRadius = ( ( int )gd.getNextNumber() - 1 ) / 2;
		bins = ( int )gd.getNextNumber() - 1;
		slope = ( float )gd.getNextNumber();
		final int maskId = ids.get( gd.getNextChoiceIndex() );
		if ( maskId != -1 ) mask = ( ByteProcessor )WindowManager.getImage( maskId ).getProcessor().convertToByte( true );
		else mask = null;
		projection = gd.getNextChoiceIndex();
		
		final GenericDialog gdp;
		if ( projection == EQUIRECTANGULAR )
		{
			gdp = new GenericDialog( "Projection" );
			gdp.addNumericField( "min lambda : ", minLambda / Math.PI * 180, 2 );
			gdp.addNumericField( "min phi : ", minPhi / Math.PI * 180, 2 );
			gdp.addNumericField( "hfov : ", hfov / Math.PI * 180, 2 );
			gdp.addNumericField( "vfov : ", vfov / Math.PI * 180, 2 );	
			
			gdp.addHelp( "http://pacific.mpi-cbg.de/wiki/index.php/Enhance_Local_Contrast_(CLAHE)" );
			
			gdp.showDialog();
			
			if ( gdp.wasCanceled() ) return false;
			
			minLambda = ( float )( Util.mod( ( float )gdp.getNextNumber(), 360 ) / 180 * Math.PI );
			minPhi = ( float )( Util.mod( ( float )gdp.getNextNumber(), 180 ) / 180 * Math.PI );
			hfov = Math.min( ( float )( Math.PI * 2 - minLambda ), ( float )( Util.mod( ( float )gdp.getNextNumber(), 360 ) / 180 * Math.PI ) );
			vfov = Math.min( ( float )( Math.PI - minPhi ), ( float )( Util.mod( ( float )gdp.getNextNumber(), 180 ) / 180 * Math.PI ) );
			
			if ( hfov == 0 ) hfov = ( float )( 2 * Math.PI );
			if ( vfov == 0 ) vfov = ( float )Math.PI;
		}
		
		return true;
	}
	
	
//	@Override
	/**
	 * {@link PlugIn} access
	 * 
	 * @param arg not yet used
	 */
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
		switch ( projection )
		{
		case FLAT:
			Flat.run( imp, blockRadius, bins, slope, mask );
			break;
		case EQUIRECTANGULAR:
			Nonsense.run( imp, blockRadius, bins, slope, mask, minLambda, minPhi, hfov, vfov );
			break;
		}
	}
}
