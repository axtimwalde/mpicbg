package mpicbg.ij;

import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.Collection;
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
		final Collection< Feature > features = new ArrayList< Feature >();
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
			final Collection< Feature > fs1,
			final Collection< Feature > fs2,
			final List< PointMatch > matches,
			final float rod )
	{
		for ( final Feature f1 : fs1 )
		{
			final Accumulator accumulator = new Accumulator(f1);
			for (final Feature f2 : fs2) {
				accumulator.accept(f2);
			}

			final Feature best = accumulator.getClosestChecked(rod);
			if (best != null) {
				final Point p1 = new Point(new double[]{f1.location[0], f1.location[1]});
				final Point p2 = new Point(new double[]{best.location[0], best.location[1]});
				matches.add(new PointMatch(p1, p2));
			}
		}

		// now remove ambiguous matches
		for ( int i = 0; i < matches.size(); )
		{
			boolean amb = false;
			final PointMatch m = matches.get( i );
			final double[] m_p2 = m.getP2().getL();
			for ( int j = i + 1; j < matches.size(); )
			{
				final PointMatch n = matches.get( j );
				final double[] n_p2 = n.getP2().getL();
				if ( m_p2[ 0 ] == n_p2[ 0 ] && m_p2[ 1 ] == n_p2[ 1 ] )
				{
					amb = true;
					matches.remove( j );
				}
				else ++j;
			}
			if ( amb )
				matches.remove( i );
			else ++i;
		}
	}


	private static class Accumulator implements Consumer<Feature> {
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

		public Feature getClosestChecked(double maxRatioOfDistances) {
			if (secondBestDistance < Double.MAX_VALUE && bestDistance / secondBestDistance < maxRatioOfDistances) {
				return currentClosest;
			} else {
				return null;
			}
		}
	}
}
