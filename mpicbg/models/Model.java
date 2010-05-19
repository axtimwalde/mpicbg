package mpicbg.models;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.Blitter;
import ij.process.ColorProcessor;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.List;
import java.util.Collection;

import mpicbg.ij.InverseTransformMapping;
import mpicbg.ij.visualization.PointVis;
import mpicbg.util.Util;

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
 * TODO: A model is planned to be a generic transformation pipeline to be
 *   applied to images, volumes or arbitrary sets of n-dimensional points.
 *   E.g. lens transformation of camera images, pose and location of mosaic
 *   tiles, non-rigid bending of confocal stacks etc.
 *  
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
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.4b
 */
public abstract class Model< M extends Model< M > > implements CoordinateTransform
{
	/* <visualization> */
	protected ImagePlus impSrc;
	protected ImagePlus impDst;
	final public ImagePlus getImpSrc(){ return impSrc; }
	final public ImagePlus getImpDst(){ return impDst; }
	final public void setImpSrc( final ImagePlus impSrc ){ this.impSrc = impSrc; }
	final public void setImpDst( final ImagePlus impDst ){ this.impDst = impDst; }
	/* </visualization> */
	
	
	/**
	 * @returns the minimal number of {@link PointMatch PointMatches} required
	 *   to solve the model.
	 */
	abstract public int getMinNumMatches();
	
	/**
	 * @deprecated "getMinSetSize" doesn't mean anything---use the more
	 *   speaking {@link #getMinNumMatches()} instead.  
	 */
	@Deprecated
	final public int getMinSetSize(){ return getMinNumMatches(); }
	
	// real random
	//final Random random = new Random( System.currentTimeMillis() );
	// repeatable results
	final static protected Random rnd = new Random( 69997 );

	
	/**
	 * The cost depends on what kind of algorithm is running.  It is always
	 * true that a smaller cost is better than large cost
	 */
	protected double cost = Double.MAX_VALUE;
	final public double getCost(){ return cost; }
	final public void setCost( final double c ){ cost = c; }

	/**
	 * @deprecated The term Error may be missleading---use {@link #getCost()} instead
	 */
	@Deprecated
	final public double getError(){ return getCost(); }
	/**
	 * @deprecated The term Error may be missleading---use {@link #getCost()} instead
	 */
	@Deprecated
	final public void setError( final double e ){ setCost( e ); }

	/**
	 * "Less than" operater to make {@link Model Models} comparable.
	 * 
	 * @param m
	 * @return false for {@link #cost} < 0.0, otherwise true if
	 *   {@link #cost this.cost} is smaller than {@link #cost m.cost}
	 */
	final public boolean betterThan( final Model< M > m )
	{
		if ( cost < 0 ) return false;
		return cost < m.cost;
	}

//	/**
//	 * Randomly change the {@link Model} for some amount.
//	 * 
//	 * @param amount
//	 */
//	abstract public void shake( final float amount );
	
	/**
	 * Fit the {@link Model} to a set of data points minimizing the global
	 * transfer error.  This is assumed to be implemented as a weighted least
	 * squares minimization.  Use
	 * {@link #ransac(Class, List, Collection, int, double, double) ransac}
	 * and/ or {@link #filter(Class, Collection, Collection)} to remove
	 * outliers from your data points
	 * 
	 * The estimated model transfers match.p1.local to match.p2.world.
	 * 
	 * @param matches set of point correpondences
	 * @throws {@link NotEnoughDataPointsException} if matches does not contain
	 *   enough data points
	 *   {@link IllDefinedDataPointsException} if the set of data points is
	 *   inappropriate to solve the Model
	 */
	abstract public void fit( final Collection< PointMatch > matches )
		throws NotEnoughDataPointsException, IllDefinedDataPointsException;

	/**
	 * Test the {@link Model} for a set of {@link PointMatch} candidates.
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
	final public boolean test(
			final Collection< PointMatch > candidates,
			final Collection< PointMatch > inliers,
			final double epsilon,
			final double minInlierRatio,
			final int minNumInliers )
	{
		inliers.clear();
		
		for ( PointMatch m : candidates )
		{
			m.apply( this );
			if ( m.getDistance() < epsilon ) inliers.add( m );
		}
		
		final float ir = ( float )inliers.size() / ( float )candidates.size();
		setCost( Math.max( 0.0, Math.min( 1.0, 1.0 - ir ) ) );
		
		return ( inliers.size() >= minNumInliers && ir > minInlierRatio );
	}
	
	/**
	 * Call {@link #test(Collection, Collection, double, double, int)} with
	 * minNumInliers = {@link #getMinNumMatches()}.
	 */
	final public boolean test(
			final Collection< PointMatch > candidates,
			final Collection< PointMatch > inliers,
			final double epsilon,
			final double minInlierRatio )
	{
		return test( candidates, inliers, epsilon, minInlierRatio, getMinNumMatches() );
	}
		
	/**
	 * Estimate the {@link Model} and filter potential outliers by robust
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
	 * @return true if {@link Model} could be estimated and inliers is not
	 *   empty, false otherwise.  If false, {@link Model} remains unchanged.
	 */
	final public boolean filter(
			final Collection< PointMatch > candidates,
			final Collection< PointMatch > inliers,
			final float maxTrust,
			final int minNumInliers )
		throws NotEnoughDataPointsException
	{
		if ( candidates.size() < getMinNumMatches() )
			throw new NotEnoughDataPointsException( candidates.size() + " data points are not enough to solve the Model, at least " + getMinNumMatches() + " data points required." );
		
		/* <visualization> */
		/* repeatable results */
		rnd.setSeed( 69997 );
		
		ColorProcessor ipBgSrc = null;
		ColorProcessor ipBgDst = null;
		ColorProcessor errorPlotBg = null;
		ImageStack stack = null;
		double eScale = 1.0;
		
		if ( impSrc != null && impDst != null )
		{
			ipBgSrc = ( ColorProcessor )impSrc.getProcessor().convertToRGB();
			ipBgSrc.setMinAndMax( -127, 383 );
			
			ipBgDst = ( ColorProcessor )impDst.getProcessor().convertToRGB();
			ipBgDst.setMinAndMax( -127, 383 );
		
			eScale = 1.1 * 25; // TODO (no epsilon known)
			errorPlotBg = new ColorProcessor( impSrc.getWidth(), impSrc.getHeight() );
			PointVis.drawEpsilonCoordinates( errorPlotBg, Color.GRAY, 1 );
			
			stack = new ImageStack(
					2 * impSrc.getWidth() + impDst.getWidth() + 4,
					Math.max( impSrc.getHeight(), impDst.getHeight() ) );
		}
		
		int i = 0;
		/* </visualization> */
		
		final M copy = clone();
		
		inliers.clear();
		inliers.addAll( candidates );
		final ArrayList< PointMatch > temp = new ArrayList< PointMatch >();
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
			catch ( NotEnoughDataPointsException e )
			{
				return false;
			}
			catch ( IllDefinedDataPointsException e )
			{
				return false;
			}
			
			final ErrorStatistic observer = new ErrorStatistic( temp.size() );
			for ( final PointMatch m : temp )
			{
				m.apply( copy );
				observer.add( m.getDistance() );
			}
			
			/* <visualization> */
			if ( impSrc != null && impDst != null )
			{
				/* impSrc */
				final ColorProcessor ipSrc;
				
				if ( InverseCoordinateTransform.class.isInstance( copy ) )
				{
					ipSrc = new ColorProcessor( impSrc.getWidth(), impSrc.getHeight() );
//					ipSrc.setColor( Color.GRAY );
//					ipSrc.fill();
					final InverseTransformMapping< ? > mapping = new InverseTransformMapping< InverseCoordinateTransform >( ( InverseCoordinateTransform )copy );
					mapping.mapInterpolated( ipBgSrc, ipSrc );
				}
				else
					ipSrc = ( ColorProcessor )ipBgSrc.duplicate();
				
				
				final ArrayList< Point > candidatesPointsSrc = new ArrayList< Point >();
				PointMatch.cloneSourcePoints( candidates, candidatesPointsSrc );
				Point.apply( copy, candidatesPointsSrc );
				PointVis.drawWorldPoints( ipSrc, candidatesPointsSrc, Color.BLACK, 3 );
				
				final ArrayList< Point > tempInliersPointsSrc = new ArrayList< Point >();
				PointMatch.cloneSourcePoints( inliers, tempInliersPointsSrc );
				Point.apply( copy, tempInliersPointsSrc );
				PointVis.drawWorldPoints( ipSrc, tempInliersPointsSrc, Color.GREEN, 3 );
				
				
				/* impDst */
				final ColorProcessor ipDst = ( ColorProcessor )ipBgDst.duplicate();
				final ArrayList< Point > candidatesPointsDst = new ArrayList< Point >();
				PointMatch.targetPoints( candidates, candidatesPointsDst );
				PointVis.drawLocalPoints( ipDst, candidatesPointsDst, Color.BLACK, 3 );
				
				final ArrayList< Point > tempInliersPointsDst = new ArrayList< Point >();
				PointMatch.targetPoints( inliers, tempInliersPointsDst );
				PointVis.drawLocalPoints( ipDst, tempInliersPointsDst, Color.GREEN, 3 );
				
				/* error plot */
				final ColorProcessor ipPlot = ( ColorProcessor )errorPlotBg.duplicate();
				for ( final PointMatch pm : inliers )
				{
					final float[] l = pm.getP1().getL().clone();
					final float[] w = pm.getP2().getL();
					copy.applyInPlace( l );
					
					ipPlot.setColor( Color.GREEN );
					ipPlot.setLineWidth( 3 );
					final double x = ( ( w[ 0 ] - l[ 0 ] ) / eScale + 1 ) * ipPlot.getWidth() / 2;
					final double y = ( ( w[ 1 ] - l[ 1 ] ) / eScale + 1 ) * ipPlot.getHeight() / 2;
					ipPlot.drawDot( Util.round( x ), Util.round( y ) );	
				}
				
				final int d = Util.round( observer.getMedian() * maxTrust / eScale * ipPlot.getWidth() );
				
				ipPlot.setColor( Color.MAGENTA );
				ipPlot.setLineWidth( 1 );
				ipPlot.drawOval( ( ipPlot.getWidth() - d ) / 2, ( ipPlot.getHeight() - d ) / 2, d, d );
				
				final ColorProcessor ipPanels = new ColorProcessor( stack.getWidth(), stack.getHeight() );
				ipPanels.copyBits( ipSrc, 0, 0, Blitter.COPY );
				ipPanels.copyBits( ipDst, ipSrc.getWidth() + 2, 0, Blitter.COPY );
				ipPanels.copyBits( ipPlot, ipSrc.getWidth() + ipDst.getWidth() + 4, 0, Blitter.COPY );
				
				PointVis.drawWorldPointMatchLines(
						ipPanels,
						inliers,
						Color.GREEN,
						1,
						new Rectangle( 0, 0, impSrc.getWidth(), impSrc.getHeight() ),
						new Rectangle( impSrc.getWidth() + 2, 0, impDst.getWidth(), impDst.getHeight() ),
						1.0,
						1.0 );
				
				stack.addSlice( "" + ++i, ipPanels );
			}
			/* </visualization> */
			
			inliers.clear();
			final double t = observer.getMedian() * maxTrust;
			for ( final PointMatch m : temp )
			{
				if ( m.getDistance() <= t )
					inliers.add( m );
			}
			
			copy.cost = observer.mean;
		}
		while ( numInliers > inliers.size() );
		
		if ( numInliers < minNumInliers )
			return false;
		
		/* <visualization> */
		if ( impSrc != null && impDst != null )
		{
			final ImagePlus impVis = new ImagePlus( impSrc.getTitle() + " Robust Regression", stack );
			impVis.show();
		}
		IJ.log( "filtered after " + i + " iterations");
		
		/* </visualization> */
		
		set( copy );
		return true;
	}
	
	/**
	 * Call {@link #filter(Collection, Collection, float, int)} with minNumInliers = {@link Model#getMinNumMatches()}.
	 */
	final public boolean filter(
			final Collection< PointMatch > candidates,
			final Collection< PointMatch > inliers,
			final float maxTrust )
		throws NotEnoughDataPointsException
	{
		return filter( candidates, inliers, maxTrust, getMinNumMatches() );
	}
	
	
	/**
	 * Call {@link #filter(Collection, Collection, float)} with maxTrust = 4 and minNumInliers = {@link Model#getMinNumMatches()}.
	 */
	final public boolean filter(
			final Collection< PointMatch > candidates,
			final Collection< PointMatch > inliers )
		throws NotEnoughDataPointsException
	{
		return filter( candidates, inliers, 4f, getMinNumMatches() );
	}
	
	
	/**
	 * Find the {@link Model} of a set of {@link PointMatch} candidates
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
	 * @return true if {@link Model} could be estimated and inliers is not
	 *   empty, false otherwise.  If false, {@link Model} remains unchanged.
	 */
	final public boolean ransac(
			final List< PointMatch > candidates,
			final Collection< PointMatch > inliers,
			final int iterations,
			final double epsilon,
			final double minInlierRatio,
			final int minNumInliers )
		throws NotEnoughDataPointsException
	{
		/* <visualization> */
		/* repeatable results */
		rnd.setSeed( 69997 );
		
		ColorProcessor ipBgSrc = null;
		ColorProcessor ipBgDst = null;
		ColorProcessor errorPlotBg = null;
		ImageStack stack = null;
		double eScale = 1.0;
		
		if ( impSrc != null && impDst != null )
		{
			ipBgSrc = ( ColorProcessor )impSrc.getProcessor().convertToRGB();
			ipBgSrc.setMinAndMax( -127, 383 );
			
			ipBgDst = ( ColorProcessor )impDst.getProcessor().convertToRGB();
			ipBgDst.setMinAndMax( -127, 383 );
		
			eScale = 1.1 * epsilon;
			errorPlotBg = new ColorProcessor( impSrc.getWidth(), impSrc.getHeight() );
			PointVis.drawEpsilonCoordinates( errorPlotBg, Color.GRAY, 1 );
			
			stack = new ImageStack(
					2 * impSrc.getWidth() + impDst.getWidth() + 4,
					Math.max( impSrc.getHeight(), impDst.getHeight() ) );
		}
		
		int bestI = -1;
		/* </visualization> */
			
		if ( candidates.size() < getMinNumMatches() )
			throw new NotEnoughDataPointsException( candidates.size() + " data points are not enough to solve the Model, at least " + getMinNumMatches() + " data points required." );
		
		final M copy = clone();
		final M m = clone();
		
		inliers.clear();
		
		int i = 0;
		final HashSet< PointMatch > minMatches = new HashSet< PointMatch >();
		
A:		while ( i < iterations )
		{
			// choose model.MIN_SET_SIZE disjunctive matches randomly
			minMatches.clear();
			for ( int j = 0; j < getMinNumMatches(); ++j )
			{
				PointMatch p;
				do
				{
					p = candidates.get( ( int )( rnd.nextDouble() * candidates.size() ) );
				}
				while ( minMatches.contains( p ) );
				minMatches.add( p );
			}
			try { m.fit( minMatches ); }
			catch ( IllDefinedDataPointsException e )
			{
				++i;
				continue;
			}
			
			final ArrayList< PointMatch > tempInliers = new ArrayList< PointMatch >();
			
			int numInliers = 0;
			boolean isGood = m.test( candidates, tempInliers, epsilon, minInlierRatio );
			while ( isGood && numInliers < tempInliers.size() )
			{
				numInliers = tempInliers.size();
				try { m.fit( tempInliers ); }
				catch ( IllDefinedDataPointsException e )
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
				bestI = i;
			}
			++i;
			
			/* <visualization> */
			if ( impSrc != null && impDst != null )
			{
				if ( isGood )
				{
					try { m.fit( tempInliers ); }
					catch ( IllDefinedDataPointsException e )
					{
						++i;
						continue A;
					}
				}
				/* impSrc */
				final ColorProcessor ipSrc = ( ColorProcessor )ipBgSrc.duplicate();
				
				final ArrayList< Point > candidatesPointsSrc = new ArrayList< Point >();
				PointMatch.sourcePoints( candidates, candidatesPointsSrc );
				PointVis.drawLocalPoints( ipSrc, candidatesPointsSrc, Color.BLACK, 3 );
				
				final ArrayList< Point > tempInliersPointsSrc = new ArrayList< Point >();
				PointMatch.sourcePoints( tempInliers, tempInliersPointsSrc );
				PointVis.drawLocalPoints( ipSrc, tempInliersPointsSrc, Color.GREEN, 3 );
				
				final ArrayList< Point > minMatchesPointsSrc = new ArrayList< Point >();
				PointMatch.sourcePoints( minMatches, minMatchesPointsSrc );
				PointVis.drawLocalPoints( ipSrc, minMatchesPointsSrc, Color.WHITE, 3 );
				
				
				/* impDst */
				final ColorProcessor ipDst = ( ColorProcessor )ipBgDst.duplicate();
				final ArrayList< Point > candidatesPointsDst = new ArrayList< Point >();
				PointMatch.targetPoints( candidates, candidatesPointsDst );
				PointVis.drawLocalPoints( ipDst, candidatesPointsDst, Color.BLACK, 3 );
				
				final ArrayList< Point > tempInliersPointsDst = new ArrayList< Point >();
				PointMatch.targetPoints( tempInliers, tempInliersPointsDst );
				PointVis.drawLocalPoints( ipDst, tempInliersPointsDst, Color.GREEN, 3 );
				
				final ArrayList< Point > minMatchesPointsDst = new ArrayList< Point >();
				PointMatch.targetPoints( minMatches, minMatchesPointsDst );
				PointVis.drawLocalPoints( ipDst, minMatchesPointsDst, Color.WHITE, 3 );
				
				
				/* error plot */
				final ColorProcessor ipPlot = ( ColorProcessor )errorPlotBg.duplicate();
				for ( final PointMatch pm : tempInliers )
				{
					final float[] l = pm.getP1().getL().clone();
					final float[] w = pm.getP2().getL();
					m.applyInPlace( l );
					
					ipPlot.setColor( Color.GREEN );
					ipPlot.setLineWidth( 3 );
					final double x = ( ( w[ 0 ] - l[ 0 ] ) / eScale + 1 ) * ipPlot.getWidth() / 2;
					final double y = ( ( w[ 1 ] - l[ 1 ] ) / eScale + 1 ) * ipPlot.getHeight() / 2;
					ipPlot.drawDot( Util.round( x ), Util.round( y ) );	
				}
				
				final ColorProcessor ipPanels = new ColorProcessor( stack.getWidth(), stack.getHeight() );
				ipPanels.copyBits( ipSrc, 0, 0, Blitter.COPY );
				ipPanels.copyBits( ipDst, ipSrc.getWidth() + 2, 0, Blitter.COPY );
				ipPanels.copyBits( ipPlot, ipSrc.getWidth() + ipDst.getWidth() + 4, 0, Blitter.COPY );
				
				PointVis.drawLocalPointMatchLines(
						ipPanels,
						tempInliers,
						Color.GREEN,
						1,
						new Rectangle( 0, 0, impSrc.getWidth(), impSrc.getHeight() ),
						new Rectangle( impSrc.getWidth() + 2, 0, impDst.getWidth(), impDst.getHeight() ),
						1.0,
						1.0 );
				
				stack.addSlice( "" + i, ipPanels );
			}
			/* </visualization> */
			
		}
		
		/* <visualization> */
		if ( impSrc != null && impDst != null )
		{
			final ImagePlus impVis = new ImagePlus( impSrc.getTitle() + " RANSAC", stack );
			impVis.show();
		}
		IJ.log( "best i: " + bestI );
		
		/* </visualization> */
		
		if ( inliers.size() == 0 )
			return false;
		
		set( copy );
		return true;
	}
	
	/**
	 * Call {@link #ransac(List, Collection, int, double, double, int)} with
	 * minNumInliers = {@link #getMinNumMatches()}.
	 */
	final public boolean ransac(
			final List< PointMatch > candidates,
			final Collection< PointMatch > inliers,
			final int iterations,
			final double epsilon,
			final double minInlierRatio )
		throws NotEnoughDataPointsException
	{
		return ransac( candidates, inliers, iterations, epsilon, minInlierRatio, getMinNumMatches() );
	}
	
	/**
	 * Estimate a {@link Model} from a set with many outliers by first
	 * filtering the worst outliers with
	 * {@link #ransac(Class, List, Collection, int, double, double) RANSAC}
	 * \citet[{FischlerB81} and filter potential outliers by robust iterative
	 * regression.
	 * 
	 * @param modelClass class of the model to be estimated
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
	 * @return true if {@link Model} could be estimated and inliers is not
	 *   empty, false otherwise.  If false, {@link Model} remains unchanged.
	 */
	final public boolean filterRansac(
			final List< PointMatch > candidates,
			final Collection< PointMatch > inliers,
			final int iterations,
			final float maxEpsilon,
			final float minInlierRatio,
			final int minNumInliers,
			final float maxTrust )
		throws NotEnoughDataPointsException
	{
		final ArrayList< PointMatch > temp = new ArrayList< PointMatch >();
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
	 * Call {@link #filterRansac(List, Collection, int, float, float, int, float)}
	 * with maxTrust = 4.
	 */
	final public boolean filterRansac(
			final List< PointMatch > candidates,
			final Collection< PointMatch > inliers,
			final int iterations,
			final float maxEpsilon,
			final float minInlierRatio,
			final int minNumInliers )
		throws NotEnoughDataPointsException
	{
		return filterRansac( candidates, inliers, iterations, maxEpsilon, minInlierRatio, minNumInliers, 4f );
	}
	
	
	/**
	 * Call {@link #filterRansac(List, Collection, int, float, float, int, float)}
	 * with minNumInliers = {@link #getMinNumMatches()}.
	 */
	final public boolean filterRansac(
			final List< PointMatch > candidates,
			final Collection< PointMatch > inliers,
			final int iterations,
			final float maxEpsilon,
			final float minInlierRatio,
			final float maxTrust )
		throws NotEnoughDataPointsException
	{
		return filterRansac( candidates, inliers, iterations, maxEpsilon, minInlierRatio, getMinNumMatches(), maxTrust );
	}
	
	
	/**
	 * Call {@link #filterRansac(List, Collection, int, float, float, float)}
	 * with maxTrust = 4.
	 */
	final public boolean filterRansac(
			final List< PointMatch > candidates,
			final Collection< PointMatch > inliers,
			final int iterations,
			final float maxEpsilon,
			final float minInlierRatio )
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
	final public Collection< PointMatch > icp(
			final List< Point > p,
			final List< Point > q )
	{
		final M m = clone();
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
				float minimalDistance = Float.MAX_VALUE;
				Point closestPoint = null;
				for ( final Point qi : q )
				{
					final float d = Point.distance( pi, qi );
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
			catch ( Exception e )
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
	 * Create a meaningful string representation of the model for save into
	 * text-files or display on terminals.
	 */
	abstract public String toString();
	
	
	/**
	 * Set the model to m
	 * @param m
	 */
	abstract public void set( final M m );

	
	/**
	 * Clone the model.
	 */
	abstract public M clone();
};
