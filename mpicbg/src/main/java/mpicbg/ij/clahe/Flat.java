/*-
 * #%L
 * MPICBG Core Library.
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

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * "Contrast Limited Adaptive Histogram Equalization" as
 * described in
 * 
 * <p>BibTeX:</p>
 * <pre>
 * &#64;article{zuiderveld94,
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
 * @author Stephan Saalfeld &lt;saalfeld@mpi-cbg.de&gt;
 * @version 0.3b
 */
public class Flat
{
	final static private Flat instance = new Flat();
	final static private FastFlat fastInstance = new FastFlat();
	static public Flat getInstance(){ return instance; }
	static public FastFlat getFastInstance(){ return fastInstance; }
	
	/**
	 * A class for storing binned image data for efficient histogram processing.
	 * Provides methods for efficient histogram updates when iterating horizontally.
	 */
	private static class SlidingWindowHistogram {
		private final int[] binnedValues; // Column-major storage of binned pixel values
		private final int[] histogram;
		private final int[] clippedHistogram;
		private final int stride;
		private int yMin, yMax; // Current window bounds

		/**
		 * Create a new sliding window histogram
		 * @param src Source image
		 * @param bins Number of histogram bins
		 */
		public SlidingWindowHistogram(ByteProcessor src, int bins) {
			this.stride = src.getHeight();
			this.binnedValues = new int[src.getWidth() * src.getHeight()];
			this.histogram = new int[bins];
			this.clippedHistogram = new int[bins];
			final float binningFactor = (bins - 1) / 255.0f;

			// Precompute all binned pixel values and store in row-major order
			for (int y = 0; y < src.getHeight(); y++) {
				for (int x = 0; x < src.getWidth(); x++) {
					final int originalValue = src.get(x, y);
					binnedValues[x * stride + y] = mpicbg.util.Util.roundPos(originalValue * binningFactor);
				}
			}
		}

		/**
		 * Initialize the histogram for the current row with the initial window
		 * @param xMin Left bound of the initial window
		 * @param xMax Right bound of the initial window
		 * @param yMin Lower bound of the window
		 * @param yMax Upper bound of the window
		 */
		public void initialize(int xMin, int xMax, int yMin, int yMax) {
			// Clear histogram
			Arrays.fill(histogram, 0);
			this.yMin = yMin;
			this.yMax = yMax;

			// Fill histogram for initial window
				for (int x = xMin; x < xMax; x++) {
					int offset = x * stride;
					for (int y = yMin; y < yMax; y++) {
						histogram[binnedValues[offset + y]]++;
				}
			}
		}

		/**
		 * Add a new column to the histogram
		 * @param x x-coordinate of the column to add
		 */
		public void addColumn(final int x) {
			final int offset = x * stride;
			for (int y = yMin; y < yMax; y++) {
				++histogram[binnedValues[offset + y]];
			}
		}

		/**
		 * Remove a column from the histogram
		 * @param x x-coordinate of the column to remove
		 */
		public void removeColumn(final int x) {
			final int offset = x * stride;
			for (int y = yMin; y < yMax; y++) {
				--histogram[binnedValues[offset + y]];
			}
		}

		/**
		 * Get the normalized value for a pixel using histogram equalization
		 * @param v The binned pixel value
		 * @param limit The clip limit for histogram equalization
		 * @return The normalized pixel value (0-255)
		 */
		public int getNormalizedValue(final int v, final int limit) {
			return mpicbg.util.Util.roundPos(Util.transferValue(v, histogram, clippedHistogram, limit) * 255.0f);
		}
	}

	/**
	 * Process an {@link ImagePlus} with a given set of parameters.  Create
	 * mask and bounding box from the {@link Roi} of that {@link ImagePlus} and
	 * the passed mask if any.  Process {@link CompositeImage CompositeImages}
	 * as such.
	 * 
	 * @param imp
	 * @param blockRadius
	 * @param bins
	 * @param slope
	 * @param mask can be null
	 * 
	 * @deprecated Use the instance method
	 *   {@link #getInstance()}.{@link #run(ImagePlus, int, int, float, ByteProcessor, boolean)}
	 *   instead.
	 */
	@Deprecated
	static public void run(
			final ImagePlus imp,
			final int blockRadius,
			final int bins,
			final float slope,
			final ByteProcessor mask )
	{
		getInstance().run( imp, blockRadius, bins, slope, mask, true );
	}
	
	/**
	 * Process and {@link ImagePlus} with a given set of parameters.  Create
	 * mask and bounding box from the {@link Roi} of that {@link ImagePlus} and
	 * the passed mask if any.
	 * 
	 * @param imp
	 * @param blockRadius
	 * @param bins
	 * @param slope
	 * @param mask can be null
	 * @param composite how to process {@link CompositeImage CompositeImages}
	 *   true: interpret the displayed image as luminance channel for estimating the
	 *     contrast transfer function
	 *   false: process the active channel only as for non-composite images
	 */
	final public void run(
			final ImagePlus imp,
			final int blockRadius,
			final int bins,
			final float slope,
			final ByteProcessor mask,
			final boolean composite )
	
	{
		final Roi roi = imp.getRoi();
		if ( roi == null )
			run( imp, blockRadius, bins, slope, null, mask, composite );
		else
		{
			final Rectangle roiBox = roi.getBounds();
			final ImageProcessor roiMask = roi.getMask();
			if ( mask != null )
			{
				final Rectangle oldRoi = mask.getRoi();
				mask.setRoi( roi );
				final ByteProcessor cropMask = ( ByteProcessor )mask.crop().convertToByte( true );
				if ( roiMask != null )
				{
					final byte[] roiMaskPixels = ( byte[] )roiMask.getPixels();
					final byte[] cropMaskPixels = ( byte[] )cropMask.getPixels();
					for ( int i = 0; i < roiMaskPixels.length; ++i )
						cropMaskPixels[ i ] = ( byte )mpicbg.util.Util.roundPos( ( cropMaskPixels[ i ] & 0xff ) * ( roiMaskPixels[ i ] & 0xff ) / 255.0f );
				}
				run( imp, blockRadius, bins, slope, roiBox, cropMask, composite );
				mask.setRoi( oldRoi );
			}
			else if ( roiMask == null )
				run( imp, blockRadius, bins, slope, roiBox, null, composite );
			else
				run( imp, blockRadius, bins, slope, roiBox, ( ByteProcessor )roiMask.convertToByte( false ), composite );
		}
	}
	
	
	/**
	 * Process and {@link ImagePlus} with a given set of parameters including
	 * the bounding box and mask.
	 * 
	 * @param imp
	 * @param blockRadius
	 * @param bins
	 * @param slope
	 * @param roiBox can be null
	 * @param mask can be null
	 * @param composite how to process {@link CompositeImage CompositeImages}
	 *   true: interpret the displayed image as luminance channel for estimating the
	 *     contrast transfer function
	 *   false: process the active channel only as for non-composite images
	 */
	final public void run(
			final ImagePlus imp,
			final int blockRadius,
			final int bins,
			final float slope,
			final java.awt.Rectangle roiBox,
			final ByteProcessor mask,
			boolean composite )
	{
		composite = composite & imp.getNChannels() > 1;
		
		/* initialize box if necessary */
		final Rectangle box;
		if ( roiBox == null )
		{
			if ( mask == null )
				box = new Rectangle( 0, 0, imp.getWidth(), imp.getHeight() );
			else
				box = new Rectangle( 0, 0, Math.min( imp.getWidth(), mask.getWidth() ), Math.min( imp.getHeight(), mask.getHeight() ) );
		}
		else
			box = roiBox;
		
		/* make sure that the box is not larger than the mask */
		if ( mask != null )
		{
			box.width = Math.min( mask.getWidth(), box.width );
			box.height = Math.min( mask.getHeight(), box.height );
		}
		
		/* make sure that the box is not larger than the image */
		box.width = Math.min( imp.getWidth() - box.x, box.width );
		box.height = Math.min( imp.getHeight() - box.y, box.height );
		
		final int boxXMax = box.x + box.width;
		final int boxYMax = box.y + box.height;
		
		/* convert 8bit processors with a LUT to RGB */
		final ImageProcessor ip;
		if ( imp.getType() == ImagePlus.COLOR_256 )
		{
			ip = imp.getProcessor().convertToRGB();
			imp.setProcessor( imp.getTitle(), ip );
		}
		else
			ip = imp.getProcessor();
		
		/* work on ByteProcessors that reflect the user defined intensity range */
		/* for CompositeImages with composite checked, this is the image as displayed in the window */
		final ByteProcessor src;
		if ( composite )
			src = ( ByteProcessor )new ColorProcessor( imp.getImage() ).convertToByte( true );
		else if ( imp.getType() == ImagePlus.GRAY8 )
			src = ( ByteProcessor )ip.convertToByte( true ).duplicate();
		else
			src = ( ByteProcessor )ip.convertToByte( true );
		
		final ByteProcessor dst = ( ByteProcessor )src.duplicate();
		
		final ArrayList< Apply< ? > > appliers = new ArrayList< Apply< ? > >();
		try
		{
			if ( composite )
			{
				/* ignore ip and create Appliers for each channel, assuming that there is no COLOR_256 included */
				for ( int n = 0; n < imp.getNChannels(); ++n )
				{
					final int channelIndex = imp.getStackIndex( n + 1, imp.getSlice(), imp.getFrame() );
					final ImageProcessor cp = imp.getStack().getProcessor( channelIndex );
					switch ( imp.getType() )
					{
					case ImagePlus.GRAY8:
						appliers.add( new ByteApply( ( ByteProcessor )cp, src, dst, mask, box.x, box.y, boxXMax, boxYMax ) );
						break;
					case ImagePlus.GRAY16:
						appliers.add( new ShortApply( ( ShortProcessor )cp, src, dst, mask, box.x, box.y, boxXMax, boxYMax ) );
						break;
					case ImagePlus.GRAY32:
						appliers.add( new FloatApply( ( FloatProcessor )cp, src, dst, mask, box.x, box.y, boxXMax, boxYMax ) );
						break;
					case ImagePlus.COLOR_RGB:
						appliers.add( new RGBApply( ( ColorProcessor )cp, src, dst, mask, box.x, box.y, boxXMax, boxYMax ) );
						break;
					}
				}
			}
			else
			{
				switch ( imp.getType() )
				{
				case ImagePlus.GRAY8:
					appliers.add( new FastByteApply( ( ByteProcessor )ip, src, dst, mask, box.x, box.y, boxXMax, boxYMax ) );
					break;
				case ImagePlus.GRAY16:
					appliers.add( new ShortApply( ( ShortProcessor )ip, src, dst, mask, box.x, box.y, boxXMax, boxYMax ) );
					break;
				case ImagePlus.GRAY32:
					appliers.add( new FloatApply( ( FloatProcessor )ip, src, dst, mask, box.x, box.y, boxXMax, boxYMax ) );
					break;
				case ImagePlus.COLOR_RGB:
					appliers.add( new RGBApply( ( ColorProcessor )ip, src, dst, mask, box.x, box.y, boxXMax, boxYMax ) );
					break;
				}
			}
		}
		catch ( final Exception e )
		{
			IJ.error( e.getMessage() );
			return;
		}
		
		run( imp, blockRadius, bins, slope, box.x, box.y, boxXMax, boxYMax, src, dst, mask, ip, composite, appliers );
	}
	
	
	/**
	 * The actual implementation
	 * 
	 * @param imp
	 * @param blockRadius
	 * @param bins
	 * @param slope
	 * @param boxXMin
	 * @param boxYMin
	 * @param boxXMax
	 * @param boxYMax
	 * @param src
	 * @param dst
	 * @param mask
	 * @param ip
	 */
	protected void run(
			final ImagePlus imp,
			final int blockRadius,
			final int bins,
			final float slope,
			final int boxXMin,
			final int boxYMin,
			final int boxXMax,
			final int boxYMax,
			final ByteProcessor src,
			final ByteProcessor dst,
			final ByteProcessor mask,
			final ImageProcessor ip,
			final boolean composite,
			final ArrayList< Apply< ? > > appliers )
	{
		final boolean updatePerRow = imp.isVisible();
		final SlidingWindowHistogram windowHistogram = new SlidingWindowHistogram(src, bins);
		final float binningFactor = (bins - 1) / 255.0f;
		
		for ( int y = boxYMin; y < boxYMax; ++y )
		{
			final int yMin = Math.max( 0, y - blockRadius );
			final int yMax = Math.min( imp.getHeight(), y + blockRadius + 1 );
			final int h = yMax - yMin;
			
			/* initialization at x-1 */
			final int xMin0 = Math.max( 0, boxXMin - blockRadius - 1 );
			final int xMax0 = Math.min( imp.getWidth() - 1, boxXMin + blockRadius );
			
			/* initially fill histogram */
			windowHistogram.initialize(xMin0, xMax0, yMin, yMax);

			for ( int x = boxXMin; x < boxXMax; ++x )
			{
				final int v = mpicbg.util.Util.roundPos(src.get(x, y) * binningFactor);
				
				final int xMin = Math.max( 0, x - blockRadius );
				final int xMax = x + blockRadius + 1;
				final int w = Math.min( imp.getWidth(), xMax ) - xMin;
				final int n = h * w;
				
				final int limit;
				if ( mask == null )
					limit = mpicbg.util.Util.roundPos(slope * n / bins);
				else
					limit = mpicbg.util.Util.roundPos(1 + mask.get(x - boxXMin,  y - boxYMin) / 255.0f * (slope - 1) * n / bins);

				/* remove left behind values from histogram */
				if (xMin > 0) {
					windowHistogram.removeColumn(xMin - 1);
				}
					
				/* add newly included values to histogram */
				if (xMax <= imp.getWidth()) {
					windowHistogram.addColumn(xMax - 1);
				}

				final int normalizedValue = windowHistogram.getNormalizedValue(v, limit);
				dst.set(x, y, normalizedValue);
			}
			
			/* multiply the current row into ip or the respective channel */
			if ( updatePerRow )
			{
				for ( final Apply< ? > apply : appliers )
					apply.apply(
						boxXMin,
						y,
						boxXMax,
						y + 1 );
				imp.updateAndDraw();
			}
		}
		if ( !updatePerRow )
		{
			for ( final Apply< ? > apply : appliers )
				apply.apply(
					boxXMin,
					boxYMin,
					boxXMax,
					boxYMax );
			imp.updateAndDraw();
		}
	}
}
