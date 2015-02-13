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

import java.awt.geom.AffineTransform;

/**
 *
 * @param <M>
 *
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
public abstract class AbstractAffineModel2D< M extends AbstractAffineModel2D< M > > extends AbstractModel< M > implements InvertibleBoundable, InvertibleCoordinateTransform, Affine2D< M >
{
	private static final long serialVersionUID = -4601554609754736334L;

	/**
	 * Create an {@link AffineTransform} representing the current parameters
	 * the model.
	 *
	 * @return {@link AffineTransform}
	 */
	@Override
	abstract public AffineTransform createAffine();

	/**
	 * Create an {@link AffineTransform} representing the inverse of the
	 * current parameters of the model.
	 *
	 * @return {@link AffineTransform}
	 */
	@Override
	abstract public AffineTransform createInverseAffine();

	@Override
	public void estimateBounds( final double[] min, final double[] max )
	{
		assert min.length >= 2 && max.length >= 2 : "2d affine transformations can be applied to 2d points only.";

		double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
		double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

		final double[] l = min.clone();
		applyInPlace( l );

		if ( l[ 0 ] < minX ) minX = l[ 0 ];
		if ( l[ 0 ] > maxX ) maxX = l[ 0 ];
		if ( l[ 1 ] < minY ) minY = l[ 1 ];
		if ( l[ 1 ] > maxY ) maxY = l[ 1 ];

		l[ 0 ] = min[ 0 ];
		l[ 1 ] = max[ 1 ];
		applyInPlace( l );

		if ( l[ 0 ] < minX ) minX = l[ 0 ];
		else if ( l[ 0 ] > maxX ) maxX = l[ 0 ];
		if ( l[ 1 ] < minY ) minY = l[ 1 ];
		else if ( l[ 1 ] > maxY ) maxY = l[ 1 ];

		l[ 0 ] = max[ 0 ];
		l[ 1 ] = max[ 1 ];
		applyInPlace( l );

		if ( l[ 0 ] < minX ) minX = l[ 0 ];
		else if ( l[ 0 ] > maxX ) maxX = l[ 0 ];
		if ( l[ 1 ] < minY ) minY = l[ 1 ];
		else if ( l[ 1 ] > maxY ) maxY = l[ 1 ];

		l[ 0 ] = max[ 0 ];
		l[ 1 ] = min[ 1 ];
		applyInPlace( l );

		if ( l[ 0 ] < minX ) minX = l[ 0 ];
		else if ( l[ 0 ] > maxX ) maxX = l[ 0 ];
		if ( l[ 1 ] < minY ) minY = l[ 1 ];
		else if ( l[ 1 ] > maxY ) maxY = l[ 1 ];

		min[ 0 ] = minX;
		min[ 1 ] = minY;
		max[ 0 ] = maxX;
		max[ 1 ] = maxY;
	}

	//@Override
	@Override
	public void estimateInverseBounds( final double[] min, final double[] max ) throws NoninvertibleModelException
	{
		assert min.length >= 2 && max.length >= 2 : "2d affine transformations can be applied to 2d points only.";

		double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
		double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

		final double[] l = min.clone();
		applyInverseInPlace( l );

		if ( l[ 0 ] < minX ) minX = l[ 0 ];
		if ( l[ 0 ] > maxX ) maxX = l[ 0 ];
		if ( l[ 1 ] < minY ) minY = l[ 1 ];
		if ( l[ 1 ] > maxY ) maxY = l[ 1 ];

		l[ 0 ] = min[ 0 ];
		l[ 1 ] = max[ 1 ];
		applyInverseInPlace( l );

		if ( l[ 0 ] < minX ) minX = l[ 0 ];
		else if ( l[ 0 ] > maxX ) maxX = l[ 0 ];
		if ( l[ 1 ] < minY ) minY = l[ 1 ];
		else if ( l[ 1 ] > maxY ) maxY = l[ 1 ];

		l[ 0 ] = max[ 0 ];
		l[ 1 ] = max[ 1 ];
		applyInverseInPlace( l );

		if ( l[ 0 ] < minX ) minX = l[ 0 ];
		else if ( l[ 0 ] > maxX ) maxX = l[ 0 ];
		if ( l[ 1 ] < minY ) minY = l[ 1 ];
		else if ( l[ 1 ] > maxY ) maxY = l[ 1 ];

		l[ 0 ] = max[ 0 ];
		l[ 1 ] = min[ 1 ];
		applyInverseInPlace( l );

		if ( l[ 0 ] < minX ) minX = l[ 0 ];
		else if ( l[ 0 ] > maxX ) maxX = l[ 0 ];
		if ( l[ 1 ] < minY ) minY = l[ 1 ];
		else if ( l[ 1 ] > maxY ) maxY = l[ 1 ];

		min[ 0 ] = minX;
		min[ 1 ] = minY;
		max[ 0 ] = maxX;
		max[ 1 ] = maxY;
	}

	@Override
	public String toString()
	{
		return ( "[3,3](" + createAffine() + ") " + cost );
	}

	@Override
	abstract public void preConcatenate( final M model );
	@Override
	abstract public void concatenate( final M model );

	//@Override
	@Override
	abstract public M createInverse();
}
