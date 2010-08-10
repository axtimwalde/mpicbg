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
			final int limit,
			final int bins )
	{
		System.arraycopy( hist, 0, clippedHist, 0, hist.length );
		int clippedEntries = 0, clippedEntriesBefore;
		do
		{
			clippedEntriesBefore = clippedEntries;
			clippedEntries = 0;
			for ( int i = 0; i <= bins; ++i )
			{
				final int d = clippedHist[ i ] - limit;
				if ( d > 0 )
				{
					clippedEntries += d;
					clippedHist[ i ] = limit;
				}
			}
			
			final int d = clippedEntries / ( bins + 1 );
			final int m = clippedEntries % ( bins + 1 );
			for ( int i = 0; i <= bins; ++i)
				clippedHist[ i ] += d;
			
			if ( m != 0 )
			{
				final int s = bins / m;
				for ( int i = s / 2; i <= bins; i += s )
					++clippedHist[ i ];
			}
		}
		while ( clippedEntries != clippedEntriesBefore );
	}
	
	
	/**
	 * Get the CDF entry of a value.
	 * 
	 * @param v the value
	 * @param hist the histogram from which the CDF is collected
	 * @param bins
	 * @return
	 */
	final static private float transferValue(
			final int v,
			final int[] hist,
			final int bins )
	{
		int hMin = bins;
		for ( int i = 0; i < hMin; ++i )
			if ( hist[ i ] != 0 ) hMin = i;
		
		int cdf = 0;
		for ( int i = hMin; i <= v; ++i )
			cdf += hist[ i ];
		
		int cdfMax = cdf;
		for ( int i = v + 1; i <= bins; ++i )
			cdfMax += hist[ i ];
		
		final int cdfMin = hist[ hMin ];
		
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
	 * @param bins
	 * @return
	 */
	final static public float transferValue(
			final int v,
			final int[] hist,
			final int[] clippedHist,
			final int limit,
			final int bins )
	{
		clipHistogram( hist, clippedHist, limit, bins );
		return transferValue( v, clippedHist, bins );
	}
	
	final static int roundPositive( float a )
	{
		return ( int )( a + 0.5f );
	}
}
