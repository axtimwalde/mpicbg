package mpicbg.ij.util;

import mpicbg.imagefeatures.Feature;

import java.util.function.Consumer;

public class FeatureKdTree {


	public static class Accumulator implements Consumer<Feature> {
		private final Feature target;

		private Feature currentClosest = null;
		private double bestDistance = Double.MAX_VALUE;
		private double secondBestDistance = Double.MAX_VALUE;

		public Accumulator(Feature target) {
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

		public Feature getClosest() {
			return currentClosest;
		}

		public Feature getClosestChecked(double maxRatioOfDistances) {
			if (secondBestDistance < Double.MAX_VALUE && bestDistance / secondBestDistance < maxRatioOfDistances) {
				return currentClosest;
			} else {
				return null;
			}
		}

		private double currentSearchRadius() {
			return secondBestDistance;
		}
	}
}
