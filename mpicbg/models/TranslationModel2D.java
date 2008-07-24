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

import java.util.Collection;

/**
 * A 2d translation model.
 * 
 * @version 0.2b
 */
public class TranslationModel2D extends AffineModel2D
{
	static final protected int MIN_NUM_MATCHES = 1;
	
	@Override
	final public int getMinNumMatches(){ return MIN_NUM_MATCHES; }
	
	@Override
	final public void fit( Collection< PointMatch > matches ) throws NotEnoughDataPointsException
	{
		if ( matches.size() < MIN_NUM_MATCHES ) throw new NotEnoughDataPointsException( matches.size() + " data points are not enough to estimate a 2d translation model, at least " + MIN_NUM_MATCHES + " data points required." );
		
		// center of mass:
		float pcx = 0, pcy = 0;
		float qcx = 0, qcy = 0;
		
		double ws = 0.0;
		
		for ( PointMatch m : matches )
		{
			float[] p = m.getP1().getL(); 
			float[] q = m.getP2().getW(); 
			
			float w = m.getWeight();
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

		float dx = pcx - qcx;
		float dy = pcy - qcy;
		
		affine.setToIdentity();
		affine.translate( -dx, -dy );
	}
	
	@Override
	final public void shake(
			float amount )
	{
		affine.translate(
				rnd.nextGaussian() * amount,
				rnd.nextGaussian() * amount );
	}

	@Override
	public TranslationModel2D clone()
	{
		TranslationModel2D tm = new TranslationModel2D();
		tm.affine.setTransform( affine );
		tm.cost = cost;
		return tm;
	}
	
	public RigidModel2D toRigidModel2D()
	{
		RigidModel2D trm = new RigidModel2D();
		trm.getAffine().setTransform( affine );
		trm.cost = cost;
		return trm;
	}
}
