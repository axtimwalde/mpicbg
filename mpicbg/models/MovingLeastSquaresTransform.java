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
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 *
 */
package mpicbg.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class MovingLeastSquaresTransform implements CoordinateTransform
{
	protected Model< ? > model = new AffineModel2D();
	final public Model< ? > getModel(){ return model; }
	final public void setModel( final Model< ? > model ){ this.model = model; }
	final public void setModel( final Class< Model< ? > > modelClass )
	{
		try
		{
			model = modelClass.newInstance(); 
		}
		catch ( Exception e ){ e.printStackTrace(); }
	}
	
	protected float alpha = 1.0f;
	final public float getAlpha(){ return alpha; }
	final public void setAlpha( final float alpha ){ this.alpha = alpha; }
	
	final protected Set< PointMatch > matches = new HashSet< PointMatch >();
	final public Set< PointMatch > getMatches(){ return matches; }
	final public void setMatches( final Collection< PointMatch > matches )
	{
		this.matches.clear();
		this.matches.addAll( matches );
	}
	
	final protected double weigh( final double d )
	{
		return 1.0 / Math.pow( d, alpha );
	}
	
	//@Override
	public float[] apply( float[] location )
	{
		final float[] a = location.clone();
		applyInPlace( a );
		return a;
	}

	//@Override
	public void applyInPlace( final float[] location )
	{
		final Collection< PointMatch > weightedMatches = new ArrayList< PointMatch >();
		for ( final PointMatch m : matches )
		{
			final float[] l = m.getP1().getL();

//			/* specific for 2d */
//			final float dx = l[ 0 ] - location[ 0 ];
//			final float dy = l[ 1 ] - location[ 1 ];
//			
//			final float weight = m.getWeight() * ( float )weigh( 1.0f + Math.sqrt( dx * dx + dy * dy ) );
			
			float s = 0;
			for ( int i = 0; i < location.length; ++i )
			{
				final float dx = l[ i ] - location[ i ];
				s += dx * dx;
			}
			if ( s <= 0 )
			{
				final float[] w = m.getP2().getW();
				for ( int i = 0; i < location.length; ++i )
					location[ i ] = w[ i ];
				return;
			}
			final float weight = m.getWeight() * ( float )weigh( Math.sqrt( s / l.length ) );
			final PointMatch mw = new PointMatch( m.getP1(), m.getP2(), weight );
			weightedMatches.add( mw );
		}
		
		try 
		{
			model.fit( weightedMatches );
			model.applyInPlace( location );
		}
		catch ( IllDefinedDataPointsException e ){}
		catch ( NotEnoughDataPointsException e ){}
	}
}
