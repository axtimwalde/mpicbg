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

import java.awt.geom.AffineTransform;
import java.util.Collection;

/**
 * 2d-translation {@link Model} to be applied to points in 2d-space.
 * 
 * @version 0.2b
 */
public class TranslationModel2D extends AbstractAffineModel2D< TranslationModel2D >
{
	static final protected int MIN_NUM_MATCHES = 1;
	
	protected float tx = 0, ty = 0;
	
	@Override
	final public int getMinNumMatches(){ return MIN_NUM_MATCHES; }
	
	@Override
	final public AffineTransform createAffine(){ return new AffineTransform( 1, 0, 0, 1, tx, ty ); }
	
	@Override
	final public AffineTransform createInverseAffine(){ return new AffineTransform( 1, 0, 0, 1, -tx, -ty ); }
	
	//@Override
	final public float[] apply( final float[] l )
	{
		assert l.length == 2 : "2d translation transformations can be applied to 2d points only.";
		
		return new float[]{ l[ 0 ] + tx, l[ 1 ] + ty };
	}
	
	//@Override
	final public void applyInPlace( final float[] l )
	{
		assert l.length == 2 : "2d translation transformations can be applied to 2d points only.";
		
		l[ 0 ] += tx;
		l[ 1 ] += ty;
	}
	
	//@Override
	final public float[] applyInverse( final float[] l )
	{
		assert l.length == 2 : "2d translation transformations can be applied to 2d points only.";
		
		return new float[]{ l[ 0 ] - tx, l[ 1 ] - ty };
	}

	//@Override
	final public void applyInverseInPlace( final float[] l )
	{
		assert l.length == 2 : "2d translation transformations can be applied to 2d points only.";
		
		l[ 0 ] -= tx;
		l[ 1 ] -= ty;
	}
	
	@Override
	final public void fit( final Collection< PointMatch > matches ) throws NotEnoughDataPointsException
	{
		if ( matches.size() < MIN_NUM_MATCHES ) throw new NotEnoughDataPointsException( matches.size() + " data points are not enough to estimate a 2d translation model, at least " + MIN_NUM_MATCHES + " data points required." );
		
		// center of mass:
		float pcx = 0, pcy = 0;
		float qcx = 0, qcy = 0;
		
		float ws = 0.0f;
		
		for ( final PointMatch m : matches )
		{
			final float[] p = m.getP1().getL(); 
			final float[] q = m.getP2().getW(); 
			
			final float w = m.getWeight();
			ws += w;
			
			pcx += w * p[ 0 ];
			pcy += w * p[ 1 ];
			qcx += w * q[ 0 ];
			qcy += w * q[ 1 ];
		}
		pcx /= ws;
		pcy /= ws;
		qcx /= ws;
		qcy /= ws;

		tx = qcx - pcx;
		ty = qcy - pcy;
	}
	
	@Override
	final public void shake( final float amount )
	{
		tx += rnd.nextGaussian() * amount;
		ty += rnd.nextGaussian() * amount;
	}

	@Override
	final public TranslationModel2D clone()
	{
		final TranslationModel2D m = new TranslationModel2D();
		m.tx = tx;
		m.ty = ty;
		m.cost = cost;
		return m;
	}
	
	@Override
	final public void set( final TranslationModel2D m )
	{
		tx = m.tx;
		ty = m.ty;
		cost = m.getCost();
	}

	@Override
	final public void preConcatenate( final TranslationModel2D m )
	{
		tx += m.tx;
		ty += m.ty;
	}
	
	@Override
	final public void concatenate( final TranslationModel2D m )
	{
		tx += m.tx;
		ty += m.ty;
	}
	
	/**
	 * Initialize the model such that the respective affine transform is:
	 * 
	 * 1 0 tx
	 * 0 1 ty
	 * 0 0 1
	 * 
	 * @param tx
	 * @param ty
	 */
	final public void set( final float tx, final float ty )
	{
		this.tx = tx;
		this.ty = ty;
	}
}
