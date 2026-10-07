package mpicbg.models;

import java.util.Collection;
import java.util.List;

/**
 * Progress of a {@link Model#ransac RANSAC} run, passed to the caller's
 * early stopping predicate before every iteration.
 *
 * @param <M> the model type
 * @param <P> the point match type
 */
public final class RansacState< M extends Model< M >, P extends PointMatch >
{
	private final List< P > candidates;
	private final Collection< P > bestInliers;
	private final M bestModel;
	private int iterations;

	RansacState( final List< P > candidates, final Collection< P > bestInliers, final M bestModel )
	{
		this.candidates = candidates;
		this.bestInliers = bestInliers;
		this.bestModel = bestModel;
	}

	RansacState< M, P > at( final int iterations )
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
	public Collection< P > bestInliers() { return bestInliers; }

	/** Cost of the best model found so far ({@link Double#MAX_VALUE} if none yet). */
	public double bestCost() { return bestModel.getCost(); }

	/** Best model found so far (undefined if {@link #bestNumInliers()} is 0). */
	public M bestModel() { return bestModel; }
}
