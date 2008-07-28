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
import java.awt.geom.NoninvertibleTransformException;
import java.util.Collection;

/**
 * 2d-translation {@link Model} to be applied to points in 2d-space.
 * 
 * @version 0.2b
 */
public class TranslationModel2D extends AbstractAffineModel2D< TranslationModel2D >
{
	static final protected int MIN_NUM_MATCHES = 1;
	
	@Override
	final public int getMinNumMatches(){ return MIN_NUM_MATCHES; }
	
	final protected AffineTransform affine = new AffineTransform();
	final protected AffineTransform inverseAffine = new AffineTransform();
	final public AffineTransform getAffine(){ return affine; }
	final public AffineTransform getInverseAffine(){ return inverseAffine; }
	
	//@Override
	final public float[] apply( final float[] point )
	{
		assert point.length == 2 : "2d translation transformations can be applied to 2d points only.";
		
		final float[] transformed = new float[ 2 ];
		affine.transform( point, 0, transformed, 0, 1 );
		return transformed;
	}
	
	//@Override
	final public void applyInPlace( final float[] point )
	{
		assert point.length == 2 : "2d translation transformations can be applied to 2d points only.";
		
		affine.transform( point, 0, point, 0, 1 );
	}
	
	//@Override
	final public float[] applyInverse( final float[] point ) throws NoninvertibleModelException
	{
		assert point.length == 2 : "2d translation transformations can be applied to 2d points only.";
		
		final float[] transformed = new float[ 2 ];
		try
		{
			inverseAffine.transform( point, 0, transformed, 0, 1 );
		}
		catch ( NullPointerException e )
		{
			throw new NoninvertibleModelException( e );
		}
		return transformed;
	}

	//@Override
	final public void applyInverseInPlace( final float[] point ) throws NoninvertibleModelException
	{
		assert point.length == 2 : "2d translation transformations can be applied to 2d points only.";
		
		try
		{
			inverseAffine.transform( point, 0, point, 0, 1 );
		}
		catch ( NullPointerException e )
		{
			throw new NoninvertibleModelException( e );
		}
	}
	
	@Override
	final public void fit( final Collection< PointMatch > matches ) throws NotEnoughDataPointsException
	{
		if ( matches.size() < MIN_NUM_MATCHES ) throw new NotEnoughDataPointsException( matches.size() + " data points are not enough to estimate a 2d translation model, at least " + MIN_NUM_MATCHES + " data points required." );
		
		// center of mass:
		float pcx = 0, pcy = 0;
		float qcx = 0, qcy = 0;
		
		double ws = 0.0;
		
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

		final float dx = pcx - qcx;
		final float dy = pcy - qcy;
		
		affine.setToIdentity();
		affine.translate( -dx, -dy );
		invert();
	}
	
	@Override
	final public void shake( final float amount )
	{
		affine.translate(
				rnd.nextGaussian() * amount,
				rnd.nextGaussian() * amount );
	}

	@Override
	final public TranslationModel2D clone()
	{
		final TranslationModel2D m = new TranslationModel2D();
		m.affine.setTransform( affine );
		m.cost = cost;
		return m;
	}
	
	@Override
	final public void set( final TranslationModel2D m )
	{
		this.affine.setTransform( m.getAffine() );
		this.cost = m.getCost();
	}

	final private void invert()
	{
		try
		{
			inverseAffine.setTransform( affine );
			inverseAffine.invert();
		}
		catch ( NoninvertibleTransformException e ){}
	}
	
	@Override
	final public void preConcatenate( final TranslationModel2D model )
	{
		affine.preConcatenate( model.getAffine() );
	}
	
	@Override
	final public void concatenate( final TranslationModel2D model )
	{
		affine.concatenate( model.getAffine() );
	}
	
	public RigidModel2D toRigidModel2D()
	{
		RigidModel2D trm = new RigidModel2D();
		trm.getAffine().setTransform( affine );
		trm.cost = cost;
		return trm;
	}
}
