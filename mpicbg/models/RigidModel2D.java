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

public class RigidModel2D extends AffineModel2D
{
	static final protected int MIN_NUM_MATCHES = 2;
	
	@Override
	public int getMinNumMatches(){ return MIN_NUM_MATCHES; }

	@Override
	public String toString()
	{
		return ( "[3,3](" + affine + ") " + cost );
	}

	final public void fit( Collection< PointMatch > matches ) throws NotEnoughDataPointsException
	{
		if ( matches.size() < MIN_NUM_MATCHES ) throw new NotEnoughDataPointsException( matches.size() + " data points are not enough to estimate a 2d rigid model, at least " + MIN_NUM_MATCHES + " data points required." );
		
		// Implementing Johannes Schindelin's squared error minimization formula
		// tan(angle) = Sum(x1*y1 + x2y2) / Sum(x1*y2 - x2*y1)
		
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
		float sum1 = 0, sum2 = 0;
		float x1, y1, x2, y2;
		for ( PointMatch m : matches )
		{
			float[] p = m.getP1().getL(); 
			float[] q = m.getP2().getW();
			float w = m.getWeight();
			
			// make points local to the center of mass of the first landmark set
			x1 = p[ 0 ] - pcx; // x1
			y1 = p[ 1 ] - pcy; // x2
			x2 = q[ 0 ] - qcx + dx; // y1
			y2 = q[ 1 ] - qcy + dy; // y2
			sum1 += w * ( x1 * y2 - y1 * x2 ); //   x1 * y2 - x2 * y1 // assuming p1 is x1,x2 and p2 is y1,y2
			sum2 += w * ( x1 * x2 + y1 * y2 ); //   x1 * y1 + x2 * y2
		}
		float angle = ( float )Math.atan2( -sum1, sum2 );
		
		affine.setToIdentity();
		affine.rotate( -angle, qcx, qcy );
		affine.translate( -dx, -dy );
	}
	
	/**
	 * TODO Not yet implemented ...
	 */
	@Override
	final public void shake(
			float amount )
	{
		// TODO If you ever need it, please implement it...
	}
	
	@Override
	public RigidModel2D clone()
	{
		RigidModel2D m = new RigidModel2D();
		m.affine.setTransform( affine );
		m.cost = cost;
		return m;
	}

	public void preConcatenate( RigidModel2D model )
	{
		this.affine.preConcatenate(model.affine);
	}
	
	public void concatenate( RigidModel2D model )
	{
		this.affine.concatenate(model.affine);
	}
}
