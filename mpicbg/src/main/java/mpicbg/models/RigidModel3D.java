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

import mpicbg.util.Matrix3x3;
import Jama.EigenvalueDecomposition;
import Jama.Matrix;

/**
 *
 * @author Johannes Schindelin
 * @author Stephan Preibisch <preibisch@mpi-cbg.de>
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
public class RigidModel3D extends AbstractAffineModel3D< RigidModel3D > implements InvertibleBoundable
{
	private static final long serialVersionUID = 4703083104338949996L;

	static final protected int MIN_NUM_MATCHES = 3;

	protected double
	m00 = 1.0, m01 = 0.0, m02 = 0.0, m03 = 0.0,
	m10 = 0.0, m11 = 1.0, m12 = 0.0, m13 = 0.0,
	m20 = 0.0, m21 = 0.0, m22 = 1.0, m23 = 0.0;

	protected double
		i00 = 1.0, i01 = 0.0, i02 = 0.0, i03 = 0.0,
		i10 = 0.0, i11 = 1.0, i12 = 0.0, i13 = 0.0,
		i20 = 0.0, i21 = 0.0, i22 = 1.0, i23 = 0.0;

	@Override
	public double[] getMatrix( final double[] m )
	{
		final double[] a;
		if ( m == null || m.length != 12 )
			a = new double[ 12 ];
		else
			a = m;

		a[ 0 ] = m00;
		a[ 1 ] = m01;
		a[ 2 ] = m02;
		a[ 3 ] = m03;

		a[ 4 ] = m10;
		a[ 5 ] = m11;
		a[ 6 ] = m12;
		a[ 7 ] = m13;

		a[ 8 ] = m20;
		a[ 9 ] = m21;
		a[ 10 ] = m22;
		a[ 11 ] = m23;

		return a;
	}

	protected boolean isInvertible = true;

	@Override
	final public int getMinNumMatches(){ return MIN_NUM_MATCHES; }

	@Override
	final public double[] apply( final double[] l )
	{
		final double[] transformed = l.clone();
		applyInPlace( transformed );
		return transformed;
	}

	@Override
	final public void applyInPlace( final double[] l )
	{
		assert l.length >= 3 : "3d affine transformations can be applied to 3d points only.";

		final double l0 = l[ 0 ];
		final double l1 = l[ 1 ];
		l[ 0 ] = l0 * m00 + l1 * m01 + l[ 2 ] * m02 + m03;
		l[ 1 ] = l0 * m10 + l1 * m11 + l[ 2 ] * m12 + m13;
		l[ 2 ] = l0 * m20 + l1 * m21 + l[ 2 ] * m22 + m23;
	}

	@Override
	final public double[] applyInverse( final double[] l ) throws NoninvertibleModelException
	{
		final double[] transformed = l.clone();
		applyInverseInPlace( transformed );
		return transformed;
	}


	@Override
	final public void applyInverseInPlace( final double[] l ) throws NoninvertibleModelException
	{
		assert l.length >= 3 : "3d affine transformations can be applied to 3d points only.";

		if ( isInvertible )
		{
			final double l0 = l[ 0 ];
			final double l1 = l[ 1 ];
			l[ 0 ] = l0 * i00 + l1 * i01 + l[ 2 ] * i02 + i03;
			l[ 1 ] = l0 * i10 + l1 * i11 + l[ 2 ] * i12 + i13;
			l[ 2 ] = l0 * i20 + l1 * i21 + l[ 2 ] * i22 + i23;
		}
		else
			throw new NoninvertibleModelException( "Model not invertible." );
	}

	@Override
	final public String toString()
	{
		return
			"3d-rigid: (" +
			m00 + ", " + m01 + ", " + m02 + ", " + m03 + ", " +
			m10 + ", " + m11 + ", " + m12 + ", " + m13 + ", " +
			m20 + ", " + m21 + ", " + m22 + ", " + m23 + ")";
	}

	@Override
	final public <P extends PointMatch> void fit( final Collection< P > matches ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		if ( matches.size() < MIN_NUM_MATCHES )
			throw new NotEnoughDataPointsException(
					matches.size() + " data points are not enough to estimate a 3d rigid transformation model, at least " + MIN_NUM_MATCHES + " data points required." );

		double pcx = 0, pcy = 0, pcz = 0;
		double qcx = 0, qcy = 0, qcz = 0;

		double sumW = 0;

		for ( final P pm : matches )
		{
			final double[] p1 = pm.getP1().getL();
			final double[] p2 = pm.getP2().getW();
			final double w = pm.getWeight();

			pcx += p1[ 0 ] * w;
			pcy += p1[ 1 ] * w;
			pcz += p1[ 2 ] * w;
			qcx += p2[ 0 ] * w;
			qcy += p2[ 1 ] * w;
			qcz += p2[ 2 ] * w;
			sumW += w;
		}

		pcx /= sumW;
		pcy /= sumW;
		pcz /= sumW;
		qcx /= sumW;
		qcy /= sumW;
		qcz /= sumW;

		// calculate N
		double Sxx = 0, Sxy = 0, Sxz = 0, Syx = 0, Syy = 0, Syz = 0, Szx = 0, Szy = 0, Szz = 0;

		for ( final P pm : matches )
		{
			final double[] p1 = pm.getP1().getL();
			final double[] p2 = pm.getP2().getW();
			final double w = pm.getWeight();

			final double x1 = (p1[ 0 ] - pcx) * w;
			final double y1 = (p1[ 1 ] - pcy) * w;
			final double z1 = (p1[ 2 ] - pcz) * w;
			final double x2 = (p2[ 0 ] - qcx) * w;
			final double y2 = (p2[ 1 ] - qcy) * w;
			final double z2 = (p2[ 2 ] - qcz) * w;
			Sxx += x1 * x2;
			Sxy += x1 * y2;
			Sxz += x1 * z2;
			Syx += y1 * x2;
			Syy += y1 * y2;
			Syz += y1 * z2;
			Szx += z1 * x2;
			Szy += z1 * y2;
			Szz += z1 * z2;
		}

		final double[][] N = new double[ 4 ][ 4 ];
		N[0][0] = Sxx + Syy + Szz;
		N[0][1] = Syz - Szy;
		N[0][2] = Szx - Sxz;
		N[0][3] = Sxy - Syx;
		N[1][0] = Syz - Szy;
		N[1][1] = Sxx - Syy - Szz;
		N[1][2] = Sxy + Syx;
		N[1][3] = Szx + Sxz;
		N[2][0] = Szx - Sxz;
		N[2][1] = Sxy + Syx;
		N[2][2] = -Sxx + Syy - Szz;
		N[2][3] = Syz + Szy;
		N[3][0] = Sxy - Syx;
		N[3][1] = Szx + Sxz;
		N[3][2] = Syz + Szy;
		N[3][3] = -Sxx - Syy + Szz;

		// calculate eigenvector with maximal eigenvalue
		final EigenvalueDecomposition evd = new EigenvalueDecomposition( new Matrix( N ) );

		final double[] eigenvalues = evd.getRealEigenvalues();
		final Matrix eigenVectors = evd.getV();

		int index = 0;
		for (int i = 1; i < 4; i++)
			if (eigenvalues[i] > eigenvalues[index])
				index = i;

		final double q0 = eigenVectors.get( 0, index );
		final double qx = eigenVectors.get( 1, index );
		final double qy = eigenVectors.get( 2, index );
		final double qz = eigenVectors.get( 3, index );

		// set result
		m00 = q0 * q0 + qx * qx - qy * qy - qz * qz;
		m01 = 2 * (qx * qy - q0 * qz);
		m02 = 2 * (qx * qz + q0 * qy);
		m10 = 2 * (qy * qx + q0 * qz);
		m11 = (q0 * q0 - qx * qx + qy * qy - qz * qz);
		m12 = 2 * (qy * qz - q0 * qx);
		m20 = 2 * (qz * qx - q0 * qy);
		m21 = 2 * (qz * qy + q0 * qx);
		m22 = (q0 * q0 - qx * qx - qy * qy + qz * qz);

		// translational part
		m03 = qcx - m00 * pcx - m01 * pcy - m02 * pcz;
		m13 = qcy - m10 * pcx - m11 * pcy - m12 * pcz;
		m23 = qcz - m20 * pcx - m21 * pcy - m22 * pcz;

		invert();
	}
	@Override
	final public void set( final RigidModel3D m )
	{
		m00 = m.m00;
		m10 = m.m10;
		m20 = m.m20;
		m01 = m.m01;
		m11 = m.m11;
		m21 = m.m21;
		m02 = m.m02;
		m12 = m.m12;
		m22 = m.m22;
		m03 = m.m03;
		m13 = m.m13;
		m23 = m.m23;

		cost = m.cost;

		invert();
	}

	@Override
	public RigidModel3D copy()
	{
		final RigidModel3D m = new RigidModel3D();
		m.m00 = m00;
		m.m10 = m10;
		m.m20 = m20;
		m.m01 = m01;
		m.m11 = m11;
		m.m21 = m21;
		m.m02 = m02;
		m.m12 = m12;
		m.m22 = m22;
		m.m03 = m03;
		m.m13 = m13;
		m.m23 = m23;

		m.cost = cost;

		m.invert();

		return m;
	}

	protected void invert()
	{
		final double det = Matrix3x3.det( m00, m01, m02, m10, m11, m12, m20, m21, m22 );
		if ( det == 0 )
		{
			isInvertible = false;
			return;
		}

		isInvertible = true;

		final double idet = 1.0 / det;

		i00 = ( m11 * m22 - m12 * m21 ) * idet;
		i01 = ( m02 * m21 - m01 * m22 ) * idet;
		i02 = ( m01 * m12 - m02 * m11 ) * idet;
		i10 = ( m12 * m20 - m10 * m22 ) * idet;
		i11 = ( m00 * m22 - m02 * m20 ) * idet;
		i12 = ( m02 * m10 - m00 * m12 ) * idet;
		i20 = ( m10 * m21 - m11 * m20 ) * idet;
		i21 = ( m01 * m20 - m00 * m21 ) * idet;
		i22 = ( m00 * m11 - m01 * m10 ) * idet;

		i03 = -i00 * m03 - i01 * m13 - i02 * m23;
		i13 = -i10 * m03 - i11 * m13 - i12 * m23;
		i23 = -i20 * m03 - i21 * m13 - i22 * m23;
	}

	@Override
	final public void preConcatenate( final RigidModel3D model )
	{
		final double a00 = model.m00 * m00 + model.m01 * m10 + model.m02 * m20;
		final double a01 = model.m00 * m01 + model.m01 * m11 + model.m02 * m21;
		final double a02 = model.m00 * m02 + model.m01 * m12 + model.m02 * m22;
		final double a03 = model.m00 * m03 + model.m01 * m13 + model.m02 * m23 + model.m03;

		final double a10 = model.m10 * m00 + model.m11 * m10 + model.m12 * m20;
		final double a11 = model.m10 * m01 + model.m11 * m11 + model.m12 * m21;
		final double a12 = model.m10 * m02 + model.m11 * m12 + model.m12 * m22;
		final double a13 = model.m10 * m03 + model.m11 * m13 + model.m12 * m23 + model.m13;

		final double a20 = model.m20 * m00 + model.m21 * m10 + model.m22 * m20;
		final double a21 = model.m20 * m01 + model.m21 * m11 + model.m22 * m21;
		final double a22 = model.m20 * m02 + model.m21 * m12 + model.m22 * m22;
		final double a23 = model.m20 * m03 + model.m21 * m13 + model.m22 * m23 + model.m23;

		m00 = a00;
		m01 = a01;
		m02 = a02;
		m03 = a03;

		m10 = a10;
		m11 = a11;
		m12 = a12;
		m13 = a13;

		m20 = a20;
		m21 = a21;
		m22 = a22;
		m23 = a23;

		invert();
	}

	@Override
	final public void concatenate( final RigidModel3D model )
	{
		final double a00 = m00 * model.m00 + m01 * model.m10 + m02 * model.m20;
		final double a01 = m00 * model.m01 + m01 * model.m11 + m02 * model.m21;
		final double a02 = m00 * model.m02 + m01 * model.m12 + m02 * model.m22;
		final double a03 = m00 * model.m03 + m01 * model.m13 + m02 * model.m23 + m03;

		final double a10 = m10 * model.m00 + m11 * model.m10 + m12 * model.m20;
		final double a11 = m10 * model.m01 + m11 * model.m11 + m12 * model.m21;
		final double a12 = m10 * model.m02 + m11 * model.m12 + m12 * model.m22;
		final double a13 = m10 * model.m03 + m11 * model.m13 + m12 * model.m23 + m13;

		final double a20 = m20 * model.m00 + m21 * model.m10 + m22 * model.m20;
		final double a21 = m20 * model.m01 + m21 * model.m11 + m22 * model.m21;
		final double a22 = m20 * model.m02 + m21 * model.m12 + m22 * model.m22;
		final double a23 = m20 * model.m03 + m21 * model.m13 + m22 * model.m23 + m23;

		m00 = a00;
		m01 = a01;
		m02 = a02;
		m03 = a03;

		m10 = a10;
		m11 = a11;
		m12 = a12;
		m13 = a13;

		m20 = a20;
		m21 = a21;
		m22 = a22;
		m23 = a23;

		invert();
	}

	final public void concatenate( final TranslationModel3D model )
	{
		final double[] t = model.getTranslation();

		m03 = m00 * t[ 0 ] + m01 * t[ 1 ] + m02 * t[ 2 ] + m03;
		m13 = m10 * t[ 0 ] + m11 * t[ 1 ] + m12 * t[ 2 ] + m13;
		m23 = m20 * t[ 0 ] + m21 * t[ 1 ] + m22 * t[ 2 ] + m23;

		invert();
	}

	final public void preConcatenate( final TranslationModel3D model )
	{
		final double[] t = model.getTranslation();

		m03 += t[ 0 ];
		m13 += t[ 1 ];
		m23 += t[ 2 ];

		invert();
	}

	/**
	 * Initialize the model such that the respective affine transform is:
	 *
	 * <pre>
	 * m00 m01 m02 m03
	 * m10 m11 m12 m13
	 * m20 m21 m22 m23
	 * 0   0   0   1
	 * </pre>
	 *
	 * @param m00
	 * @param m01
	 * @param m02
	 * @param m03
	 *
	 * @param m10
	 * @param m11
	 * @param m12
	 * @param m13
	 *
	 * @param m20
	 * @param m21
	 * @param m22
	 * @param m23
	 */
	final public void set(
			final double m00, final double m01, final double m02, final double m03,
			final double m10, final double m11, final double m12, final double m13,
			final double m20, final double m21, final double m22, final double m23 )
	{
		this.m00 = m00;
		this.m01 = m01;
		this.m02 = m02;
		this.m03 = m03;

		this.m10 = m10;
		this.m11 = m11;
		this.m12 = m12;
		this.m13 = m13;

		this.m20 = m20;
		this.m21 = m21;
		this.m22 = m22;
		this.m23 = m23;

		invert();
	}

	/**
	 * TODO Not yet tested
	 */
	@Override
	public RigidModel3D createInverse()
	{
		final RigidModel3D ict = new RigidModel3D();

		ict.m00 = i00;
		ict.m10 = i10;
		ict.m20 = i20;
		ict.m01 = i01;
		ict.m11 = i11;
		ict.m21 = i21;
		ict.m02 = i02;
		ict.m12 = i12;
		ict.m22 = i22;
		ict.m03 = i03;
		ict.m13 = i13;
		ict.m23 = i23;

		ict.i00 = m00;
		ict.i10 = m10;
		ict.i20 = m20;
		ict.i01 = m01;
		ict.i11 = m11;
		ict.i21 = m21;
		ict.i02 = m02;
		ict.i12 = m12;
		ict.i22 = m22;
		ict.i03 = m03;
		ict.i13 = m13;
		ict.i23 = m23;

		ict.cost = cost;

		ict.isInvertible = isInvertible;

		return ict;
	}

	/**
	 * Rotate
	 *
	 * @param axis 0=x, 1=y, 2=z
	 * @param d angle in radians
	 *
	 * TODO Don't be lazy and do it directly on the values instead of creating another transform
	 */
	public void rotate( final int axis, final double d )
	{
		final double dcos = Math.cos( d );
		final double dsin = Math.sin( d );
		final RigidModel3D dR = new RigidModel3D();

		switch ( axis )
		{
		case 0:
			dR.set(
					1.0f, 0.0f, 0.0f, 0.0f,
					0.0f, dcos, -dsin, 0.0f,
					0.0f, dsin, dcos, 0.0f );
			break;
		case 1:
			dR.set(
					dcos, 0.0f, dsin, 0.0f,
					0.0f, 1.0f, 0.0f, 0.0f,
					-dsin, 0.0f, dcos, 0.0f );
			break;
		default:
			dR.set(
					dcos, -dsin, 0.0f, 0.0f,
					dsin, dcos, 0.0f, 0.0f,
					0.0f, 0.0f, 1.0f, 0.0f );
			break;
		}

		preConcatenate( dR );
	}

	@Override
	public void toArray( final double[] data )
	{
		data[ 0 ] = m00;
		data[ 1 ] = m10;
		data[ 2 ] = m20;
		data[ 3 ] = m01;
		data[ 4 ] = m11;
		data[ 5 ] = m21;
		data[ 6 ] = m02;
		data[ 7 ] = m12;
		data[ 8 ] = m22;
		data[ 9 ] = m03;
		data[ 10 ] = m13;
		data[ 11 ] = m23;
	}

	@Override
	public void toMatrix( final double[][] data )
	{
		data[ 0 ][ 0 ] = m00;
		data[ 0 ][ 1 ] = m01;
		data[ 0 ][ 2 ] = m02;
		data[ 0 ][ 3 ] = m03;
		data[ 1 ][ 0 ] = m10;
		data[ 1 ][ 1 ] = m11;
		data[ 1 ][ 2 ] = m12;
		data[ 1 ][ 3 ] = m13;
		data[ 2 ][ 0 ] = m20;
		data[ 2 ][ 1 ] = m21;
		data[ 2 ][ 2 ] = m22;
		data[ 2 ][ 3 ] = m23;
	}
}
