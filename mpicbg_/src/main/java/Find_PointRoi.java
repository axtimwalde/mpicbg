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
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.PointRoi;
import ij.gui.Toolbar;
import ij.plugin.PlugIn;

import java.awt.BasicStroke;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;


public class Find_PointRoi implements PlugIn
{
	@Override
	public void run( final String arg )
	{
		try
		{
			final ImagePlus imp = IJ.getImage();
			final PointRoi roi = ( PointRoi )imp.getRoi();
			final int[] x = roi.getXCoordinates();
			final int[] y = roi.getYCoordinates();
			
			final GenericDialog gd = new GenericDialog( "Find PointRoi" );
			gd.addNumericField( "index :", 1, 0 );
			
			gd.showDialog();
			
			if ( gd.wasCanceled() )
			{
				imp.getCanvas().setDisplayList( null, null, null );
				return;
			}
			
			final int index = ( int )gd.getNextNumber();
			final Rectangle boundingBox = roi.getBounds();
			
			final int w = ( int )( 16 / imp.getCanvas().getMagnification() + 0.5 );
			
			imp.getCanvas().setDisplayList(
					new Ellipse2D.Float(
							boundingBox.x + x[ index - 1 ] - w,
							boundingBox.y + y[ index - 1 ] - w,
							2 * w,
							2 * w ),
					Toolbar.getForegroundColor(),
					new BasicStroke( 3 ) );
			
		}
		catch ( final Throwable e )
		{
			IJ.error( "No PointRoi found." );
		}
	}
}
