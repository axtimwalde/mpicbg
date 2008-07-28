package mpicbg.imagefeatures;


import java.io.Serializable;

import mpicbg.models.InvertibleCoordinateTransform;

/**
 * Local image feature
 * 
 * TODO Replace the {@link InvertibleCoordinateTransform Transformation}
 *   descriptors by a {@link InvertibleCoordinateTransform}.  Think about by
 *   hwich means to compare then!
 * 
 * @version 0.3b
 */
public class Feature implements Comparable< Feature >, Serializable
{
	/**
	 * v0.3b
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
	public int compareTo( final Feature f )
	{
		return scale < f.scale ? 1 : scale == f.scale ? 0 : -1;
	}
	
	public float descriptorDistance( final Feature f )
	{
		float d = 0;
		for ( int i = 0; i < descriptor.length; ++i )
		{
			final float a = descriptor[ i ] - f.descriptor[ i ];
			d += a * a;
		}
		return ( float )Math.sqrt( d );
	}
}

