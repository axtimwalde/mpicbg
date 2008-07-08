package mpicbg.image;

// Copyright (c) 2001 Brent Boyer. All Rights Reserved.

/**
 * This code benchmarks the performance of copying an array purely in Java
 * versus copying it with System.arraycopy.
 * <p>
 * If available on the executing platform, it may be very useful to perform
 * benchmarks with the server JVM as well as the default client JVM.
 */
public class TestArrayCopy
{

	/**
	 * This constant specifies the minimum number of times that a task to be
	 * benchmarked must run before an actual timing measurement should be done.
	 * This is done both to load all relevant classes as well as to warm up
	 * hotspot before doing a measurement.
	 */
	private static final int MIN_WARMUP_CALLS = 10000;

	/**
	 * This constant specifies the minimum time that a task to be benchmarked
	 * must run to assure accurate timing measurements. It must be set
	 * reasonably large to deal with the low resolution clocks present on many
	 * platforms (e.g. typical windoze boxen have error around 10s of ms?). The
	 * units of this constant are milliseconds.
	 */
	private static final int MIN_BENCHMARK_TIME = 30 * 1000;

	public static final void main(String[] args) throws Exception
	{
		findWhenSystemBeatsJava();
		// exploreScalingOfSystemArrayCopy();
	}

	private static final void findWhenSystemBeatsJava() throws IllegalStateException
	{
		for (int i = 1; true; i++)
		{
			System.out.println();

			double javaWithNewTime = benchmarkTask(new JavaArrayCopyWithNew(i));
			System.out.println("Time to copy an int[" + i + "] purely in Java classical loop: " + javaWithNewTime + " us");

			double javaTime = benchmarkTask(new JavaArrayCopy(i));
			System.out.println("Time to copy an int[" + i + "] purely in Java for each: " + javaTime + " us");

			double systemTime = benchmarkTask(new SystemArrayCopy(i));
			System.out.println("Time to copy an int[" + i + "] using System.arraycopy: " + systemTime + " us");

			/*if (systemTime < javaTime)
			{
				if (i == 1) throw new IllegalStateException("found that System.arraycopy beats a pure Java copy even for an array of length 1");
				else
				{
					System.out.println();
					System.out.println("System.arraycopy first beats a pure Java copy for int arrays of length = " + i);
					break;
				}
			}*/
		}
		// +++ the above algorithm could be more efficient (e.g. first find an
		// upper bound using doubling or array length, and then use bisection to
		// find exact crossover)
	}

	// 2002/11/10 result: linear behavior starts for array lengths around 512,
	// give or take a factor of 2

	/**
	 * Measures how long it takes to execute the run method of the task arg.
	 * <p>
	 * This method uses the MIN_WARMUP_CALLS and MIN_BENCHMARK_TIME constants to
	 * obtain accurate results.
	 * <p>
	 * This method explicitly requests garbage collection before doing each
	 * benchmark. Therefore, unless you actually want to include the effect of
	 * garbage collection in the benchmark, the JVM should use a
	 * "stop-the-world" garbage collector, if it is available. (Usually it is.
	 * With Sun's tools, this is, in fact, the default garbage collector type.)
	 * The type of garbage collectors to avoid are incremental, concurrent, or
	 * parallel ones.
	 * <p>
	 * 
	 * @return average execution time of a single invocation of task.run, in
	 *         microseconds
	 * @see #MIN_WARMUP_CALLS
	 * @see #MIN_BENCHMARK_TIME
	 */
	private static final double benchmarkTask(Runnable task)
	{
		long numberOfRuns = MIN_WARMUP_CALLS;
		while (true)
		{
			System.gc();
			long t1 = System.currentTimeMillis();
			for (long i = 0; i < numberOfRuns; i++)
			{
				task.run();
			}
			long t2 = System.currentTimeMillis();

			long testRunTime = t2 - t1;
			if ((numberOfRuns > MIN_WARMUP_CALLS) && (testRunTime > MIN_BENCHMARK_TIME))
			{
				//System.out.println("numberOfRuns required = " + numberOfRuns);
				return (testRunTime * 1000.0) / numberOfRuns;
			}
			else
			{
				numberOfRuns *= 2;
			}
		}
	}

	private static final class JavaArrayCopyWithNew implements Runnable
	{
		private final int[] source;
		private final int[] target;
		
		JavaArrayCopyWithNew(int arrayLength)
		{
			source = new int[arrayLength];
			target = new int[arrayLength]; 
		}

		public void run()
		{
			for (int k = 0; k < 1000*1000; k++)
				for ( int i = 0; i < source.length; ++i) 
					target[ i ] = source[ i ];
			
		}
		
	}

	private static final class JavaArrayCopy implements Runnable
	{
		private final int[] source;
		private final int[] target;
		
		JavaArrayCopy(int arrayLength)
		{
			source = new int[arrayLength];
			target = new int[arrayLength];
		}

		public void run()
		{
			for (int k = 0; k < 1000*1000; k++)
			{
				int i = 0;
				for ( int a : source )
					target[ i++ ] = a;
			}
			
		}		
}

	private static final class SystemArrayCopy implements Runnable
	{
		private final int[] source;
		private final int[] target;

		SystemArrayCopy(int arrayLength)
		{
			source = new int[arrayLength];
			target = new int[arrayLength];
		}

		public void run()
		{
			System.arraycopy(source, 0, target, 0, target.length);
		}
	}

}