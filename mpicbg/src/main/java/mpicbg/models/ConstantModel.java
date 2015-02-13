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

import java.util.Collection;

/**
 * Wraps another models but does not pass through calls to {@link Model#fit}.
 * We use this to let models influence each other combining them in an
 * {@link InterpolatedModel}.
 *
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
public class ConstantModel< A extends Model< A >, M extends ConstantModel< A, M > > extends AbstractModel< M >
{
	private static final long serialVersionUID = 4028319936789363770L;

	final protected A model;

	public ConstantModel( final A model )
	{
		this.model = model;
	}

	public A getModel()
	{
		return model;
	}

	@Override
	public int getMinNumMatches()
	{
		return 0;
	}

	@Override
	public < P extends PointMatch > void fit( final Collection< P > matches ){}

	@Override
	public void set( final M m ) {}

	@Override
	public M copy()
	{
		@SuppressWarnings( "unchecked" )
		final M copy = ( M )new ConstantModel< A, M >( model.copy() );
		copy.cost = cost;
		return copy;
	}

	@Override
	public double[] apply( final double[] location )
	{
		final double[] copy = location.clone();
		applyInPlace( copy );
		return copy;
	}

	@Override
	public void applyInPlace( final double[] location )
	{
		model.applyInPlace( location );
	}
}
