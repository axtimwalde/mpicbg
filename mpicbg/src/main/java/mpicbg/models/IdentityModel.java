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
import java.util.Collection;

/**
 * nd-identity {@link AbstractModel}.
 *
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
public class IdentityModel extends AbstractModel< IdentityModel > implements
		Affine1D< IdentityModel >, Affine2D< IdentityModel >, Affine3D< IdentityModel >, InvertibleBoundable
{
	private static final long serialVersionUID = 6465657594415965070L;

	static final protected int MIN_NUM_MATCHES = 0;

	@Override
	public int getMinNumMatches()
	{
		return MIN_NUM_MATCHES;
	}

	@Override
	final public double[] apply( final double[] l )
	{
		return l.clone();
	}

	@Override
	final public void applyInPlace( final double[] l ) {}

	@Override
	final public double[] applyInverse( final double[] l )
	{
		return l.clone();
	}

	@Override
	final public void applyInverseInPlace( final double[] l ) {}

	@Override
	final public void fit(
			final double[][] p,
			final double[][] q,
			final double[] w ) {}

	@Override
	final public void fit(
			final float[][] p,
			final float[][] q,
			final float[] w ) {}

	@Override
	final public < P extends PointMatch >void fit( final Collection< P > matches ) {}

	@Override
	public IdentityModel copy()
	{
		final IdentityModel m = new IdentityModel();
		m.cost = cost;
		return m;
	}

	@Override
	final public void set( final IdentityModel m )
	{
		cost = m.getCost();
	}

	@Override
	final public void preConcatenate( final IdentityModel m ) {}

	@Override
	final public void concatenate( final IdentityModel m ) {}

	/**
	 * TODO Not yet tested
	 */
	//@Override
	@Override
	public IdentityModel createInverse()
	{
		final IdentityModel ict = new IdentityModel();

		ict.cost = cost;

		return ict;
	}

	@Override
	public void toArray( final double[] data )
	{
		assert data.length > 1 : "Array must be at least 2 fields long.";

		data[ 0 ] = 1;
		data[ 1 ] = 0;

		if ( data.length > 5 )
		{
			data[ 2 ] = 0;
			if ( data.length > 11 )
			{
				data[ 3 ] = 0;
				data[ 4 ] = 1;
				data[ 5 ] = 0;
				data[ 6 ] = 0;
				data[ 7 ] = 0;
				data[ 8 ] = 1;
				data[ 9 ] = 0;
				data[ 10 ] = 0;
				data[ 11 ] = 0;
			}
			else
			{
				data[ 3 ] = 1;
				data[ 4 ] = 0;
				data[ 5 ] = 0;
			}
		}
	}

	@Override
	public void toMatrix( final double[][] data )
	{
		assert data.length > 0 && data[ 0 ].length > 1 : "Matrix must be at least 1x2 fields large.";

		data[ 0 ][ 0 ] = 1;
		data[ 0 ][ 1 ] = 0;

		if ( data.length > 1 )
		{
			data[ 0 ][ 2 ] = 0;
			data[ 1 ][ 0 ] = 0;
			data[ 1 ][ 1 ] = 1;
			data[ 1 ][ 2 ] = 0;
		}

		if ( data.length > 2 )
		{
			data[ 0 ][ 3 ] = 0;
			data[ 1 ][ 3 ] = 0;
			data[ 2 ][ 0 ] = 0;
			data[ 2 ][ 1 ] = 0;
			data[ 2 ][ 2 ] = 1;
			data[ 2 ][ 3 ] = 0;
		}
	}

	@Override
	public AffineTransform createAffine()
	{
		return new AffineTransform();
	}

	@Override
	public AffineTransform createInverseAffine()
	{
		return new AffineTransform();
	}

	@Override
	public void estimateBounds( final double[] min, final double[] max ) {}

	@Override
	public void estimateInverseBounds( final double[] min, final double[] max ) {}
}
