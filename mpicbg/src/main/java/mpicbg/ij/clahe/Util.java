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
 * &lsquot;Contrast Limited Adaptive Histogram Equalization&rsquot; as
 * described in
 * 
 * <br />BibTeX:
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
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.3b
 */
public class Util
{
	/**
	 * Clip histogram and redistribute clipped entries.
	 * 
	 * @param hist source
	 * @param clippedHist target 
	 * @param limit clip limit
	 * @param bins number of bins
	 */
	final static private void clipHistogram(
			final int[] hist,
			final int[] clippedHist,
			final int limit )
	{
		System.arraycopy( hist, 0, clippedHist, 0, hist.length );
		int clippedEntries = 0, clippedEntriesBefore;
		do
		{
			clippedEntriesBefore = clippedEntries;
			clippedEntries = 0;
			for ( int i = 0; i < hist.length; ++i )
			{
				final int d = clippedHist[ i ] - limit;
				if ( d > 0 )
				{
					clippedEntries += d;
					clippedHist[ i ] = limit;
				}
			}
			
			final int d = clippedEntries / ( hist.length );
			final int m = clippedEntries % ( hist.length );
			for ( int i = 0; i < hist.length; ++i)
				clippedHist[ i ] += d;
			
			if ( m != 0 )
			{
				final int s = ( hist.length - 1 ) / m;
				for ( int i = s / 2; i < hist.length; i += s )
					++clippedHist[ i ];
			}
		}
		while ( clippedEntries != clippedEntriesBefore );
	}
	
	
	/**
	 * Create the full transfer function as a LUT
	 * 
	 * @param v the value
	 * @param hist the histogram from which the function is generated
	 * @return
	 */
	final static float[] createTransfer(
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
	 * @return
	 */
	final static public float transferValue(
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
	final static public float transferValue(
			final int v,
			final int[] hist,
			final int[] clippedHist,
			final int limit )
	{
		clipHistogram( hist, clippedHist, limit );
		return transferValue( v, clippedHist );
	}
}
