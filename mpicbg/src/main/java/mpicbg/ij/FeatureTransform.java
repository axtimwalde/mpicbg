package mpicbg.ij;

import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

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
		final NearestNeighborSearch neighborSearch = new BruteForceSearch(fs2);

		for (final Feature f1 : fs1) {
			final FeatureAccumulator accumulator = neighborSearch.findFor(f1);
			final Feature best = accumulator.getClosestChecked(rod);

			if (best != null) {
				final Point p1 = new Point(new double[]{f1.location[0], f1.location[1]});
				final Point p2 = new Point(new double[]{best.location[0], best.location[1]});
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
		final NearestNeighborSearch neighborSearch = new RadiusSearch(fs2, radius);
		final List<PointMatch> matches = new ArrayList<>();

		for (final Feature f1 : fs1) {
			final FeatureAccumulator accumulator = neighborSearch.findFor(f1);
			final Feature best = accumulator.getClosestChecked(rod);

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


	private static class FeatureAccumulator implements Consumer<Feature> {
		private final Feature target;

		private Feature currentClosest = null;
		private double bestDistance = Double.MAX_VALUE;
		private double secondBestDistance = Double.MAX_VALUE;

		public FeatureAccumulator(Feature target) {
			this.target = target;
		}

		@Override
		public void accept(Feature feature) {
			final double d = target.descriptorDistance(feature);

			if (d < bestDistance) {
				secondBestDistance = bestDistance;
				bestDistance = d;
				currentClosest = feature;
			} else if (d < secondBestDistance) {
				secondBestDistance = d;
			}
		}

		public Feature getClosestChecked(double maxRatioOfDistances) {
			if (secondBestDistance < Double.MAX_VALUE && bestDistance / secondBestDistance < maxRatioOfDistances) {
				return currentClosest;
			} else {
				return null;
			}
		}
	}

	private interface NearestNeighborSearch {
		FeatureAccumulator findFor(Feature f);
	}

	private static class BruteForceSearch implements NearestNeighborSearch {
		private final Collection<Feature> features;

		public BruteForceSearch(Collection<Feature> features) {
			this.features = features;
		}

		@Override
		public FeatureAccumulator findFor(Feature f) {
			final FeatureAccumulator acc = new FeatureAccumulator(f);
			features.forEach(acc);
			return acc;
		}
	}

	private static class RadiusSearch implements NearestNeighborSearch {

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

		@Override
		public FeatureAccumulator findFor(Feature f) {
			final FeatureAccumulator acc = new FeatureAccumulator(f);
			search(root, f, 0, acc);
			return acc;
		}

		private void search(
				final Node node,
				final Feature target,
				final int depth,
				final FeatureAccumulator acc
		) {
			if (node == null) {
				return;
			}

			// Include node if it is within the radius
			final double distanceSquared = locationDistanceSquared(target, node.feature);
			if (distanceSquared < radiusSquared) {
				acc.accept(node.feature);
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
			search(near, target, depth + 1, acc);

			// Only search the other subtree if it is within the radius
			if (far != null && distanceToDecisionBoundary * distanceToDecisionBoundary < radiusSquared) {
				search(far, target, depth + 1, acc);
			}
		}

		private static double locationDistanceSquared(Feature a, Feature b) {
			final double dx = a.location[0] - b.location[0];
			final double dy = a.location[1] - b.location[1];
			return dx * dx + dy * dy;
		}
	}
}
