package mpicbg.models;

import java.util.Collection;
import java.util.List;

/**
 * Decides whether {@link Model#ransac RANSAC} may stop before reaching its maximum number of iterations.
 * Evaluated before every iteration. The criterion is supplied by the caller.
 */
@FunctionalInterface
public interface RansacStoppingCriterion
{
	/** Never stops early; RANSAC runs for all iterations. */
	RansacStoppingCriterion NONE = state -> false;

	/**
	 * @param state progress of the current RANSAC run
	 *
	 * @return true to stop RANSAC now
	 */
	boolean shouldStop( State state );

	/**
	 * State of RANSAC progress
	 */
	final class State
	{
		private final List< ? > candidates;
		private final Collection< ? > bestInliers;
		private final Model< ? > bestModel;
		private int iterations;

		State( final List< ? > candidates, final Collection< ? > bestInliers, final Model< ? > bestModel )
		{
			this.candidates = candidates;
			this.bestInliers = bestInliers;
			this.bestModel = bestModel;
		}

		State at( final int iterations )
		{
			this.iterations = iterations;
			return this;
		}

		/** Number of iterations completed so far. */
		public int iterations() { return iterations; }

		/** Total number of candidates. */
		public int numCandidates() { return candidates.size(); }

		/** Number of inliers of the best model found so far (0 if none yet). */
		public int bestNumInliers() { return bestInliers.size(); }

		/** Inliers of the best model found so far (empty if none yet). */
		public Collection< ? > bestInliers() { return bestInliers; }

		/** Cost of the best model found so far ({@link Double#MAX_VALUE} if none yet). */
		public double bestCost() { return bestModel.getCost(); }

		/** Best model found so far (undefined if {@link #bestNumInliers()} is 0). */
		public Model< ? > bestModel() { return bestModel; }
	}
}
