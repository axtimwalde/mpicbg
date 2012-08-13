import java.awt.BasicStroke;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.PointRoi;
import ij.gui.Toolbar;
import ij.plugin.PlugIn;


public class Find_PointRoi implements PlugIn
{
	public void run( String arg )
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
		catch ( Throwable e )
		{
			IJ.error( "No PointRoi found." );
		}
	}
}
