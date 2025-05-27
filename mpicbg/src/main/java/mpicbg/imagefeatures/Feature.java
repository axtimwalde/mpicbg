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
package mpicbg.imagefeatures;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import mpicbg.ij.FeatureTransform;
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
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 */
public class Feature implements Comparable< Feature >, Serializable
{
	private static final long serialVersionUID = 551364650592173605L;

	public double scale;
	public double orientation;
	public double[] location;
	public float[] descriptor; // save some memory

	/** Dummy constructor for Serialization to work properly. */
	public Feature() {}

	public Feature( final double s, final double o, final double[] l, final float[] d )
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
	@Override
	final public int compareTo( final Feature f )
	{
		return Double.compare(f.scale, scale);
	}

	final public double descriptorDistance( final Feature f )
	{
		float d = 0;

		// Unroll the loop to speed up the distance calculation
		// Feature descriptor lengths are like divisible by 4
		final int unrolledLength = descriptor.length - 3;
		int i = 0;
		for (; i < unrolledLength; i += 4) {
			final float a0 = descriptor[i] - f.descriptor[i];
			final float a1 = descriptor[i + 1] - f.descriptor[i + 1];
			final float a2 = descriptor[i + 2] - f.descriptor[i + 2];
			final float a3 = descriptor[i + 3] - f.descriptor[i + 3];
			d += a0 * a0 + a1 * a1 + a2 * a2 + a3 * a3;
		}

		for (; i < descriptor.length; ++i) {
			final float a = descriptor[i] - f.descriptor[i];
			d += a * a;
		}

		return Math.sqrt( d );
	}

	/**
	 * Identify corresponding features
	 * Deprecated: Use {@link FeatureTransform#matchFeatures(Collection, Collection, List, float)} instead.
	 *
	 * @param fs1 feature collection from set 1
	 * @param fs2 feature collection from set 2
	 * @param matches collects the matching coordinates
	 * @param rod Ratio of distances (closest/next closest match)
	 *
	 * @return Number of matches
	 */
	@Deprecated
	static public int matchFeatures(
			final List<Feature> fs1,
			final List<Feature> fs2,
			final List<PointMatch> matches,
			final double rod)
	{
		FeatureTransform.matchFeatures(fs1, fs2, matches, (float)rod);
		return matches.size();
	}
}

