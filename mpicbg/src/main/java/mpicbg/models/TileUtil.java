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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 *
 * @author Albert Cardona
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
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
			final Set< Tile< ? > > tiles,
			final Set< Tile< ? > > fixedTiles,
			final int nThreads) throws InterruptedException, ExecutionException
	{
		final ThreadGroup tg = Thread.currentThread().getThreadGroup();
		final ExecutorService exe = Executors.newFixedThreadPool(
				nThreads,
				new ThreadFactory() {
					final AtomicInteger c = new AtomicInteger(0);
					@Override
					public Thread newThread(final Runnable r) {
						final Thread t = new Thread(tg, r, "tile-fit-and-apply-" + c.incrementAndGet());
						t.setPriority(Thread.NORM_PRIORITY);
						return t;
					}
				});

		try {

			final long t0 = System.currentTimeMillis();

			final ArrayList<Tile<?>> shuffledTiles = new ArrayList<Tile<?>>(tiles.size() - fixedTiles.size());
			for (final Tile<?> t : tiles) {
				if (fixedTiles.contains(t)) continue;
				shuffledTiles.add(t);
			}
			Collections.shuffle(shuffledTiles);


			final long t1 = System.currentTimeMillis();
			System.out.println("Shuffling took " + (t1 - t0) + " ms");

			int i = 0;

			boolean proceed = i < maxIterations;

			/* initialize the configuration with the current model of each tile */
			tc.apply();

			final long t2 = System.currentTimeMillis();

			System.out.println("First apply took " + (t2 - t1) + " ms");

			final LinkedList< Future< ? > > futures = new LinkedList< Future< ? > >();
			final HashSet<Tile<?>> executingTiles = new HashSet<Tile<?>>(nThreads);

			while ( proceed )
			{
				/*
				for ( final Tile< ? > tile : tiles )
				{
					if ( fixedTiles.contains( tile ) ) continue;
					tile.fitModel();
					tile.apply();
				}
				*/

				final LinkedList<Tile<?>> pending = new LinkedList<Tile<?>>(shuffledTiles);
				Collections.shuffle(pending);

				while (!pending.isEmpty()) {
					final Tile<?> tile = pending.removeFirst();
					synchronized (executingTiles) {
						if (intersects(tile.getConnectedTiles(), executingTiles)) {
							pending.addLast(tile);
							continue;
						}
						executingTiles.add(tile);
					}
					futures.add(exe.submit(new Runnable() {
						@Override
						public void run() {
							try {
								tile.fitModel();
								tile.apply( damp );
								synchronized (executingTiles) {
									executingTiles.remove(tile);
								}
							} catch (final Exception e) {
								throw new RuntimeException(e);
							}
						}
					}));
					if (futures.size() > nThreads * 4) {
						for (int k=0; k<nThreads; ++k) {
							futures.removeFirst().get();
						}
					}
				}

				// Wait until all finish
				for (final Future<?> fu : futures) {
					fu.get();
				}

				executingTiles.clear();
				futures.clear();


				tc.updateErrors();
				observer.add( tc.getError() );

				IJ.log( i + ": " + observer.mean + " " + observer.max );

				if ( i > maxPlateauwidth )
				{
					proceed = tc.getError() > maxAllowedError;

					int d = maxPlateauwidth;
					while ( !proceed && d >= 1 )
					{
						try
						{
							proceed |= Math.abs( observer.getWideSlope( d ) ) > 0.0001;
						}
						catch ( final Exception e ) { e.printStackTrace(); }
						d /= 2;
					}
				}

				proceed &= ++i < maxIterations;
			}

			final long t3 = System.currentTimeMillis();

			System.out.println("Concurrent tile optimization loop took " + (t3 - t2) + " ms, total took " + (t3 - t0) + " ms");

		} finally {
			exe.shutdownNow();
		}
	}

	/** Whether {@param a} and {@param b} have any element in common. */
	private static boolean intersects(final Set<Tile<?>> a, final Set<Tile<?>> b) {
		final Set<Tile<?>> large, small;
		if (a.size() > b.size()) {
			large = a;
			small = b;
		} else {
			large = b;
			small = a;
		}
		for (final Tile<?> t : small) {
			if (large.contains(t)) return true;
		}
		return false;
	}
}
