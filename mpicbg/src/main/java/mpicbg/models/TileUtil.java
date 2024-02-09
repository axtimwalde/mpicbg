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
package mpicbg.models;

import ij.IJ;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

/**
 *
 *
 * @author Albert Cardona
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 * @author Michael Innerberger
 */
public class TileUtil
{

	/**
	 * Returns a lazy collection of arrays of {@link Tile}, where none of the tiles of one specific array
	 * are connected to any of the tiles in that same array.
	 *
	 * Assumes that all tiles are not connected to all tiles, otherwise this operation will be very expensive
	 * and the returned arrays will contain a single {@link Tile} each.
	 *
	 * @param tiles The {@link Set} of {@link Tile}, where each {@link Tile} contains a {@link Set} of other {@link Tile} to whom it is connected with {@link PointMatch}es.
	 * @param maxArrayElements The maximum number of tiles to include in any one of the returned arrays.
	 * @return A {@link Collection} of {@link Tile} arrays, where, within each array, no one {@link Tile} is connected to any of the other {@link Tile} of the array.
	 */
	static public final Iterable< Tile< ? >[] > generateIndependentGroups(
			final Set< Tile < ? > > tiles,
			final int maxArrayElements )
	{
		return new Iterable<Tile<?>[]>()
		{
			@Override
			public final Iterator<Tile<?>[]> iterator()
			{
				return new Iterator<Tile<?>[]>()
				{
					/**
					 * Set of remaining tiles, which has been shuffled to try to minimize
					 * the probability that neighboring tiles are adjacent when iterating.
					 */
					final HashSet<Tile<?>> remaining = new HashSet<Tile<?>>( shuffle(tiles) );

					private final Collection<Tile<?>> shuffle( final Collection<Tile<?>> ts ) {
						final ArrayList<Tile<?>> s = new ArrayList<Tile<?>>( ts );
						Collections.shuffle( s );
						return s;
					}

					@Override
					public final boolean hasNext() {
						return remaining.size() > 0;
					}

					@Override
					public Tile<?>[] next() {
						final Tile<?>[] array = new Tile[ maxArrayElements ];
						final Iterator<Tile<?>> it = remaining.iterator();
						array[0] = it.next();
						it.remove();
						int next = 1;
						while (next < maxArrayElements && it.hasNext()) {
							final Tile<?> t = it.next();
							for (int i=0; i<next; ++i) {
								if (array[i].getConnectedTiles().contains(t)) {
									continue;
								} else {
									array[next] = t;
									next += 1;
									it.remove();
									break;
								}
							}
						}
						if (maxArrayElements != next) {
							final Tile<?>[] a = new Tile[ next ];
							System.arraycopy(array, 0, a, 0, next);
							return a;
						}
						return array;
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	static public final void optimizeConcurrently(
			final ErrorStatistic observer,
			final double maxAllowedError,
			final int maxIterations,
			final int maxPlateauwidth,
			final double damp,
			final TileConfiguration tc,
			final Set<Tile<?>> tiles,
			final Set<Tile<?>> fixedTiles,
			final int nThreads) {

		// only ThreadPoolExecutors know how many threads are currently running
		final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(nThreads);

		try {
			final long t0 = System.currentTimeMillis();

			final List<Tile<?>> freeTiles = new ArrayList<>(tiles.size() - fixedTiles.size());
			for (final Tile<?> t : tiles) {
				if (fixedTiles.contains(t)) continue;
				freeTiles.add(t);
			}
			Collections.shuffle(freeTiles);

			final long t1 = System.currentTimeMillis();
			System.out.println("Shuffling took " + (t1 - t0) + " ms");

			/* initialize the configuration with the current model of each tile */
			tc.apply(executor);

			final long t2 = System.currentTimeMillis();
			System.out.println("First apply took " + (t2 - t1) + " ms");

			int i = 0;
			boolean proceed = i < maxIterations;
			final Set<Tile<?>> executingTiles = ConcurrentHashMap.newKeySet();

			while (proceed) {
				Collections.shuffle(freeTiles);
				final Deque<Tile<?>> pending = new ConcurrentLinkedDeque<>(freeTiles);
				final List<Future<Void>> tasks = new ArrayList<>(nThreads);

				for (int j = 0; j < nThreads; j++) {
					final boolean cleanUp = (j == 0);
					tasks.add(executor.submit(() -> fitAndApplyWorker(pending, executingTiles, damp, cleanUp)));
				}

				for (final Future<Void> task : tasks) {
					try {
						task.get();
					} catch (final InterruptedException | ExecutionException e) {
						throw new RuntimeException(e);
					}
				}

				tc.updateErrors(executor);
				observer.add(tc.getError());

				IJ.log(i + ": " + observer.mean + " " + observer.max);

				if (i > maxPlateauwidth) {
					proceed = tc.getError() > maxAllowedError;

					int d = maxPlateauwidth;
					while (!proceed && d >= 1) {
						try {
							proceed |= Math.abs(observer.getWideSlope(d)) > 0.0001;
						} catch (final Exception e) {
							e.printStackTrace();
						}
						d /= 2;
					}
				}

				proceed &= ++i < maxIterations;
			}

			final long t3 = System.currentTimeMillis();

			System.out.println("Concurrent tile optimization loop took " + (t3 - t2) + " ms, total took " + (t3 - t0) + " ms");

		} finally {
			executor.shutdownNow();
		}
	}

	private static Void fitAndApplyWorker(
			final Deque<Tile<?>> pendingTiles,
			final Set<Tile<?>> executingTiles,
			final double damp,
			final boolean cleanUp
	) throws NotEnoughDataPointsException, IllDefinedDataPointsException {

		final int n = pendingTiles.size();
		for (int i = 0; (i < n) || cleanUp; i++){
			// the polled tile can only be null if the deque is empty, i.e., there is no more work
			final Tile<?> tile = pendingTiles.pollFirst();
			if (tile == null)
				return null;

			executingTiles.add(tile);
			final boolean canBeProcessed = Collections.disjoint(tile.getConnectedTiles(), executingTiles);

			if (canBeProcessed) {
				tile.fitModel();
				tile.apply(damp);
				executingTiles.remove(tile);
			} else {
				executingTiles.remove(tile);
				pendingTiles.addLast(tile);
			}
		}
		return null;
	}
}
