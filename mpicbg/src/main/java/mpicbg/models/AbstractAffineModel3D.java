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

import mpicbg.util.Util;

/**
 *
 * @param <M>
 *
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
public abstract class AbstractAffineModel3D < M extends AbstractAffineModel3D< M > > extends AbstractModel< M > implements InvertibleBoundable, Affine3D< M >
{
	private static final long serialVersionUID = -7611859904219650457L;

	public abstract double[] getMatrix( final double[] m );

	@Override
	public void estimateBounds( final double[] min, final double[] max )
	{
		final double[] rMin = new double[]{ Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE };
		final double[] rMax = new double[]{ -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE };

		final double[] f = min.clone();

		applyInPlace( f );
		Util.min( rMin, f );
		Util.max( rMax, f );

		f[ 0 ] = max[ 0 ];
		f[ 1 ] = min[ 1 ];
		f[ 2 ] = min[ 2 ];
		applyInPlace( f );
		Util.min( rMin, f );
		Util.max( rMax, f );

		f[ 0 ] = min[ 0 ];
		f[ 1 ] = max[ 1 ];
		f[ 2 ] = min[ 2 ];
		applyInPlace( f );
		Util.min( rMin, f );
		Util.max( rMax, f );

		f[ 0 ] = max[ 0 ];
		f[ 1 ] = max[ 1 ];
		f[ 2 ] = min[ 2 ];
		applyInPlace( f );
		Util.min( rMin, f );
		Util.max( rMax, f );

		f[ 0 ] = min[ 0 ];
		f[ 1 ] = min[ 1 ];
		f[ 2 ] = max[ 2 ];
		applyInPlace( f );
		Util.min( rMin, f );
		Util.max( rMax, f );

		f[ 0 ] = max[ 0 ];
		f[ 1 ] = min[ 1 ];
		f[ 2 ] = max[ 2 ];
		applyInPlace( f );
		Util.min( rMin, f );
		Util.max( rMax, f );

		f[ 0 ] = min[ 0 ];
		f[ 1 ] = max[ 1 ];
		f[ 2 ] = max[ 2 ];
		applyInPlace( f );
		Util.min( rMin, f );
		Util.max( rMax, f );

		f[ 0 ] = max[ 0 ];
		f[ 1 ] = max[ 1 ];
		f[ 2 ] = max[ 2 ];
		applyInPlace( f );
		Util.min( rMin, f );
		Util.max( rMax, f );

		min[ 0 ] = rMin[ 0 ];
		min[ 1 ] = rMin[ 1 ];
		min[ 2 ] = rMin[ 2 ];

		max[ 0 ] = rMax[ 0 ];
		max[ 1 ] = rMax[ 1 ];
		max[ 2 ] = rMax[ 2 ];
	}

	/**
	 * TODO not yet tested!
	 */
	@Override
	public void estimateInverseBounds( final double[] min, final double[] max ) throws NoninvertibleModelException
	{
		final double[] rMin = new double[]{ Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE };
		final double[] rMax = new double[]{ -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE };

		final double[] f = min.clone();

		applyInverseInPlace( f );
		Util.min( rMin, f );
		Util.max( rMax, f );

		f[ 0 ] = max[ 0 ];
		f[ 1 ] = min[ 1 ];
		f[ 2 ] = min[ 2 ];
		applyInverseInPlace( f );
		Util.min( rMin, f );
		Util.max( rMax, f );

		f[ 0 ] = min[ 0 ];
		f[ 1 ] = max[ 1 ];
		f[ 2 ] = min[ 2 ];
		applyInverseInPlace( f );
		Util.min( rMin, f );
		Util.max( rMax, f );

		f[ 0 ] = max[ 0 ];
		f[ 1 ] = max[ 1 ];
		f[ 2 ] = min[ 2 ];
		applyInverseInPlace( f );
		Util.min( rMin, f );
		Util.max( rMax, f );

		f[ 0 ] = min[ 0 ];
		f[ 1 ] = min[ 1 ];
		f[ 2 ] = max[ 2 ];
		applyInverseInPlace( f );
		Util.min( rMin, f );
		Util.max( rMax, f );

		f[ 0 ] = max[ 0 ];
		f[ 1 ] = min[ 1 ];
		f[ 2 ] = max[ 2 ];
		applyInverseInPlace( f );
		Util.min( rMin, f );
		Util.max( rMax, f );

		f[ 0 ] = min[ 0 ];
		f[ 1 ] = max[ 1 ];
		f[ 2 ] = max[ 2 ];
		applyInverseInPlace( f );
		Util.min( rMin, f );
		Util.max( rMax, f );

		f[ 0 ] = max[ 0 ];
		f[ 1 ] = max[ 1 ];
		f[ 2 ] = max[ 2 ];
		applyInverseInPlace( f );
		Util.min( rMin, f );
		Util.max( rMax, f );

		min[ 0 ] = rMin[ 0 ];
		min[ 1 ] = rMin[ 1 ];
		min[ 2 ] = rMin[ 2 ];

		max[ 0 ] = rMax[ 0 ];
		max[ 1 ] = rMax[ 1 ];
		max[ 2 ] = rMax[ 2 ];
	}

}
