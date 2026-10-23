package mpicbg.models;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

class HomographyModel2DTest
{
	/** ground truth homography with small perspective terms at pixel scale */
	private static final double[] H = {
			1.05, -0.08, 120.0,
			0.12, 0.97, -45.0,
			2.0e-4, -1.5e-4, 1.0 };

	private static HomographyModel2D groundTruth()
	{
		final HomographyModel2D m = new HomographyModel2D();
		m.set( H[ 0 ], H[ 1 ], H[ 2 ], H[ 3 ], H[ 4 ], H[ 5 ], H[ 6 ], H[ 7 ], H[ 8 ] );
		return m;
	}

	private static double[] matrix( final HomographyModel2D m )
	{
		final double[] a = {
				m.m00, m.m01, m.m02,
				m.m10, m.m11, m.m12,
				m.m20, m.m21, m.m22 };
		for ( int i = 0; i < 9; ++i )
			a[ i ] /= m.m22;
		return a;
	}

	private static List< PointMatch > matches( final HomographyModel2D m, final int n, final Random rnd, final double weight )
	{
		final List< PointMatch > matches = new ArrayList<>();
		for ( int i = 0; i < n; ++i )
		{
			final double[] p = { rnd.nextDouble() * 2000, rnd.nextDouble() * 2000 };
			final double[] q = m.apply( p );
			matches.add( new PointMatch( new Point( p ), new Point( q ), weight ) );
		}
		return matches;
	}

	private static void assertTransfer( final HomographyModel2D truth, final HomographyModel2D fitted, final Random rnd, final double tol )
	{
		for ( int i = 0; i < 50; ++i )
		{
			final double[] p = { rnd.nextDouble() * 2000, rnd.nextDouble() * 2000 };
			final double[] q = truth.apply( p );
			final double[] f = fitted.apply( p );
			assertEquals( q[ 0 ], f[ 0 ], tol );
			assertEquals( q[ 1 ], f[ 1 ], tol );
		}
	}

	@Test
	void applyInverseRoundTrip() throws NoninvertibleModelException
	{
		final HomographyModel2D m = groundTruth();
		final double[] p = { 321.5, 987.25 };
		final double[] q = m.apply( p );
		final double[] back = m.applyInverse( q );
		assertNotNull( back );
		assertArrayEquals( p, back, 1e-9 );
	}

	@Test
	void createInverseMatchesApplyInverse() throws NoninvertibleModelException
	{
		final HomographyModel2D m = groundTruth();
		final HomographyModel2D inv = m.createInverse();
		final Random rnd = new Random( 1 );
		for ( int i = 0; i < 20; ++i )
		{
			final double[] p = { rnd.nextDouble() * 2000, rnd.nextDouble() * 2000 };
			assertArrayEquals( m.applyInverse( p ), inv.apply( p ), 1e-9 );
			assertArrayEquals( m.apply( p ), inv.applyInverse( p ), 1e-9 );
		}
		assertArrayEquals( matrix( m ), matrix( inv.createInverse() ), 1e-9 );
	}

	@Test
	void fitExactFourPoints() throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		final HomographyModel2D truth = groundTruth();
		final Random rnd = new Random( 2 );
		final HomographyModel2D m = new HomographyModel2D();
		m.fit( matches( truth, 4, rnd, 1.0 ) );
		assertArrayEquals( matrix( truth ), matrix( m ), 1e-9 );
		assertTransfer( truth, m, rnd, 1e-6 );
	}

	@Test
	void fitOverDeterminedAtPixelScale() throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		final HomographyModel2D truth = groundTruth();
		final Random rnd = new Random( 3 );
		final HomographyModel2D m = new HomographyModel2D();
		m.fit( matches( truth, 20, rnd, 1.0 ) );
		assertArrayEquals( matrix( truth ), matrix( m ), 1e-6 );
		assertTransfer( truth, m, rnd, 1e-6 );
	}

	@Test
	void fitHonorsWeights() throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		final HomographyModel2D truth = groundTruth();
		final Random rnd = new Random( 4 );
		final List< PointMatch > matches = matches( truth, 20, rnd, 1.0 );

		// gross outliers with zero weight must not influence the fit
		for ( int i = 0; i < 5; ++i )
		{
			final double[] p = { rnd.nextDouble() * 2000, rnd.nextDouble() * 2000 };
			final double[] q = { rnd.nextDouble() * 2000, rnd.nextDouble() * 2000 };
			matches.add( new PointMatch( new Point( p ), new Point( q ), 0.0 ) );
		}

		final HomographyModel2D m = new HomographyModel2D();
		m.fit( matches );
		assertArrayEquals( matrix( truth ), matrix( m ), 1e-6 );
		assertTransfer( truth, m, rnd, 1e-6 );

		// the same outliers with full weight must corrupt the fit, proving the weights were used
		final List< PointMatch > unweighted = new ArrayList<>();
		for ( final PointMatch pm : matches )
			unweighted.add( new PointMatch( pm.getP1(), pm.getP2(), 1.0 ) );
		final HomographyModel2D c = new HomographyModel2D();
		c.fit( unweighted );
		final double[] p = { 1000, 1000 };
		final double[] q = truth.apply( p );
		final double[] f = c.apply( p );
		assertTrue( Math.hypot( q[ 0 ] - f[ 0 ], q[ 1 ] - f[ 1 ] ) > 1.0 );
	}
}
