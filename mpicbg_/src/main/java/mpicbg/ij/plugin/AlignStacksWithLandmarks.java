package mpicbg.ij.plugin;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

import java.util.Collection;

import mpicbg.ij.stack.InverseTransformMapping;
import mpicbg.models.AbstractModel;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.InverseCoordinateTransform;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.PointMatch;

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

/**
 * 
 *
 * @author Stephan Saalfeld &lt;saalfeld@mpi-cbg.de&gt;
 * @version 0.1a
 */
public class AlignStacksWithLandmarks implements PlugIn
{
	final static public < M extends AbstractModel< M > & InverseCoordinateTransform > ImagePlus createAlignedStack(
			final ImagePlus source,
			final ImagePlus target,
			final Collection< PointMatch > pointMatches,
			final M model ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		final ImageStack sourceStack = source.getStack();
		//final ImageStack targetStack = new ImageStack( target.getWidth(), target.getHeight(), target.getImageStackSize() );
		final ImageStack targetStack = new ImageStack( target.getWidth(), target.getHeight() );
		
		model.fit( pointMatches );
		
		final InverseTransformMapping< M > mapping = new InverseTransformMapping< M >( model );
		
		final ImageProcessor ipSource = sourceStack.getProcessor( 1 );
		
		for ( int i = 0; i < target.getNSlices(); ++i )
		{
			final ImageProcessor ip = ipSource.createProcessor( target.getWidth(), target.getHeight() );
			mapping.setSlice( i );
			mapping.mapInterpolated( sourceStack, ip );
			targetStack.addSlice( "" + i, ip );
			IJ.showProgress( i, target.getNSlices() );
		}
		final ImagePlus alignedTarget = source.createImagePlus();
		alignedTarget.setTitle( source.getTitle() + " aligned" );
		alignedTarget.setStack( targetStack, source.getNChannels(), target.getNSlices(), source.getNFrames() );
		alignedTarget.setCalibration( target.getCalibration().copy() );
		
		return alignedTarget;
	}

	/* (non-Javadoc)
	 * @see ij.plugin.PlugIn#run(java.lang.String)
	 */
	@Override
	public void run( String arg0 )
	{
	// TODO Auto-generated method stub

	}

}
