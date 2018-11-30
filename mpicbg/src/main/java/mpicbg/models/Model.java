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

import java.util.Collection;
import java.util.List;

/**
 * {@link CoordinateTransform} whose parameters can be estimated through
 * a least-squares(like) fit.
 *
 * Provides methods for generic optimization and model extraction algorithms.
 * Currently, the Random Sample Consensus \cite{FischlerB81}, a robust
 * regression method and the Iterative Closest Point Algorithm \cite{Zhang94}
 * are implemented.
 *
 *
 * BibTeX:
 * <pre>
 * &#64;article{FischlerB81,
 *	 author    = {Martin A. Fischler and Robert C. Bolles},
 *   title     = {Random sample consensus: a paradigm for model fitting with applications to image analysis and automated cartography},
 *   journal   = {Communications of the ACM},
 *   volume    = {24},
 *   number    = {6},
 *   year      = {1981},
 *   pages     = {381--395},
 *   publisher = {ACM Press},
 *   address   = {New York, NY, USA},
 *   issn      = {0001-0782},
 *   doi       = {http://doi.acm.org/10.1145/358669.358692},
 * }
 * &#64;article{Zhang94,
 *   author    = {{Zhengyou Zhang}},
 *   title     = {Iterative point matching for registration of free-form curves and surfaces},
 *   journal   = {International Journal of Computer Vision},
 *   volume    = {13},
 *   number    = {2},
 *   month     = {October},
 *   year      = {1994},
 *   pages     = {119--152},
 * }
 * </pre>
 *
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 */
public interface Model< M extends Model< M > > extends CoordinateTransform
{
	/**
	 * @return the minimal number of {@link PointMatch PointMatches} required
	 *   to solve the model.
	 */
	public int getMinNumMatches();

	public double getCost();
	public void setCost( final double c );

	/**
	 * "Less than" operater to make {@link Model Models} comparable.
	 *
	 * @param m
	 * @return false for {@link #getCost() cost} &lt; 0.0, otherwise true if
	 *   {@link #getCost() this.getCost()} is smaller than
	 *   {@link #getCost() m.getCost()}
	 */
	public boolean betterThan( final M m );

	/**
	 * <p>Fit the {@link Model} to a set of data points minimizing the global
	 * transfer error.  This is assumed to be implemented as a weighted least
	 * squares minimization.</p>
	 *
	 * <p>This is a lower level version of {@link #fit(Collection)} for
	 * optimal memory efficiency.</p>
	 *
	 * <p>The estimated model transfers p to q.</p>
	 *
	 * <p><em>n</em>-dimensional points are passed as an <em>n</em>-dimensional
	 * array of floats, e.g. four 2d points as:</p>
	 * <pre><code>
	 * float[][]{
	 *   {x<sub>1</sub>, x<sub>2</sub>, x<sub>3</sub>, x<sub>4</sub>},
	 *   {y<sub>1</sub>, y<sub>2</sub>, y<sub>3</sub>, y<sub>4</sub>} }
	 * </code></pre>
	 *
	 * @param p source points
	 * @param q target points
	 * @param w weights
	 *
	 * @throws NotEnoughDataPointsException if not enough data points
	 *   were available
	 * @throws IllDefinedDataPointsException if the set of data points is
	 *   inappropriate to solve the Model
	 */
	public void fit(
			final float[][] p,
			final float[][] q,
			final float[] w ) throws NotEnoughDataPointsException, IllDefinedDataPointsException;

	/**
	 * <p>Fit the {@link Model} to a set of data points minimizing the global
	 * transfer error.  This is assumed to be implemented as a weighted least
	 * squares minimization.</p>
	 *
	 * <p>This is a lower level version of {@link #fit(Collection)} for
	 * optimal memory efficiency.</p>
	 *
	 * <p>The estimated model transfers p to q.</p>
	 *
	 * <p><em>n</em>-dimensional points are passed as an <em>n</em>-dimensional
	 * array of doubles, e.g. four 2d points as:</p>
	 * <pre><code>
	 * double[][]{
	 *   {x<sub>1</sub>, x<sub>2</sub>, x<sub>3</sub>, x<sub>4</sub>},
	 *   {y<sub>1</sub>, y<sub>2</sub>, y<sub>3</sub>, y<sub>4</sub>} }
	 * </code></pre>
	 *
	 * @param p source points
	 * @param q target points
	 * @param w weights
	 *
	 * @throws NotEnoughDataPointsException if not enough data points
	 *   were available
	 * @throws IllDefinedDataPointsException if the set of data points is
	 *   inappropriate to solve the Model
	 */
	public void fit(
			final double[][] p,
			final double[][] q,
			final double[] w ) throws NotEnoughDataPointsException, IllDefinedDataPointsException;

	/**
	 * Fit the {@link Model} to a set of data points minimizing the global
	 * transfer error.  This is assumed to be implemented as a weighted least
	 * squares minimization.  Use {@link #ransac ransac}
	 * and/ or {@link #filter} to remove outliers from your data points.
	 * <p>
	 * The estimated model transfers match.p1.local to match.p2.world.
	 * </p>
	 *
	 * @param matches set of point correpondences
	 * @throws NotEnoughDataPointsException if matches does not contain
	 *   enough data points
	 * @throws IllDefinedDataPointsException if the set of data points is
	 *   inappropriate to solve the Model
	 */
	public < P extends PointMatch >void fit( final Collection< P > matches )
		throws NotEnoughDataPointsException, IllDefinedDataPointsException;



	/**
	 * Test the {@link Model} for a set of {@link PointMatch} candidates.
	 * Return true if the number of inliers / number of candidates is larger
	 * than or equal to min_inlier_ratio, otherwise false.
	 * <p>
	 * Clears inliers and fills it with the fitting subset of candidates.
	 * </p>
	 * <p>
	 * Sets {@code cost = 1.0 - |inliers| / |candidates|}.
	 * </p>
	 *
	 * @param candidates set of point correspondence candidates
	 * @param inliers set of point correspondences that fit the model
	 * @param epsilon maximal allowed transfer error
	 * @param minInlierRatio minimal ratio {@code |inliers| / |candidates| (0.0 => 0%, 1.0 => 100%)}
	 * @param minNumInliers minimally required absolute number of inliers
	 */
	public < P extends PointMatch >boolean test(
			final Collection< P > candidates,
			final Collection< P > inliers,
			final double epsilon,
			final double minInlierRatio,
			final int minNumInliers );

	/**
	 * Call {@link #test(Collection, Collection, double, double, int)} with
	 * minNumInliers = {@link #getMinNumMatches()}.
	 */
	public < P extends PointMatch >boolean test(
			final Collection< P > candidates,
			final Collection< P > inliers,
			final double epsilon,
			final double minInlierRatio );

	/**
	 * Estimate the {@link Model} and filter potential outliers by robust
	 * iterative regression.
	 * <p>
	 * This method performs well on data sets with low amount of outliers.  If
	 * you have many outliers, you can filter those with a `tolerant' RANSAC
	 * first as done in {@link #filterRansac}.
	 * </p>
	 * <p>
	 * Sets {@link #getCost() cost} to the average point transfer error.
	 * </p>
	 *
	 * @param candidates Candidate data points eventually inluding some outliers
	 * @param inliers Remaining after the robust regression filter
	 * @param maxTrust reject candidates with a cost larger than
	 *   {@code maxTrust * median cost}
	 * @param minNumInliers minimally required absolute number of inliers
	 *
	 * @return true if {@link Model} could be estimated and inliers is not
	 *   empty, false otherwise.  If false, {@link Model} remains unchanged.
	 */
	public < P extends PointMatch >boolean filter(
			final Collection< P > candidates,
			final Collection< P > inliers,
			final double maxTrust,
			final int minNumInliers )
		throws NotEnoughDataPointsException;

	/**
	 * Call {@link #filter(Collection, Collection, double, int)} with minNumInliers = {@link Model#getMinNumMatches()}.
	 */
	public < P extends PointMatch >boolean filter(
			final Collection< P > candidates,
			final Collection< P > inliers,
			final double maxTrust )
		throws NotEnoughDataPointsException;

	/**
	 * Call {@link #filter(Collection, Collection, double)} with maxTrust = 4 and minNumInliers = {@link Model#getMinNumMatches()}.
	 */
	public < P extends PointMatch >boolean filter(
			final Collection< P > candidates,
			final Collection< P > inliers )
		throws NotEnoughDataPointsException;


	/**
	 * Find the {@link Model} of a set of {@link PointMatch} candidates
	 * containing a high number of outliers using
	 * {@link #ransac(List, Collection, int, double, double, int) RANSAC}
	 * \citet[{FischlerB81}.
	 *
	 * @param candidates candidate data points inluding (many) outliers
	 * @param inliers remaining candidates after RANSAC
	 * @param iterations number of iterations
	 * @param epsilon maximal allowed transfer error
	 * @param minInlierRatio minimal number of inliers to number of
	 *   candidates
	 * @param minNumInliers minimally required absolute number of inliers
	 *
	 * @return true if {@link Model} could be estimated and inliers is not
	 *   empty, false otherwise.  If false, {@link Model} remains unchanged.
	 */
	public < P extends PointMatch >boolean ransac(
			final List< P > candidates,
			final Collection< P > inliers,
			final int iterations,
			final double epsilon,
			final double minInlierRatio,
			final int minNumInliers )
		throws NotEnoughDataPointsException;

	/**
	 * Call {@link #ransac(List, Collection, int, double, double, int)} with
	 * minNumInliers = {@link #getMinNumMatches()}.
	 */
	public < P extends PointMatch >boolean ransac(
			final List< P > candidates,
			final Collection< P > inliers,
			final int iterations,
			final double epsilon,
			final double minInlierRatio )
		throws NotEnoughDataPointsException;

	/**
	 * Estimate a {@link Model} from a set with many outliers by first
	 * filtering the worst outliers with {@link #ransac RANSAC}
	 * \citet[{FischlerB81} and filter potential outliers by robust iterative
	 * regression.
	 *
	 * @param candidates candidate data points inluding (many) outliers
	 * @param inliers remaining candidates after RANSAC
	 * @param iterations number of iterations
	 * @param maxEpsilon maximal allowed transfer error
	 * @param minInlierRatio minimal number of inliers to number of
	 *   candidates
	 * @param minNumInliers minimally required absolute number of inliers
	 * @param maxTrust reject candidates with a cost larger than
	 *   maxTrust * median cost
	 *
	 * @return true if {@link Model} could be estimated and inliers is not
	 *   empty, false otherwise.  If false, {@link Model} remains unchanged.
	 */
	public < P extends PointMatch >boolean filterRansac(
			final List< P > candidates,
			final Collection< P > inliers,
			final int iterations,
			final double maxEpsilon,
			final double minInlierRatio,
			final int minNumInliers,
			final double maxTrust )
		throws NotEnoughDataPointsException;

	/**
	 * Call {@link #filterRansac(List, Collection, int, double, double, int, double)}
	 * with maxTrust = 4.
	 */
	public < P extends PointMatch >boolean filterRansac(
			final List< P > candidates,
			final Collection< P > inliers,
			final int iterations,
			final double maxEpsilon,
			final double minInlierRatio,
			final int minNumInliers )
		throws NotEnoughDataPointsException;

	/**
	 * Call {@link #filterRansac(List, Collection, int, double, double, int, double)}
	 * with minNumInliers = {@link #getMinNumMatches()}.
	 */
	public < P extends PointMatch >boolean filterRansac(
			final List< P > candidates,
			final Collection< P > inliers,
			final int iterations,
			final double maxEpsilon,
			final double minInlierRatio,
			final double maxTrust )
		throws NotEnoughDataPointsException;

	/**
	 * Call {@link #filterRansac(List, Collection, int, double, double, double)}
	 * with maxTrust = 4.
	 */
	public < P extends PointMatch >boolean filterRansac(
			final List< P > candidates,
			final Collection< P > inliers,
			final int iterations,
			final double maxEpsilon,
			final double minInlierRatio )
		throws NotEnoughDataPointsException;


	/**
	 * Filter a {@link Collection} of {@link PointMatch PointMatches} by the
	 * smoothness of their support for the given {@link Model}.  Smoothness
	 * means that each {@link PointMatch} agrees with a locally weighted least
	 * squares fit of this {@link Model} up to a given maximal transfer error
	 * or up to a given multiply of the local mean transfer error.  Locally
	 * weighted means that all {@link PointMatch PointMatches} contribute to
	 * the fit weighted by their Euclidean distance to the candidate.
	 *
	 * @param candidates
	 * @param inliers
	 * @param sigma
	 * @param maxEpsilon
	 * @param maxTrust
	 * @return
	 *
	 * @throws NotEnoughDataPointsException
	 * @throws IllDefinedDataPointsException
	 */
	public < P extends PointMatch > boolean localSmoothnessFilter(
			final Collection< P > candidates,
			final Collection< P > inliers,
			final double sigma,
			final double maxEpsilon,
			final double maxTrust )
		throws NotEnoughDataPointsException, IllDefinedDataPointsException;


	/**
	 * Estimate the best model in terms of the Iterative Closest Point
	 * Algorithm \cite{Zhang94} for matching two point clouds into each other.
	 * <pre>{@code
	 * p -> q
	 * }</pre>
	 *
	 * @param p source
	 * @param q target
	 *
	 * @return the list of matches
	 *
	 * TODO Test---at least once!
	 */
	public Collection< PointMatch > icp(
			final List< Point > p,
			final List< Point > q );

	/**
	 * Set the model to m
	 * @param m
	 */
	public void set( final M m );

	/**
	 * Clone the model.
	 */
	public M copy();
};
