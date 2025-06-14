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
public class Util
{
	/**
	 * Clip histogram and redistribute clipped entries.
	 * Compared to the original cited above, this uses a lookahead method to reduce
	 * the number of distribution steps. It is based on the algorithm described in
	 * "Resource Efficient Real-Time Processing of Contrast Limited Adaptive Histogram
	 * Equalization; Unal, Akoglu (2010)".
	 * 
	 * @param hist source
	 * @param clippedHist target 
	 * @param limit clip limit
	 */
	static private void clipHistogram(
			final int[] hist,
			final int[] clippedHist,
			final int limit )
	{
		// First pass: copy histogram and clip entries
		final int nBins = hist.length;
		int excess = 0;
		int fullBins = 0;
		for (int i = 0; i < nBins; ++i) {
			final int value = hist[i];
			if (value >= limit) {
				clippedHist[i] = limit;
				excess += value - limit;
				++fullBins;
			} else {
				clippedHist[i] = value;
			}
		}

		// Do redistribution pass until excess is exhausted
		while (excess > 0) {
			final int freeBins = nBins - fullBins;
			final int proRataExcess = excess / freeBins;
			int remainder = excess % freeBins;

			// Redistribute the average excess to all bins
			for (int i = 0; i < nBins; ++i) {
				// Distribute excess and remainder evenly across bins
				int value = clippedHist[i];
				if (value == limit) {
					// Skip previously filled bins
					continue;
				}

				// Add this bin's share of the excess and remainder
				value += proRataExcess;
				if (remainder > 0) {
					++value;
					--remainder;
				}

				if (value >= limit) {
					// Accumulate new excess in the remainder
					// There is a chance that it will be distributed already in this pass
					clippedHist[i] = limit;
					remainder += value - limit;
					++fullBins;
				} else {
					clippedHist[i] = value;
				}
			}

			// Update excess for next iteration
			excess = remainder;
		}
	}
	
	
	/**
	 * Create the full transfer function as a LUT
	 * 
	 * @param hist the histogram from which the function is generated
	 * @param limit the limit for clipping histogram entries
	 * @return the transfer function as a LUT
	 */
	static float[] createTransfer(
			final int[] hist,
			final int limit )
	{
		final int[] cdfs = new int[ hist.length ];
		clipHistogram( hist, cdfs, limit );
		
		int hMin = hist.length - 1;
		for ( int i = 0; i < hMin; ++i )
			if ( cdfs[ i ] != 0 ) hMin = i;
		
		int cdf = 0;
		for ( int i = hMin; i < hist.length; ++i )
		{
			cdf += cdfs[ i ];
			cdfs[ i ] = cdf;
		}
		
		final int cdfMin = cdfs[ hMin ];
		final int cdfMax = cdfs[ hist.length - 1 ];
		
		final float[] transfer = new float[ hist.length ];
		for ( int i = 0; i < transfer.length; ++i )
			transfer[ i ] = ( cdfs[ i ] - cdfMin ) / ( float )( cdfMax - cdfMin );
		
		return transfer;
	}
	
	
	/**
	 * Transfer a value.
	 * 
	 * @param v the value
	 * @param clippedHist the clipped histogram from which the transfer
	 *        function is generated
	 * @return the value of the transfer function evaluated at v
	 */
	static public float transferValue(
			final int v,
			final int[] clippedHist )
	{
		int hMin = clippedHist.length - 1;
		for ( int i = 0; i < hMin; ++i )
			if ( clippedHist[ i ] != 0 ) hMin = i;
		
		int cdf = 0;
		for ( int i = hMin; i <= v; ++i )
			cdf += clippedHist[ i ];
		
		int cdfMax = cdf;
		for ( int i = v + 1; i < clippedHist.length; ++i )
			cdfMax += clippedHist[ i ];
		
		final int cdfMin = clippedHist[ hMin ];
		
		return ( cdf - cdfMin ) / ( float )( cdfMax - cdfMin );
	}
	
	
	/**
	 * Transfer a value through contrast limited histogram equalization.
	 * For efficiency, the histograms to be used are passed as parameters.
	 *  
	 * @param v
	 * @param hist
	 * @param clippedHist
	 * @param limit
	 * @return
	 */
	static public float transferValue(
			final int v,
			final int[] hist,
			final int[] clippedHist,
			final int limit )
	{
		clipHistogram( hist, clippedHist, limit );
		return transferValue( v, clippedHist );
	}
}
