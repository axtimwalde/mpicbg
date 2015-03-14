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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/**
 * Abstract class for arbitrary transformation models to be applied
 * to {@link Point Points} in n-dimensional space.
 *
 * Model: R^n --> R^n
 *
 * Provides methods for generic optimization and model extraction algorithms.
 * Currently, the Random Sample Consensus \cite{FischlerB81}, a robust
 * regression method and the Iterative Closest Point Algorithm \cite{Zhang94}
 * are implemented.
 *
 * BibTeX:
 * <pre>
 * &#64;article{FischlerB81,
 *	 author    = {Martin A. Fischler and Robert C. Bolles},
 *   title     = {Random sample consensus: a paradigm for model fitting with applications to image analysis and automated cartography},
 *   journal   = {Communications of the ACM},
 *   volume    = {24},
 *   number    = {6},
 *   year      = {1981},
 *   pages     = {381--395},
 *   publisher = {ACM Press},
 *   address   = {New York, NY, USA},
 *   issn      = {0001-0782},
 *   doi       = {http://doi.acm.org/10.1145/358669.358692},
 * }
 * &#64;article{Zhang94,
 *   author    = {{Zhengyou Zhang}},
 *   title     = {Iterative point matching for registration of free-form curves and surfaces},
 *   journal   = {International Journal of Computer Vision},
 *   volume    = {13},
 *   number    = {2},
 *   month     = {October},
 *   year      = {1994},
 *   pages     = {119--152},
 * }
 * </pre>
 *
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
public abstract class AbstractModel< M extends AbstractModel< M > > implements Model< M >, Serializable
{
	private static final long serialVersionUID = -4649306649450399752L;

	/* real random */
//	final Random random = new Random( System.currentTimeMillis() );

	/* repeatable results */
	final static protected Random rnd = new Random( 69997 );

	/**
	 * The cost depends on what kind of algorithm is running.  It is always
	 * true that a smaller cost is better than large cost
	 */
	protected double cost = Double.MAX_VALUE;
	@Override
	final public double getCost(){ return cost; }
	@Override
	final public void setCost( final double c ){ cost = c; }

	/**
	 * "Less than" operater to make {@link AbstractModel Models} comparable.
	 *
	 * @param m
	 * @return false for {@link #cost} < 0.0, otherwise true if
	 *   {@link #cost this.cost} is smaller than {@link #cost m.cost}
	 */
	@Override
	final public boolean betterThan( final M m )
	{
		if ( cost < 0 ) return false;
		return cost < m.cost;
	}

	/**
	 * Test the {@link AbstractModel} for a set of {@link PointMatch} candidates.
	 * Return true if the number of inliers / number of candidates is larger
	 * than or equal to min_inlier_ratio, otherwise false.
	 *
	 * Clears inliers and fills it with the fitting subset of candidates.
	 *
	 * Sets {@link #getCost() cost} = 1.0 - |inliers| / |candidates|.
	 *
	 * @param candidates set of point correspondence candidates
	 * @param inliers set of point correspondences that fit the model
	 * @param epsilon maximal allowed transfer error
	 * @param minInlierRatio minimal ratio |inliers| / |candidates| (0.0 => 0%, 1.0 => 100%)
	 * @param minNumInliers minimally required absolute number of inliers
	 */
	@Override
	final public < P extends PointMatch >boolean test(
			final Collection< P > candidates,
			final Collection< P > inliers,
			final double epsilon,
			final double minInlierRatio,
			final int minNumInliers )
	{
		inliers.clear();

		for ( final P m : candidates )
		{
			m.apply( this );
			if ( m.getDistance() < epsilon ) inliers.add( m );
		}

		final double ir = ( double )inliers.size() / ( double )candidates.size();
		setCost( Math.max( 0.0, Math.min( 1.0, 1.0 - ir ) ) );

		return ( inliers.size() >= minNumInliers && ir > minInlierRatio );
	}

	/**
	 * Call {@link #test(Collection, Collection, double, double, int)} with
	 * minNumInliers = {@link #getMinNumMatches()}.
	 */
	@Override
	final public < P extends PointMatch >boolean test(
			final Collection< P > candidates,
			final Collection< P > inliers,
			final double epsilon,
			final double minInlierRatio )
	{
		return test( candidates, inliers, epsilon, minInlierRatio, getMinNumMatches() );
	}

	/**
	 * Estimate the {@link AbstractModel} and filter potential outliers by robust
	 * iterative regression.
	 *
	 * This method performs well on data sets with low amount of outliers.  If
	 * you have many outliers, you can filter those with a `tolerant' RANSAC
	 * first as done in {@link #filterRansac() filterRansac}.
	 *
	 * Sets {@link #getCost() cost} to the average point transfer error.
	 *
	 * @param candidates Candidate data points eventually inluding some outliers
	 * @param inliers Remaining after the robust regression filter
	 * @param maxTrust reject candidates with a cost larger than
	 *   maxTrust * median cost
	 * @param minNumInliers minimally required absolute number of inliers
	 *
	 * @return true if {@link AbstractModel} could be estimated and inliers is not
	 *   empty, false otherwise.  If false, {@link AbstractModel} remains unchanged.
	 */
	@Override
	final public < P extends PointMatch >boolean filter(
			final Collection< P > candidates,
			final Collection< P > inliers,
			final double maxTrust,
			final int minNumInliers )
		throws NotEnoughDataPointsException
	{
		if ( candidates.size() < getMinNumMatches() )
			throw new NotEnoughDataPointsException( candidates.size() + " data points are not enough to solve the Model, at least " + getMinNumMatches() + " data points required." );

		final M copy = copy();

		inliers.clear();
		inliers.addAll( candidates );
		final ArrayList< P > temp = new ArrayList< P >();
		int numInliers;
		do
		{
			temp.clear();
			temp.addAll( inliers );
			numInliers = inliers.size();
			try
			{
				copy.fit( inliers );
			}
			catch ( final NotEnoughDataPointsException e )
			{
				return false;
			}
			catch ( final IllDefinedDataPointsException e )
			{
				return false;
			}
			final ErrorStatistic observer = new ErrorStatistic( temp.size() );
			for ( final PointMatch m : temp )
			{
				m.apply( copy );
				observer.add( m.getDistance() );
			}
			inliers.clear();
			final double t = observer.getMedian() * maxTrust;
			for ( final P m : temp )
			{
				if ( m.getDistance() <= t )
					inliers.add( m );
			}

			copy.cost = observer.mean;
		}
		while ( numInliers > inliers.size() );

		if ( numInliers < minNumInliers )
			return false;

		set( copy );
		return true;
	}

	/**
	 * Call {@link #filter(Collection, Collection, double, int)} with minNumInliers = {@link AbstractModel#getMinNumMatches()}.
	 */
	@Override
	final public < P extends PointMatch >boolean filter(
			final Collection< P > candidates,
			final Collection< P > inliers,
			final double maxTrust )
		throws NotEnoughDataPointsException
	{
		return filter( candidates, inliers, maxTrust, getMinNumMatches() );
	}


	/**
	 * Call {@link #filter(Collection, Collection, double)} with maxTrust = 4 and minNumInliers = {@link AbstractModel#getMinNumMatches()}.
	 */
	@Override
	final public < P extends PointMatch >boolean filter(
			final Collection< P > candidates,
			final Collection< P > inliers )
		throws NotEnoughDataPointsException
	{
		return filter( candidates, inliers, 4f, getMinNumMatches() );
	}


	/**
	 * Find the {@link AbstractModel} of a set of {@link PointMatch} candidates
	 * containing a high number of outliers using
	 * {@link #ransac(List, Collection, int, double, double, int) RANSAC}
	 * \citet[{FischlerB81}.
	 *
	 * @param modelClass class of the model to be estimated
	 * @param candidates candidate data points inluding (many) outliers
	 * @param inliers remaining candidates after RANSAC
	 * @param iterations number of iterations
	 * @param epsilon maximal allowed transfer error
	 * @param minInlierRatio minimal number of inliers to number of
	 *   candidates
	 * @param minNumInliers minimally required absolute number of inliers
	 *
	 * @return true if {@link AbstractModel} could be estimated and inliers is not
	 *   empty, false otherwise.  If false, {@link AbstractModel} remains unchanged.
	 */
	@Override
	final public < P extends PointMatch >boolean ransac(
			final List< P > candidates,
			final Collection< P > inliers,
			final int iterations,
			final double epsilon,
			final double minInlierRatio,
			final int minNumInliers )
		throws NotEnoughDataPointsException
	{
		if ( candidates.size() < getMinNumMatches() )
			throw new NotEnoughDataPointsException( candidates.size() + " data points are not enough to solve the Model, at least " + getMinNumMatches() + " data points required." );

		cost = Double.MAX_VALUE;

		final M copy = copy();
		final M m = copy();

		inliers.clear();

		int i = 0;
		final HashSet< P > minMatches = new HashSet< P >();

A:		while ( i < iterations )
		{
			// choose model.MIN_SET_SIZE disjunctive matches randomly
			minMatches.clear();
			for ( int j = 0; j < getMinNumMatches(); ++j )
			{
				P p;
				do
				{
					p = candidates.get( ( int )( rnd.nextDouble() * candidates.size() ) );
				}
				while ( minMatches.contains( p ) );
				minMatches.add( p );
			}
			try { m.fit( minMatches ); }
			catch ( final IllDefinedDataPointsException e )
			{
				++i;
				continue;
			}

			final ArrayList< P > tempInliers = new ArrayList< P >();

			int numInliers = 0;
			boolean isGood = m.test( candidates, tempInliers, epsilon, minInlierRatio );
			while ( isGood && numInliers < tempInliers.size() )
			{
				numInliers = tempInliers.size();
				try { m.fit( tempInliers ); }
				catch ( final IllDefinedDataPointsException e )
				{
					++i;
					continue A;
				}
				isGood = m.test( candidates, tempInliers, epsilon, minInlierRatio, minNumInliers );
			}
			if (
					isGood &&
					m.betterThan( copy ) &&
					tempInliers.size() >= minNumInliers )
			{
				copy.set( m );
				inliers.clear();
				inliers.addAll( tempInliers );
			}
			++i;
		}
		if ( inliers.size() == 0 )
			return false;

		set( copy );
		return true;
	}

	/**
	 * Call {@link #ransac(List, Collection, int, double, double, int)} with
	 * minNumInliers = {@link #getMinNumMatches()}.
	 */
	@Override
	final public < P extends PointMatch >boolean ransac(
			final List< P > candidates,
			final Collection< P > inliers,
			final int iterations,
			final double epsilon,
			final double minInlierRatio )
		throws NotEnoughDataPointsException
	{
		return ransac( candidates, inliers, iterations, epsilon, minInlierRatio, getMinNumMatches() );
	}

	/**
	 * Estimate a {@link AbstractModel} from a set with many outliers by first
	 * filtering the worst outliers with
	 * {@link #ransac(Class, List, Collection, int, double, double) RANSAC}
	 * \citet[{FischlerB81} and filter potential outliers by robust iterative
	 * regression.
	 *
	 * @param candidates candidate data points inluding (many) outliers
	 * @param inliers remaining candidates after RANSAC
	 * @param iterations number of iterations
	 * @param maxEpsilon maximal allowed transfer error
	 * @param minInlierRatio minimal number of inliers to number of
	 *   candidates
	 * @param minNumInliers minimally required absolute number of inliers
	 * @param maxTrust reject candidates with a cost larger than
	 *   maxTrust * median cost
	 *
	 * @return true if {@link AbstractModel} could be estimated and inliers is not
	 *   empty, false otherwise.  If false, {@link AbstractModel} remains unchanged.
	 */
	@Override
	final public < P extends PointMatch >boolean filterRansac(
			final List< P > candidates,
			final Collection< P > inliers,
			final int iterations,
			final double maxEpsilon,
			final double minInlierRatio,
			final int minNumInliers,
			final double maxTrust )
		throws NotEnoughDataPointsException
	{
		final ArrayList< P > temp = new ArrayList< P >();
		if (
				ransac(
						candidates,
						temp,
						iterations,
						maxEpsilon,
						minInlierRatio,
						minNumInliers ) &&
				filter( temp, inliers, maxTrust, minNumInliers ) )
			return true;
		return false;
	}

	/**
	 * Call {@link #filterRansac(List, Collection, int, double, double, int, double)}
	 * with maxTrust = 4.
	 */
	@Override
	final public < P extends PointMatch >boolean filterRansac(
			final List< P > candidates,
			final Collection< P > inliers,
			final int iterations,
			final double maxEpsilon,
			final double minInlierRatio,
			final int minNumInliers )
		throws NotEnoughDataPointsException
	{
		return filterRansac( candidates, inliers, iterations, maxEpsilon, minInlierRatio, minNumInliers, 4f );
	}


	/**
	 * Call {@link #filterRansac(List, Collection, int, double, double, int, double)}
	 * with minNumInliers = {@link #getMinNumMatches()}.
	 */
	@Override
	final public < P extends PointMatch >boolean filterRansac(
			final List< P > candidates,
			final Collection< P > inliers,
			final int iterations,
			final double maxEpsilon,
			final double minInlierRatio,
			final double maxTrust )
		throws NotEnoughDataPointsException
	{
		return filterRansac( candidates, inliers, iterations, maxEpsilon, minInlierRatio, getMinNumMatches(), maxTrust );
	}


	/**
	 * Call {@link #filterRansac(List, Collection, int, double, double, double)}
	 * with maxTrust = 4.
	 */
	@Override
	final public < P extends PointMatch >boolean filterRansac(
			final List< P > candidates,
			final Collection< P > inliers,
			final int iterations,
			final double maxEpsilon,
			final double minInlierRatio )
		throws NotEnoughDataPointsException
	{
		return filterRansac( candidates, inliers, iterations, maxEpsilon, minInlierRatio, 4f );
	}


	/**
	 * Estimate the best model in terms of the Iterative Closest Point
	 * Algorithm \cite{Zhang94} for matching two point clouds into each other.
	 *
	 * p -> q
	 *
	 * @param p source
	 * @param q target
	 *
	 * @return the list of matches
	 *
	 * TODO Test---at least once!
	 */
	@Override
	final public Collection< PointMatch > icp(
			final List< Point > p,
			final List< Point > q )
	{
		final M m = copy();
		final List< PointMatch > currentMatches = new ArrayList< PointMatch >();
		final List< PointMatch > previousMatches = new ArrayList< PointMatch >();
		do
		{
			previousMatches.clear();
			previousMatches.addAll( currentMatches );
			currentMatches.clear();

			/* Match by Euclidean distance in space */
			for ( final Point pi : p )
			{
				double minimalDistance = Double.MAX_VALUE;
				Point closestPoint = null;
				for ( final Point qi : q )
				{
					final double d = Point.distance( pi, qi );
					if ( d < minimalDistance )
					{
						minimalDistance = d;
						closestPoint = qi;
					}
				}
				currentMatches.add( new PointMatch( pi, closestPoint ) );
			}
			try
			{
				m.fit( currentMatches );
			}
			catch ( final Exception e )
			{
				e.printStackTrace();
				return null;
			}
		}
		while ( currentMatches.equals( previousMatches ) );
		this.set( m );
		return currentMatches;
	}

	/**
	 * Default fit implementation using {@link #fit(Collection)}.  This foils
	 * the intention that {@link #fit(float[][], float[][], float[])} would be
	 * potentially more efficient.  You should better implement it directly.
	 */
	@Override
	public void fit(
			final float[][] p,
			final float[][] q,
			final float[] w )
		throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		assert
			p.length > 0 &&
			q.length == p.length : "Numbers of dimensions do not match.";

		assert
			p[ 0 ].length == p[ 1 ].length &&
			p[ 0 ].length == q[ 0 ].length &&
			p[ 0 ].length == q[ 1 ].length &&
			p[ 0 ].length == w.length : "Array lengths do not match.";

		final int n = p.length;
		final int l = p[ 0 ].length;

		final ArrayList< PointMatch > matches = new ArrayList< PointMatch >( l );
		for ( int i = 0; i < l; ++i )
		{
			final double[] pi = new double[ n ];
			final double[] qi = new double[ n ];
			for ( int d = 0; d < n; ++d )
			{
				pi[ d ] = p[ d ][ i ];
				qi[ d ] = q[ d ][ i ];
			}

			matches.add(
					new PointMatch(
							new Point( pi ),
							new Point( qi ),
							w[ i ] ) );
		}

		fit( matches );
	}

	/**
	 * Default fit implementation using {@link #fit(Collection)}.  This foils
	 * the intention that {@link #fit(double[][], double[][], double[])} would be
	 * potentially more efficient.  You should better implement it directly.
	 */
	@Override
	public void fit(
			final double[][] p,
			final double[][] q,
			final double[] w )
		throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		assert
			p.length > 0 &&
			q.length == p.length : "Numbers of dimensions do not match.";

		assert
			p[ 0 ].length == p[ 1 ].length &&
			p[ 0 ].length == q[ 0 ].length &&
			p[ 0 ].length == q[ 1 ].length &&
			p[ 0 ].length == w.length : "Array lengths do not match.";

		final int n = p.length;
		final int l = p[ 0 ].length;

		final ArrayList< PointMatch > matches = new ArrayList< PointMatch >( l );
		for ( int i = 0; i < l; ++i )
		{
			final double[] pi = new double[ n ];
			final double[] qi = new double[ n ];
			for ( int d = 0; d < n; ++d )
			{
				pi[ d ] = p[ d ][ i ];
				qi[ d ] = q[ d ][ i ];
			}

			matches.add(
					new PointMatch(
							new Point( pi ),
							new Point( qi ),
							w[ i ] ) );
		}

		fit( matches );
	}


	/**
	 * <p>Default implementation of
	 * {@link #localSmoothnessFilter(Collection, Collection, double, double, double)}.
	 * Requires that {@link #fit(Collection)} is implemented as a weighted
	 * least squares fit or something similar.</p>
	 *
	 * <p>Note that if candidates == inliers and an exception occurs, inliers
	 * will be cleared according to that there are no inliers.</p>
	 *
	 */
	@Override
	public < P extends PointMatch > boolean localSmoothnessFilter(
			final Collection< P > candidates,
			final Collection< P > inliers,
			final double sigma,
			final double maxEpsilon,
			final double maxTrust )
	{
		final double var2 = 2 * sigma * sigma;

		/* unshift an extra weight into candidates */
		for ( final P match : candidates )
			match.unshiftWeight( 1.0f );

		/* initialize inliers */
		if ( inliers != candidates )
		{
			inliers.clear();
			inliers.addAll( candidates );
		}

		boolean hasChanged = false;

//		final int p = 0;
//		System.out.print( "Smoothness filter pass  1:   0%" );
		do
		{
			//System.out.print( ( char )13 + "Smoothness filter pass " + String.format( "%2d", ++p ) + ":   0%" );
			hasChanged = false;

			final ArrayList< P > toBeRemoved = new ArrayList< P >();
			final ArrayList< P > localInliers = new ArrayList< P >();

//			final int i = 0;

			for ( final P candidate : inliers )
			{
//				System.out.print( ( char )13 + "Smoothness filter pass " + String.format( "%2d", p ) + ": " + String.format( "%3d", ( ++i * 100 / inliers.size() ) ) + "%" );

				/* calculate weights by square distance to reference in local space */
				for ( final P match : inliers )
				{
					final double w = Math.exp( -Point.squareLocalDistance( candidate.getP1(), match.getP1() ) / var2 );
					match.setWeight( 0, w );
				}

				candidate.setWeight( 0, 0 );

				boolean filteredLocalModelFound;
				try
				{
					filteredLocalModelFound = filter( candidates, localInliers, maxTrust );
				}
				catch ( final NotEnoughDataPointsException e )
				{
					filteredLocalModelFound = false;
				}

				if ( !filteredLocalModelFound )
				{
					/* clean up extra weight from candidates */
					for ( final P match : candidates )
						match.shiftWeight();

					/* no inliers */
					inliers.clear();

					return false;
				}

				candidate.apply( this );
				final double candidateDistance = Point.distance( candidate.getP1(), candidate.getP2() );
				if ( candidateDistance <= maxEpsilon )
				{
					PointMatch.apply( inliers, this );

					/* weighed mean Euclidean distances */
					double meanDistance = 0, ws = 0;
					for ( final PointMatch match : inliers )
					{
						final double w = match.getWeight();
						ws += w;
						meanDistance += Point.distance( match.getP1(), match.getP2() ) * w;
					}
					meanDistance /= ws;

					if ( candidateDistance > maxTrust * meanDistance )
					{
						hasChanged = true;
						toBeRemoved.add( candidate );
					}
				}
				else
				{
					hasChanged = true;
					toBeRemoved.add( candidate );
				}
			}
			inliers.removeAll( toBeRemoved );
//			System.out.println();
		}
		while ( hasChanged );

		/* clean up extra weight from candidates */
		for ( final P match : candidates )
			match.shiftWeight();

		return inliers.size() >= getMinNumMatches();
	}
};
