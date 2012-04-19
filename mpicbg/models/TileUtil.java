package mpicbg.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

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
			final float maxAllowedError,
			final int maxIterations,
			final int maxPlateauwidth,
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
					public Thread newThread(Runnable r) {
						Thread t = new Thread(tg, r, "tile-fit-and-apply-" + c.incrementAndGet());
						t.setPriority(Thread.NORM_PRIORITY);
						return t;
					}
				});
		
		try {
		
			/**
			 * Given a {@link Set} of {@link Tile}s and another, overlapping set of fixed {@Tile}s,
			 * separating the tiles into small sets of independent tiles, for which itModel() and apply()
			 * can be computed concurrently with the other tiles.
			 */

			long t0 = System.currentTimeMillis();
			
			final Iterable< Tile< ? >[] > lazyGroups = generateIndependentGroups( tiles, nThreads );
			final ArrayList< Tile< ? >[] > groups = new ArrayList< Tile< ? >[] >( tiles.size() / nThreads );
			for ( final Tile< ? >[] group : lazyGroups ) {
				groups.add( group );
			}
			
			long t1 = System.currentTimeMillis();
			System.out.println("Split into groups took " + (t1 - t0) + " ms");
			
			final ArrayList< Future< ? > > futures = new ArrayList< Future< ? > >( nThreads );

			int i = 0;

			boolean proceed = i < maxIterations;

			/* initialize the configuration with the current model of each tile */
			tc.apply();
			
			long t2 = System.currentTimeMillis();
			
			System.out.println("First apply took " + (t2 - t1) + " ms");

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

				for (final Tile<?>[] independentGroup : groups) {
					// Concurrently execute a set of independent tasks
					for (final Tile<?> tile : independentGroup) {
						futures.add(exe.submit(new Runnable() {
							@Override
							public void run() {
								try {
									if (fixedTiles.contains(tile)) return;
									tile.fitModel();
									tile.apply();
								} catch (Exception e) {
									throw new RuntimeException(e);
								}
							}
						}));
					}

					// Wait until the entire group of independent operations have completed
					for (final Future<?> f : futures) {
						f.get();
					}

					futures.clear();
				}

				tc.updateErrors();
				observer.add( tc.getError() );

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
						catch ( Exception e ) { e.printStackTrace(); }
						d /= 2;
					}
				}

				proceed &= ++i < maxIterations;
			}
			
			long t3 = System.currentTimeMillis();
			
			System.out.println("Concurrent tile optimization loop took " + (t3 - t2) + " ms, total took " + (t3 - t0) + " ms");
			
		} finally { 
			exe.shutdownNow();
		}
	}
}
