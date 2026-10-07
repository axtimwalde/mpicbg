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
	/** All candidates. */
	public final List< P > candidates;

	/** Inliers of the best model found so far (empty if none yet). */
	public final Collection< P > bestInliers;

	/** Best model found so far (cost {@link Double#MAX_VALUE} and otherwise undefined if {@link #bestInliers} is empty). */
	public final M bestModel;

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
}
