package mpicbg.models;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

import java.util.Collection;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.PointMatch;
import mpicbg.util.Matrix3x3;

/**
 * 3d-rigid transformation models to be applied to points in 3d-space.
 * 
 * This function uses the method by Horn, using quaternions:
 * Closed-form solution of absolute orientation using unit quaternions,
 * Horn, B. K. P., Journal of the Optical Society of America A,
 * Vol. 4, page 629, April 1987
 * 
 * @author John Bogovic
 * @author Johannes Schindelin (quaternion logic and implementation) 
 * @aurthor Stephan Preibisch
 * @version 0.1b
 * 
 */
public class SimilarityModel3D extends AbstractAffineModel3D< SimilarityModel3D > implements InvertibleBoundable 
{
	private static final long serialVersionUID = 5509363764217496393L;

	static final protected int MIN_NUM_MATCHES = 3;
	
	protected double
		m00 = 1.0, m01 = 0.0, m02 = 0.0, m03 = 0.0, 
		m10 = 0.0, m11 = 1.0, m12 = 0.0, m13 = 0.0, 
		m20 = 0.0, m21 = 0.0, m22 = 1.0, m23 = 0.0;
	
	protected double
		i00 = 1.0, i01 = 0.0, i02 = 0.0, i03 = 0.0,
		i10 = 0.0, i11 = 1.0, i12 = 0.0, i13 = 0.0,
		i20 = 0.0, i21 = 0.0, i22 = 1.0, i23 = 0.0;

	final protected double[][] N = new double[4][4];

	protected boolean isInvertible;
	
	public boolean canDoNumDimension( final int numDimensions ) { return numDimensions == 3; }

	final public void fit( 
			float[][] p,
			float[][] q,
			float[] w )
		throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		final int numMatches = p[ 0 ].length; 
		if ( numMatches < MIN_NUM_MATCHES )
			throw new NotEnoughDataPointsException( p[ 0 ].length + " data points are not enough to estimate a 3d similarity model, at least " + MIN_NUM_MATCHES + " data points required." );

		double pcx, pcy, pcz, qcx, qcy, qcz;
		pcx = pcy = pcz = qcx = qcy = qcz = 0;

		double ws = 0.0; // sum of weights
		
		for ( int i = 0; i < numMatches; i++ )
		{
			
			final double weight = w[ i ];
			
			ws += weight;
			pcx += weight * p[ 0 ][ i ];
			pcy += weight * p[ 1 ][ i ];
			pcz += weight * p[ 2 ][ i ];
			qcx += weight * q[ 0 ][ i ];
			qcy += weight * q[ 1 ][ i ];
			qcz += weight * q[ 2 ][ i ];
		}
		
		pcx /= ws;
		pcy /= ws;
		pcz /= ws;
		qcx /= ws;
		qcy /= ws;
		qcz /= ws;

		double r1 = 0, r2 = 0;
		for ( int i = 0; i < numMatches; i++ )
		{
			double x1 = p[ 0 ][ i ] - pcx;
			double y1 = p[ 1 ][ i ] - pcy;
			double z1 = p[ 2 ][ i ] - pcz;
			double x2 = q[ 0 ][ i ] - qcx;
			double y2 = q[ 1 ][ i ] - qcy;
			double z2 = q[ 2 ][ i ] - qcz;
			r1 += x1 * x1 + y1 * y1 + z1 * z1;
			r2 += x2 * x2 + y2 * y2 + z2 * z2;
		}
		final double s = Math.sqrt(r2 / r1);
		
		// calculate N
		double Sxx, Sxy, Sxz, Syx, Syy, Syz, Szx, Szy, Szz;
		Sxx = Sxy = Sxz = Syx = Syy = Syz = Szx = Szy = Szz = 0;
		for ( int i = 0; i < numMatches; i++ )
		{
			final double x1 = (p[ 0 ][ i ] - pcx) * s;
			final double y1 = (p[ 1 ][ i ] - pcy) * s;
			final double z1 = (p[ 2 ][ i ] - pcz) * s;
			final double x2 =  q[ 0 ][ i ] - qcx;
			final double y2 =  q[ 1 ][ i ] - qcy;
			final double z2 =  q[ 2 ][ i ] - qcz;
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
		
		computeN( N, Sxx, Sxz, Sxy, Syx, Syy, Syz, Szx, Szy, Szz );

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

		// compute result
		rotationTranslationPart( 
				s, q0, qx, qy, qz, 
				pcx, pcy, pcz, qcx, qcy, qcz);
		
		invert();
	}
	
	final public void fit( 
			double[][] p,
			double[][] q,
			double[] w )
		throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		final int numMatches = p[ 0 ].length; 
		if ( numMatches < MIN_NUM_MATCHES )
			throw new NotEnoughDataPointsException( p[ 0 ].length + " data points are not enough to estimate a 3d similarity model, at least " + MIN_NUM_MATCHES + " data points required." );

		double pcx, pcy, pcz, qcx, qcy, qcz;
		pcx = pcy = pcz = qcx = qcy = qcz = 0;

		double ws = 0.0; // sum of weights
		
		for ( int i = 0; i < numMatches; i++ )
		{
			
			final double weight = w[ i ];
			
			ws += weight;
			pcx += weight * p[ 0 ][ i ];
			pcy += weight * p[ 1 ][ i ];
			pcz += weight * p[ 2 ][ i ];
			qcx += weight * q[ 0 ][ i ];
			qcy += weight * q[ 1 ][ i ];
			qcz += weight * q[ 2 ][ i ];
		}
		
		pcx /= ws;
		pcy /= ws;
		pcz /= ws;
		qcx /= ws;
		qcy /= ws;
		qcz /= ws;

		double r1 = 0, r2 = 0;
		for ( int i = 0; i < numMatches; i++ )
		{
			double x1 = p[ 0 ][ i ] - pcx;
			double y1 = p[ 1 ][ i ] - pcy;
			double z1 = p[ 2 ][ i ] - pcz;
			double x2 = q[ 0 ][ i ] - qcx;
			double y2 = q[ 1 ][ i ] - qcy;
			double z2 = q[ 2 ][ i ] - qcz;
			r1 += x1 * x1 + y1 * y1 + z1 * z1;
			r2 += x2 * x2 + y2 * y2 + z2 * z2;
		}
		final double s = Math.sqrt(r2 / r1);
		
		// calculate N
		double Sxx, Sxy, Sxz, Syx, Syy, Syz, Szx, Szy, Szz;
		Sxx = Sxy = Sxz = Syx = Syy = Syz = Szx = Szy = Szz = 0;
		for ( int i = 0; i < numMatches; i++ )
		{
			final double x1 = (p[ 0 ][ i ] - pcx) * s;
			final double y1 = (p[ 1 ][ i ] - pcy) * s;
			final double z1 = (p[ 2 ][ i ] - pcz) * s;
			final double x2 =  q[ 0 ][ i ] - qcx;
			final double y2 =  q[ 1 ][ i ] - qcy;
			final double z2 =  q[ 2 ][ i ] - qcz;
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
		
		computeN( N, Sxx, Sxz, Sxy, Syx, Syy, Syz, Szx, Szy, Szz );

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

		// compute result
		rotationTranslationPart( 
				s, q0, qx, qy, qz, 
				pcx, pcy, pcz, qcx, qcy, qcz);
		
		invert();
	}
	
	@Override
	final public <P extends PointMatch> void fit( final Collection< P > matches )
		throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		final int numMatches = matches.size(); 
		if ( numMatches < MIN_NUM_MATCHES )
			throw new NotEnoughDataPointsException( matches.size() + " data points are not enough to estimate a 3d similarity model, at least " + MIN_NUM_MATCHES + " data points required." );

		double pcx, pcy, pcz, qcx, qcy, qcz;
		pcx = pcy = pcz = qcx = qcy = qcz = 0;

		double ws = 0.0; // sum of weights
		
		for ( final PointMatch m : matches )
		{
			final double[] p = m.getP1().getL(); 
			final double[] q = m.getP2().getW();
			final double w = m.getWeight();
			
			ws += w;
			pcx += w * p[ 0 ];
			pcy += w * p[ 1 ];
			pcz += w * p[ 2 ];
			qcx += w * q[ 0 ];
			qcy += w * q[ 1 ];
			qcz += w * q[ 2 ];
		}
		pcx /= ws;
		pcy /= ws;
		pcz /= ws;
		qcx /= ws;
		qcy /= ws;
		qcz /= ws;

		double r1 = 0, r2 = 0;
		for ( final PointMatch m : matches )
		{
			final double[] p = m.getP1().getL(); 
			final double[] q = m.getP2().getW();
			
			double x1 = p[ 0 ] - pcx;
			double y1 = p[ 1 ] - pcy;
			double z1 = p[ 2 ] - pcz;
			double x2 = q[ 0 ] - qcx;
			double y2 = q[ 1 ] - qcy;
			double z2 = q[ 2 ] - qcz;
			r1 += x1 * x1 + y1 * y1 + z1 * z1;
			r2 += x2 * x2 + y2 * y2 + z2 * z2;
		}
		final double s = Math.sqrt(r2 / r1);
		
		// calculate N
		double Sxx, Sxy, Sxz, Syx, Syy, Syz, Szx, Szy, Szz;
		Sxx = Sxy = Sxz = Syx = Syy = Syz = Szx = Szy = Szz = 0;
		for ( final PointMatch m : matches )
		{
			final double[] p = m.getP1().getL(); 
			final double[] q = m.getP2().getW();
			
			final double x1 = (p[ 0 ] - pcx) * s;
			final double y1 = (p[ 1 ] - pcy) * s;
			final double z1 = (p[ 2 ] - pcz) * s;
			final double x2 = q[ 0 ] - qcx;
			final double y2 = q[ 1 ] - qcy;
			final double z2 = q[ 2 ] - qcz;
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

		computeN( N, Sxx, Sxz, Sxy, Syx, Syy, Syz, Szx, Szy, Szz );

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

		// compute result
		rotationTranslationPart( 
				s, q0, qx, qy, qz, 
				pcx, pcy, pcz, qcx, qcy, qcz);
		
		invert();
	}
	
	private void rotationTranslationPart( double s, double q0, double qx, double qy, double qz, 
			double pcx, double pcy, double pcz, double qcx, double qcy, double qcz )
	{
		// rotational part
		m00 = s * (q0 * q0 + qx * qx - qy * qy - qz * qz);
		m01 = s * 2 * (qx * qy - q0 * qz);
		m02 = s * 2 * (qx * qz + q0 * qy);
		m10 = s * 2 * (qy * qx + q0 * qz);
		m11 = s * (q0 * q0 - qx * qx + qy * qy - qz * qz);
		m12 = s * 2 * (qy * qz - q0 * qx);
		m20 = s * 2 * (qz * qx - q0 * qy);
		m21 = s * 2 * (qz * qy + q0 * qx);
		m22 = s * (q0 * q0 - qx * qx - qy * qy + qz * qz);
		
		double resx = 0.0, resy = 0.0, resz = 0.0;
		resx = pcx * m00 + pcy * m01 + pcz * m02;
		resy = pcx * m10 + pcy * m11 + pcz * m12;
		resz = pcx * m20 + pcy * m21 + pcz * m22;
		
		m03 = qcx - resx;
		m13 = qcy - resy;
		m23 = qcz - resz;
		
	}
	
	private static void computeN( double[][] N, double Sxx, double Sxz, double Sxy, double Syx, double Syy, double Syz, double Szx, double Szy, double Szz )
	{
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
	}
	
	@Override
	final public void set( final SimilarityModel3D m )
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
	public SimilarityModel3D copy()
	{
		SimilarityModel3D m = new SimilarityModel3D();
		m.m00 = m00;
		m.m10 = m10;
		m.m20 = m20;
		m.m01 = m01;
		m.m11 = m11;
		m.m21 = m21;
		m.m02 = m02;
		m.m12 = m12;
		m.m22 = m22;
		m03 = m.m03;
		m13 = m.m13;
		m23 = m.m23;
		
		m.cost = cost;
		invert();
		
		return m;
	}
	
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
		assert l.length == 3 : "3d affine transformations can be applied to 3d points only.";
		
		final double l0 = l[ 0 ];
		final double l1 = l[ 1 ];
		l[ 0 ] = l0 * m00 + l1 * m01 + l[ 2 ] * m02 + m03;
		l[ 1 ] = l0 * m10 + l1 * m11 + l[ 2 ] * m12 + m13;
		l[ 2 ] = l0 * m20 + l1 * m21 + l[ 2 ] * m22 + m23;
	}
	
	final public String toString()
	{
		return
			"3d-affine: (" +
			m00 + ", " + m01 + ", " + m02 + ", " + m03 + ", " + 
			m10 + ", " + m11 + ", " + m12 + ", " + m13 + ", " + 
			m20 + ", " + m21 + ", " + m22 + ", " + m23 + ")";
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
	public double[] applyInverse( double[] l )
			throws NoninvertibleModelException 
	{
		final double[] transformed = l.clone();
		applyInverseInPlace( transformed );
		return transformed;
	}

	@Override
	public void applyInverseInPlace(double[] l)
			throws NoninvertibleModelException 
	{
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

	public void preConcatenate( final SimilarityModel3D model) 
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

	public void concatenate( SimilarityModel3D model) 
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

	@Override
	public void toArray(double[] data) 
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
	public void toMatrix(double[][] data)
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

	@Override
	public SimilarityModel3D createInverse() 
	{
		final SimilarityModel3D ict = new SimilarityModel3D();
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

	@Override
	public double[] getMatrix(double[] m) 
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

	
}
