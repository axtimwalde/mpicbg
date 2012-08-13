package mpicbg.imagefeatures;


import java.io.Serializable;
import java.util.List;

import mpicbg.models.InverseCoordinateTransform;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;

/**
 * Local image feature
 * 
 * TODO Replace the {@link InverseCoordinateTransform Transformation}
 *   descriptors by a {@link InverseCoordinateTransform}.  Think about by
 *   hwich means to compare then!
 * 
 * @version 0.4b
 */
public class Feature implements Comparable< Feature >, Serializable
{
	/**
	 * v0.4b
	 */
	private static final long serialVersionUID = -6425872941323995891L;
	
	public float scale;
	public float orientation;
	public float[] location;
	public float[] descriptor;

	/** Dummy constructor for Serialization to work properly. */
	public Feature() {}
	
	public Feature( final float s, final float o, final float[] l, final float[] d )
	{
		scale = s;
		orientation = o;
		location = l;
		descriptor = d;
	}

	/**
	 * Comparator for making {@link Feature Features} sortable.
	 * 
	 * Please note, that the comparator returns -1 for
	 * {@link #scale this.scale} &gt; {@link #scale o.scale} to sort the
	 * features in a <em>descending</em> order.
	 */
	//@Override
	final public int compareTo( final Feature f )
	{
		return scale < f.scale ? 1 : scale == f.scale ? 0 : -1;
	}
	
	final public float descriptorDistance( final Feature f )
	{
		float d = 0;
		for ( int i = 0; i < descriptor.length; ++i )
		{
			final float a = descriptor[ i ] - f.descriptor[ i ];
			d += a * a;
		}
		return ( float )Math.sqrt( d );
	}
	
	/**
	 * Identify corresponding features
	 * 
	 * @param fs1 feature collection from set 1
	 * @param fs2 feature collection from set 2
	 * @param rod Ratio of distances (closest/next closest match)
	 * 
	 * @return matches
	 */
	final static public int matchFeatures(
			final List< Feature > fs1,
			final List< Feature > fs2,
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
		return matches.size();
	}
}

