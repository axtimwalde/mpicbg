/*-
 * #%L
 * MPICBG Core Library.
 * %%
 * Copyright (C) 2008 - 2025 Stephan Saalfeld et. al.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package mpicbg.ij;

import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import mpicbg.imagefeatures.Feature;
import mpicbg.imagefeatures.FloatArray2D;
import mpicbg.imagefeatures.FloatArray2DFeatureTransform;
import mpicbg.imagefeatures.ImageArrayConverter;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;

/**
 * @author Stephan Saalfeld &lt;saalfeld@mpi-cbg.de&gt;
 * @version 0.4b
 */
abstract public class FeatureTransform< T extends FloatArray2DFeatureTransform< ? > >
{
	final protected T t;

	/**
	 * Constructor
	 *
	 * @param t feature transformation
	 */
	public FeatureTransform( final T t )
	{
		this.t = t;
	}

	/**
	 * Extract features from an ImageProcessor
	 *
	 * @param ip
	 * @param features collects all features
	 */
	public void extractFeatures( final ImageProcessor ip, final Collection< Feature > features )
	{
		final FloatArray2D fa = new FloatArray2D( ip.getWidth(), ip.getHeight() );
		ImageArrayConverter.imageProcessorToFloatArray2DCropAndNormalize( ip, fa );

		t.init( fa );
	}

	final public Collection< Feature > extractFeatures( final ImageProcessor ip )
	{
		final Collection< Feature > features = new ArrayList<>();
		extractFeatures( ip, features );
		return features;
	}


	/**
	 * Identify corresponding features
	 *
	 * @param fs1 feature collection from set 1
	 * @param fs2 feature collection from set 2
	 * @param matches collects the matching coordinates
	 * @param rod Ratio of distances (closest/next closest match)
	 */
	static public void matchFeatures(
			final Collection<Feature> fs1,
			final Collection<Feature> fs2,
			final List<PointMatch> matches,
			final float rod
	) {
		final Feature[] targets = fs1.toArray(new Feature[0]);
		final Feature[] candidates = fs2.toArray(new Feature[0]);
		final TransposedFeatures transposed = new TransposedFeatures(candidates);
		final float[] dist = new float[candidates.length];

		// Running nearest and second-nearest candidate per target, continued across candidate
		// blocks. Blocks keep the transposed sub-matrix (128 x block x 4 bytes) L2-resident while
		// all targets sweep over it; otherwise every target re-streams the whole matrix from L3.
		final float[] best = new float[targets.length];
		final float[] second = new float[targets.length];
		final int[] closest = new int[targets.length];
		java.util.Arrays.fill(best, Float.MAX_VALUE);
		java.util.Arrays.fill(second, Float.MAX_VALUE);

		for (int blockStart = 0; blockStart < candidates.length; blockStart += CANDIDATE_BLOCK) {
			final int blockEnd = Math.min(candidates.length, blockStart + CANDIDATE_BLOCK);

			for (int i = 0; i < targets.length; ++i) {
				transposed.squaredDistances(targets[i].descriptor, dist, blockStart, blockEnd);

				// Scan distances, record index of closest candidate and best / second-best distance
				for (int j = blockStart; j < blockEnd; ++j) {
					final float d = dist[j];
					if (d < best[i]) {
						second[i] = best[i];
						best[i] = d;
						closest[i] = j;
					} else if (d < second[i]) {
						second[i] = d;
					}
				}
			}
		}

		for (int i = 0; i < targets.length; ++i) {
			if (second[i] < Float.MAX_VALUE && Math.sqrt(best[i]) / Math.sqrt(second[i]) < rod) {
				final Feature f1 = targets[i];
				final Feature f2 = candidates[closest[i]];
				final Point p1 = new Point(new double[]{f1.location[0], f1.location[1]});
				final Point p2 = new Point(new double[]{f2.location[0], f2.location[1]});
				matches.add(new PointMatch(p1, p2));
			}
		}

		removeAmbiguousMatches(matches);
	}

	/**
	 * Identify corresponding features
	 *
	 * @param fs1 feature collection from set 1
	 * @param fs2 feature collection from set 2
	 * @param rod Ratio of distances (closest/next closest match)
	 * @return the list of matching points
	 */
	static public List<PointMatch> matchFeatures(
			final Collection<Feature> fs1,
			final Collection<Feature> fs2,
			final float rod
	) {
		final List<PointMatch> matches = new ArrayList<>();
		matchFeatures(fs1, fs2, matches, rod);
		return matches;
	}

	/**
	 * Identify corresponding features with locations within a given radius
	 *
	 * @param fs1 feature collection from set 1
	 * @param fs2 feature collection from set 2
	 * @param radius the maximum distance between matched points
	 * @param rod Ratio of feature distances (closest/next closest match)
	 *
	 * @return the list of matching points
	 */
	static public List<PointMatch> matchFeaturesLocally(
			final Collection<Feature> fs1,
			final Collection<Feature> fs2,
			final double radius,
			final float rod
	) {
		final RadiusSearch neighborSearch = new RadiusSearch(fs2, radius);
		final List<PointMatch> matches = new ArrayList<>();

		for (final Feature f1 : fs1) {
			final Feature best = neighborSearch.findFor(f1, rod);

			if (best != null) {
				final Point p1 = new Point(new double[]{f1.location[0], f1.location[1]});
				final Point p2 = new Point(new double[]{best.location[0], best.location[1]});
				matches.add(new PointMatch(p1, p2));
			}
		}

		removeAmbiguousMatches(matches);
		return matches;
	}

	/**
	 * Remove ambiguous matches from a list of matches. A match is ambiguous if a point shows up more than once as the
	 * target of a match (i.e., the second point in the match).
	 *
	 * @param matches list of matches (will be modified in place)
	 */
	public static void removeAmbiguousMatches(List<PointMatch> matches) {
		for (int i = 0; i < matches.size(); ) {
			boolean isAmbiguous = false;
			final PointMatch m = matches.get(i);
			final double[] m_p2 = m.getP2().getL();

			for (int j = i + 1; j < matches.size(); ) {
				final PointMatch n = matches.get(j);
				final double[] n_p2 = n.getP2().getL();

				if (m_p2[0] == n_p2[0] && m_p2[1] == n_p2[1]) {
					isAmbiguous = true;
					matches.remove(j);
				} else {
					++j;
				}
			}

			if (isAmbiguous) {
				matches.remove(i);
			} else {
				++i;
			}
		}
	}


	/** Candidates per block: 128 components x 1024 x 4 bytes = 512 KiB, half of a typical L2. */
	private static final int CANDIDATE_BLOCK = 1024;

	/*
	 * Candidate descriptors stored transposed and column-major ({@code transposed[k][j]} is
	 * component k of candidate j). Thus, the distances of one target to all candidates can be
	 * computed with the candidate index as the innermost loop. That loop is a plain element-wise
	 * update of {@code dist[j]} with no cross-iteration dependency, which C2 auto-vectorizes; a
	 * per-pair sum over the 128 components is a float reduction, which it never vectorizes. The
	 * summation order per pair is the same as in {@link Feature#descriptorDistance}, so results
	 * are bit-identical.
	 */
	private static class TransposedFeatures {
		private final int n;
		private final float[][] transposed;

		/** Transpose {@code features} for vectorization during distance calculations. */
		TransposedFeatures(final Feature[] features) {
			final int m = features.length;
			n = (m == 0) ? 0 : features[0].descriptor.length;
			transposed = new float[n][m];

			for (int j = 0; j < m; ++j) {
				final float[] d = features[j].descriptor;
				for (int k = 0; k < n; ++k) {
					transposed[k][j] = d[k];
				}
			}
		}

		/**
		 * Squared distances of {@code target} to the candidates {@code start} (inclusive)
		 * to {@code end} (exclusive) into {@code dist}.
		 */
		void squaredDistances(final float[] target, final float[] dist, final int start, final int end) {
			java.util.Arrays.fill(dist, start, end, 0f);

			// Manually unroll the loops by 4 to help the compiler vectorize it
			int k = 0;
			for (; k < n - 3; k += 4) {
				// Load 4 components of the target descriptor
				final float t0 = target[k];
				final float t1 = target[k + 1];
				final float t2 = target[k + 2];
				final float t3 = target[k + 3];

				// Load 4 column-blocks of the transposed candidate descriptors
				final float[] c0 = transposed[k];
				final float[] c1 = transposed[k + 1];
				final float[] c2 = transposed[k + 2];
				final float[] c3 = transposed[k + 3];

				// Add to the squared distances for each candidate in the block
				for (int j = start; j < end; ++j) {
					final float a0 = t0 - c0[j];
					final float a1 = t1 - c1[j];
					final float a2 = t2 - c2[j];
					final float a3 = t3 - c3[j];
					dist[j] += a0 * a0 + a1 * a1 + a2 * a2 + a3 * a3;
				}
			}

			// Cleanup loop for any remaining components
			for (; k < n; ++k) {
				final float tk = target[k];
				final float[] ck = transposed[k];
				for (int j = start; j < end; ++j) {
					final float a = tk - ck[j];
					dist[j] += a * a;
				}
			}
		}
	}

	private static class RadiusSearch {

		private static class Node {
			private final Feature feature;
			private final Node left;
			private final Node right;

			public Node(Feature feature, Node left, Node right) {
				this.feature = feature;
				this.left = left;
				this.right = right;
			}
		}

		private final double radiusSquared;
		private final Node root;

		private Feature target;
		private Feature currentClosest;
		private double bestDistance;
		private double secondBestDistance;

		public RadiusSearch(Collection<Feature> features, double radius) {
			this.radiusSquared = radius * radius;
			this.root = buildTree(features, 0);
		}

		private static Node buildTree(Collection<Feature> features, int depth) {
			if (features.isEmpty()) {
				return null;
			}

			// Split axis on the median feature
			final int axis = depth % 2;
			final List<Feature> sorted = new ArrayList<>(features);
			sorted.sort(Comparator.comparingDouble(f -> f.location[axis]));
			final int median = sorted.size() / 2;
			final Feature medianFeature = sorted.get(median);

			// Recursively build subtrees
			final List<Feature> left = sorted.subList(0, median);
			final List<Feature> right = sorted.subList(median + 1, sorted.size());
			return new Node(medianFeature, buildTree(left, depth + 1), buildTree(right, depth + 1));
		}

		public Feature findFor(Feature f, double maxRatioOfDistances) {
			target = f;
			currentClosest = null;
			bestDistance = Double.MAX_VALUE;
			secondBestDistance = Double.MAX_VALUE;
			search(root, 0);

			if (secondBestDistance < Double.MAX_VALUE && bestDistance / secondBestDistance < maxRatioOfDistances) {
				return currentClosest;
			} else {
				return null;
			}
		}

		private void search(final Node node, final int depth) {
			if (node == null) {
				return;
			}

			// Include node if it is within the radius
			final double distanceSquared = locationDistanceSquared(target, node.feature);
			if (distanceSquared < radiusSquared) {
				final double d = target.descriptorDistance(node.feature);
				if (d < bestDistance) {
					secondBestDistance = bestDistance;
					bestDistance = d;
					currentClosest = node.feature;
				} else if (d < secondBestDistance) {
					secondBestDistance = d;
				}
			}

			// Check where the target is relative to the decision boundary
			final int axis = depth % 2;
			final double distanceToDecisionBoundary = target.location[axis] - node.feature.location[axis];

			// Decide which subtree the target is in and search it first
			final Node near, far;
			if (distanceToDecisionBoundary < 0) {
				near = node.left;
				far = node.right;
			} else {
				near = node.right;
				far = node.left;
			}
			search(near, depth + 1);

			// Only search the other subtree if it is within the radius
			if (far != null && distanceToDecisionBoundary * distanceToDecisionBoundary < radiusSquared) {
				search(far, depth + 1);
			}
		}

		private static double locationDistanceSquared(Feature a, Feature b) {
			final double dx = a.location[0] - b.location[0];
			final double dy = a.location[1] - b.location[1];
			return dx * dx + dy * dy;
		}
	}
}
