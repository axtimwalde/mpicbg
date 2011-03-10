package mpicbg.ij;

import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import mpicbg.imagefeatures.Feature;
import mpicbg.imagefeatures.FloatArray2D;
import mpicbg.imagefeatures.FloatArray2DFeatureTransform;
import mpicbg.imagefeatures.ImageArrayConverter;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;

/**
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
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
	 * 
	 * @return number of detected features
	 */
	public void extractFeatures( final ImageProcessor ip, final Collection< Feature > features )
	{
		final FloatArray2D fa = new FloatArray2D( ip.getWidth(), ip.getHeight() );
		ImageArrayConverter.imageProcessorToFloatArray2DCropAndNormalize( ip, fa );
		
		t.init( fa );
	}
	
	final public Collection< Feature > extractFeatures( final ImageProcessor ip )
	{
		Collection< Feature > features = new ArrayList< Feature >();
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
			Feature best = null;
			float best_d = Float.MAX_VALUE;
			float second_best_d = Float.MAX_VALUE;
			
			for ( final Feature f2 : fs2 )
			{
				float d = f1.descriptorDistance( f2 );
				if ( d < best_d )
				{
					second_best_d = best_d;
					best_d = d;
					best = f2;
				}
				else if ( d < second_best_d )
					second_best_d = d;
			}
			if ( best != null && second_best_d < Float.MAX_VALUE && best_d / second_best_d < rod )
				matches.add(
						new PointMatch(
								new Point(
										new float[] { f1.location[ 0 ], f1.location[ 1 ] } ),
								new Point(
										new float[] { best.location[ 0 ], best.location[ 1 ] } ) ) );
		}
		
		// now remove ambiguous matches
		for ( int i = 0; i < matches.size(); )
		{
			boolean amb = false;
			final PointMatch m = matches.get( i );
			final float[] m_p2 = m.getP2().getL(); 
			for ( int j = i + 1; j < matches.size(); )
			{
				final PointMatch n = matches.get( j );
				final float[] n_p2 = n.getP2().getL(); 
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
}
