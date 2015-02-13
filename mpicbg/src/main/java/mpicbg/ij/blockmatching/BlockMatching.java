package mpicbg.ij.blockmatching;

import ij.IJ;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.ij.InverseMapping;
import mpicbg.ij.TransformMapping;
import mpicbg.ij.util.Filter;
import mpicbg.ij.util.Util;
import mpicbg.models.AbstractAffineModel2D;
import mpicbg.models.CoordinateTransform;
import mpicbg.models.CoordinateTransformList;
import mpicbg.models.ErrorStatistic;
import mpicbg.models.InvertibleCoordinateTransform;
import mpicbg.models.Model;
import mpicbg.models.MovingLeastSquaresTransform;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import mpicbg.models.SimilarityModel2D;
import mpicbg.models.TransformMesh;
import mpicbg.models.TranslationModel2D;

/**
 * Methods for establishing block-based correspondences for given sets of
 * source {@link Point Points}.
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 */
public class BlockMatching
{
	/*
	 * &sigma; of the Gaussian kernel required to make an image sampled at
	 * &sigma; = 1.6 (as suggested by Lowe, 2004)
	 */
	final static private float minSigma = 1.6f;
//	final static private float minDiffSigma = ( float )Math.sqrt( minSigma * minSigma - 0.5f );

	private BlockMatching(){}

	/**
	 * Estimate the mean intensity of a block.
	 *
	 * <dl>
	 * <dt>Note:</dt>
	 * <dd>Make sure that the block is fully contained in the image, this will
	 * not be checked by the method for efficiency reasons.</dd>
	 * </dl>
	 *
	 * @param fp
	 * @param tx
	 * @param ty
	 * @param blockWidth
	 * @param blockHeight
	 * @return
	 */
	static protected float blockMean(
			final FloatProcessor fp,
			final int tx,
			final int ty,
			final int blockWidth,
			final int blockHeight )
	{
		final int width = fp.getWidth();
		final float[] pixels = ( float[] )fp.getPixels();

		double sum = 0;
		for ( int y = ty + blockHeight - 1; y >= ty; --y )
		{
			final int ry = y * width;
			for ( int x = tx + blockWidth - 1; x >= tx; --x )
				sum += pixels[ ry + x ];
		}
		return ( float )( sum / blockWidth / blockHeight );
	}


	/**
	 * Set all pixels in source with a mask value < 0.95f to NaN
	 *
	 * @param source
	 * @param mask
	 */
	final static private void mask( final FloatProcessor source, final FloatProcessor mask )
	{
		final float[] sourcePixels = ( float[] )source.getPixels();
		final float[] maskPixels = ( float[] )mask.getPixels();
		final int n = sourcePixels.length;
		for ( int i = 0; i < n; ++i )
		{
			final float m = maskPixels[ i ];
			if ( m < 0.95f )
				sourcePixels[ i ] = Float.NaN;
		}
	}

	final static private void mapAndMask(
			final ImageProcessor source,
			final ImageProcessor mask,
			final ImageProcessor target,
			final CoordinateTransform transform )
	{
		final double[] t = new double[ 2 ];
		final int sw = source.getWidth() - 1;
		final int sh = source.getHeight() - 1;
		final int tw = target.getWidth();
		final int th = target.getHeight();
		for ( int y = 0; y < th; ++y )
		{
			for ( int x = 0; x < tw; ++x )
			{
				t[ 0 ] = x;
				t[ 1 ] = y;
				transform.applyInPlace( t );
				if (
						t[ 0 ] >= 0 &&
						t[ 0 ] <= sw &&
						t[ 1 ] >= 0 &&
						t[ 1 ] <= sh &&
						mask.getPixelInterpolated( t[ 0 ], t[ 1 ] ) > 0 )
					target.putPixel( x, y, source.getPixelInterpolated( t[ 0 ], t[ 1 ] ) );
			}
		}
	}

	/**
	 * Estimate the intensity variance of a block.
	 *
	 * <dl>
	 * <dt>Note:</dt>
	 * <dd>Make sure that the block is fully contained in the image, this will
	 * not be checked by the method for efficiency reasons.</dd>
	 * </dl>
	 *
	 * @param fp
	 * @param tx
	 * @param ty
	 * @param blockWidth
	 * @param blockHeight
	 * @return
	 */
	static protected float blockVariance(
			final FloatProcessor fp,
			final int tx,
			final int ty,
			final int blockWidth,
			final int blockHeight,
			final float mean )
	{
		final int width = fp.getWidth();
		final float[] pixels = ( float[] )fp.getPixels();

		double sum = 0;
		for ( int y = ty + blockHeight - 1; y >= ty; --y )
		{
			final int ry = y * width;
			for ( int x = tx + blockWidth - 1; x >= tx; --x )
			{
				final float a = pixels[ ry + x ] - mean;
				sum += a * a;
			}
		}
		return ( float )( sum / ( blockWidth * blockHeight - 1 ) );
	}

	/**
     * Estimate {@linkplain PointMatch point correspondences} for a
     * {@link Collection} of {@link Point Points} among two images that are
     * approximately related by an {@link InvertibleCoordinateTransform} using
     * the square difference of pixel intensities as a similarity measure.
     *
     * @param source
     * @param target
     * @param transform transfers source into target approximately
     * @param blockRadiusX horizontal radius of a block
     * @param blockRadiusY vertical radius of a block
     * @param searchRadiusX horizontal search radius
     * @param searchRadiusY vertical search radius
     * @param sourcePoints
     * @param sourceMatches
     */
    static public void matchByMinimalSquareDifference(
			final FloatProcessor source,
			final FloatProcessor target,
			final InvertibleCoordinateTransform transform,
			final int blockRadiusX,
			final int blockRadiusY,
			final int searchRadiusX,
			final int searchRadiusY,
			final Collection< ? extends Point > sourcePoints,
			final Collection< PointMatch > sourceMatches )
	{
		Util.normalizeContrast( source );
		Util.normalizeContrast( target );

		final FloatProcessor mappedTarget = new FloatProcessor( source.getWidth() + 2 * searchRadiusX, source.getHeight() + 2 * searchRadiusY );
		Util.fillWithNaN( mappedTarget );

		final TranslationModel2D tTarget = new TranslationModel2D();
		tTarget.set( -searchRadiusX, -searchRadiusY );
		final CoordinateTransformList< CoordinateTransform > lTarget = new CoordinateTransformList< CoordinateTransform >();
		lTarget.add( tTarget );
		lTarget.add( transform );
		final InverseMapping< ? > targetMapping = new TransformMapping< CoordinateTransform >( lTarget );
		targetMapping.mapInverseInterpolated( target, mappedTarget );

//		mappedTarget.setMinAndMax( 0, 1 );
//		new ImagePlus( "Mapped Target", mappedTarget ).show();

		int k = 0;
		for ( final Point p : sourcePoints )
		{
			final double[] s = p.getL();
			final int px = ( int )Math.round( s[ 0 ] );
			final int py = ( int )Math.round( s[ 1 ] );
			if (
					px - blockRadiusX >= 0 &&
					px + blockRadiusX < source.getWidth() &&
					py - blockRadiusY >= 0 &&
					py + blockRadiusY < source.getHeight() )
			{
				IJ.showProgress( k++, sourcePoints.size() );
				double tx = 0;
				double ty = 0;
				float dMin = Float.MAX_VALUE;
				for ( int ity = -searchRadiusY; ity <= searchRadiusY; ++ity )
					for ( int itx = -searchRadiusX; itx <= searchRadiusX; ++itx )
					{
						float d = 0;
						float n = 0;
						for ( int iy = -blockRadiusY; iy <= blockRadiusY; ++iy )
						{
							final int y = py + iy;
							for ( int ix = -blockRadiusX; ix <= blockRadiusX; ++ix )
							{
								final int x = px + ix;
								final float sf = source.getf( x, y );
								final float tf = mappedTarget.getf( x + itx + searchRadiusX, y + ity + searchRadiusY );
								if ( sf == Float.NaN || tf == Float.NaN )
									continue;
								else
								{
									final float a = sf - tf;
									d += a * a;
									++n;
								}
							}
						}
						if ( n > 0 )
						{
							d /= n;
							if ( d < dMin )
							{
								dMin = d;
								tx = itx;
								ty = ity;
							}
						}
					}
				final double[] t = new double[]{ tx + s[ 0 ], ty + s[ 1 ] };
				System.out.println( k + " : " + tx + ", " + ty );
				transform.applyInPlace( t );
				sourceMatches.add( new PointMatch( p, new Point( t ) ) );
			}
		}
	}


    static protected void matchByMaximalPMCC(
    		final FloatProcessor source,
    		final FloatProcessor target,
    		final int blockRadiusX,
    		final int blockRadiusY,
    		final int searchRadiusX,
    		final int searchRadiusY,
    		final float minR,
    		final float rod,
    		final float maxCurvature,
    		final Collection< PointMatch > query,
    		final Collection< PointMatch > results ) throws InterruptedException, ExecutionException
	{
		final float maxCurvatureRatio = ( maxCurvature + 1 ) * ( maxCurvature + 1 ) / maxCurvature;

		final int blockWidth = 2 * blockRadiusX + 1;
		final int blockHeight = 2 * blockRadiusY + 1;

		/* Visualization of PMCC(x,y) */
		/* <visualisation> */
//		final ImageStack rMapStack = new ImageStack( 2 * searchRadiusX + 1, 2 * searchRadiusY + 1 );
//		final AtomicInteger l = new AtomicInteger( 0 );
		/* </visualisation> */

		final AtomicInteger k = new AtomicInteger( 0 );

		final ExecutorService exec = Executors.newFixedThreadPool( Runtime.getRuntime().availableProcessors() );
		final ArrayList< Future< PointMatch > > tasks = new ArrayList< Future< PointMatch > >();

		for ( final PointMatch pm : query )
		{
			tasks.add( exec.submit( new Callable< PointMatch >()
			{
				@Override
				public PointMatch call()
				{
					IJ.showProgress( k.getAndIncrement(), query.size() );

					final Point p = pm.getP1();

					final double[] s = p.getL();
					final int px = ( int )Math.round( s[ 0 ] );
					final int py = ( int )Math.round( s[ 1 ] );
					final int ptx = px - blockRadiusX;
					final int pty = py - blockRadiusY;
					if ( ptx >= 0 && ptx + blockWidth < source.getWidth() && pty >= 0 && pty + blockHeight < source.getHeight() )
					{
						final float sourceBlockMean = blockMean( source, ptx, pty, blockWidth, blockHeight );
						if ( Float.isNaN( sourceBlockMean ) )
							return null;
						final float sourceBlockStd = ( float ) Math.sqrt( blockVariance( source, ptx, pty, blockWidth, blockHeight, sourceBlockMean ) );
						if ( sourceBlockStd == 0 )
							return null;

						double tx = 0;
						double ty = 0;
						float rMax = -Float.MAX_VALUE;

						final FloatProcessor rMap = new FloatProcessor( 2 * searchRadiusX + 1, 2 * searchRadiusY + 1 );

						for ( int ity = -searchRadiusY; ity <= searchRadiusY; ++ity )
						{
							final int ipty = ity + pty + searchRadiusY;
							for ( int itx = -searchRadiusX; itx <= searchRadiusX; ++itx )
							{
								final int iptx = itx + ptx + searchRadiusX;

								final float targetBlockMean = blockMean( target, iptx, ipty, blockWidth, blockHeight );
								if ( Float.isNaN( targetBlockMean ) )
									return null;
								final float targetBlockStd = ( float ) Math.sqrt( blockVariance( target, iptx, ipty, blockWidth, blockHeight, targetBlockMean ) );
								if ( targetBlockStd == 0 )
									return null;

								float r = 0;
								for ( int iy = 0; iy < blockHeight; ++iy )
								{
									final int ys = pty + iy;
									final int yt = ipty + iy;
									for ( int ix = 0; ix < blockWidth; ++ix )
									{
										final int xs = ptx + ix;
										final int xt = iptx + ix;
										r += ( source.getf( xs, ys ) - sourceBlockMean ) * ( target.getf( xt, yt ) - targetBlockMean );
									}
								}
								r /= sourceBlockStd * targetBlockStd * ( blockWidth * blockHeight - 1 );
								if ( r > rMax )
								{
									rMax = r;
									tx = itx;
									ty = ity;
								}
								rMap.setf( itx + searchRadiusX, ity + searchRadiusY, r );

							}
						}

						/* <visualisation> */
//						synchronized ( rMapStack )
//						{
//							rMap.setMinAndMax( rMap.getMin(), rMap.getMax() );
//							rMapStack.addSlice( "" + l.incrementAndGet(), rMap );
//						}
						/* </visualisation> */

						/* search and process maxima */
						float bestR = -2.0f;
						float secondBestR = -2.0f;
						double dx = 0, dy = 0, dxx = 0, dyy = 0, dxy = 0;
						for ( int y = 2 * searchRadiusY - 1; y > 0; --y )
							for ( int x = 2 * searchRadiusX - 1; x > 0; --x )
							{
								final float c00, c01, c02, c10, c11, c12, c20, c21, c22;

								c11 = rMap.getf( x, y );

								c00 = rMap.getf( x - 1, y - 1 );
								if ( c00 >= c11 )
									continue;
								c01 = rMap.getf( x, y - 1 );
								if ( c01 >= c11 )
									continue;
								c02 = rMap.getf( x + 1, y - 1 );
								if ( c02 >= c11 )
									continue;

								c10 = rMap.getf( x - 1, y );
								if ( c10 >= c11 )
									continue;
								c12 = rMap.getf( x + 1, y );
								if ( c12 >= c11 )
									continue;

								c20 = rMap.getf( x - 1, y + 1 );
								if ( c20 >= c11 )
									continue;
								c21 = rMap.getf( x, y + 1 );
								if ( c21 >= c11 )
									continue;
								c22 = rMap.getf( x + 1, y + 1 );
								if ( c22 >= c11 )
									continue;

								/* is it better than what we had before? */
								if ( c11 <= bestR )
								{
									if ( c11 > secondBestR )
										secondBestR = c11;
									continue;
								}

								secondBestR = bestR;
								bestR = c11;

								/* is it good enough? */
								if ( c11 < minR )
									continue;

								/* estimate finite derivatives */
								dx = ( c12 - c10 ) / 2.0f;
								dy = ( c21 - c01 ) / 2.0f;
								dxx = c10 - c11 - c11 + c12;
								dyy = c01 - c11 - c11 + c21;
								dxy = ( c22 - c20 - c02 + c00 ) / 4.0f;
							}

//						IJ.log( "maximum found" );

						/* is it good enough? */
						if ( bestR < minR )
							return null;

//						IJ.log( "minR test passed" );

						/* is there more than one maximum of equal goodness? */
						final float r = ( 1.0f + secondBestR ) / ( 1.0f + bestR );
						if ( r > rod )
							return null;

//						IJ.log( "rod test passed" );

						/* is it well localized in both x and y? */
						final double det = dxx * dyy - dxy * dxy;
						final double trace = dxx + dyy;
						if ( det <= 0 || trace * trace / det > maxCurvatureRatio )
							return null;

//						IJ.log( "edge test passed" );

						/* localize by Taylor expansion */
						/* invert Hessian */
						final double ixx = dyy / det;
						final double ixy = -dxy / det;
						final double iyy = dxx / det;

						/* calculate offset */
						final double ox = -ixx * dx - ixy * dy;
						final double oy = -ixy * dx - iyy * dy;

						if ( ox >= 1 || oy >= 1 || ox <= -1 || oy <= -1 )
							return null;

//						IJ.log( "localized" );

						final double[] t = new double[] { tx + s[ 0 ] + ox, ty + s[ 1 ] + oy };
						return new PointMatch( p, new Point( t ) );
					}
					else
						return null;
				}
			} ) );
		}

		for ( final Future< PointMatch > fu : tasks )
		{
			try
			{
				final PointMatch pm = fu.get();
				if ( pm != null )
					results.add( pm );
			}
			catch ( final InterruptedException e )
			{
				exec.shutdownNow();
				throw e;
			}
		}

		tasks.clear();
		exec.shutdown();

		/* <visualisation> */
//		if ( results.size() > 0 ) new ImagePlus( "r", rMapStack ).show();
//		if ( rMapStack.getSize() > 0 ) new ImagePlus( "r", rMapStack ).show();
		/* </visualisation> */
	}




    /**
	 * Estimate {@linkplain PointMatch point correspondences} for a
	 * {@link Collection} of {@link Point Points} among two images that are
	 * approximately related by an {@link InvertibleCoordinateTransform} using
	 * the Pearson product-moment correlation coefficient (PMCC) <i>r</i> of
	 * pixel intensities as similarity measure. Only correspondence candidates
	 * with <i>r</i> >= a given threshold are accepted.
	 *
	 * @param source
	 * @param target
	 * @param sourceMask
	 * @param targetMask
	 * @param scale
	 *            [0,1]
	 * @param transform
	 *            transfers source into target approximately
	 * @param blockRadiusX
	 *            horizontal radius of a block
	 * @param blockRadiusY
	 *            vertical radius of a block
	 * @param searchRadiusX
	 *            horizontal search radius
	 * @param searchRadiusY
	 *            vertical search radius
	 * @param minR
	 *            minimal accepted Cross-Correlation coefficient
	 * @param rod
	 * @param sourcePoints
	 * @param sourceMatches
	 * @param observer
	 */
    static public void matchByMaximalPMCC(
			FloatProcessor source,
			FloatProcessor target,
			FloatProcessor sourceMask,
			final FloatProcessor targetMask,
			final double scale,
			final CoordinateTransform transform,
			final int blockRadiusX,
			final int blockRadiusY,
			final int searchRadiusX,
			final int searchRadiusY,
			final float minR,
			final float rod,
			final float maxCurvature,
			final Collection< ? extends Point > sourcePoints,
			final Collection< PointMatch > sourceMatches,
			final ErrorStatistic observer ) throws InterruptedException, ExecutionException
	{
    	final int scaledBlockRadiusX = ( int )Math.ceil( scale * blockRadiusX );
    	final int scaledBlockRadiusY = ( int )Math.ceil( scale * blockRadiusY );
    	final int scaledSearchRadiusX = ( int )Math.ceil( scale * searchRadiusX ) + 1; // +1 for 3x3 maximum test
    	final int scaledSearchRadiusY = ( int )Math.ceil( scale * searchRadiusY ) + 1; // +1 for 3x3 maximum test

    	/* Scale source */
    	source = Filter.createDownsampled( source, scale, 0.5f, minSigma );
    	Util.normalizeContrast( source );

    	/* Scaled source mask */
    	if ( sourceMask != null )
    		mask( source, Filter.createDownsampled( sourceMask, scale, 0.5f, 0.5f ) );

    	/* Free memory */
    	sourceMask = null;

    	/* Smooth target with respect to the desired scale */
    	target = ( FloatProcessor )target.duplicate();

    	Filter.smoothForScale( target, scale, 0.5f, minSigma );
    	Util.normalizeContrast( target );

    	final FloatProcessor mappedScaledTarget = new FloatProcessor( source.getWidth() + 2 * scaledSearchRadiusX, source.getHeight() + 2 * scaledSearchRadiusY );
		Util.fillWithNaN( mappedScaledTarget );

		/* Shift relative to the scaled search radius */
		final TranslationModel2D tTarget = new TranslationModel2D();
		tTarget.set( -scaledSearchRadiusX / scale, -scaledSearchRadiusY / scale );

		/* Scale */
		final SimilarityModel2D sTarget = new SimilarityModel2D();
		sTarget.set( 1.0f / scale, 0, 0, 0 );

		/* Combined transformation */
		final CoordinateTransformList< CoordinateTransform > lTarget = new CoordinateTransformList< CoordinateTransform >();
		lTarget.add( sTarget );
		lTarget.add( tTarget );
		lTarget.add( transform );

		if ( targetMask == null )
		{
			final InverseMapping< ? > targetMapping = new TransformMapping< CoordinateTransform >( lTarget );
			targetMapping.mapInverseInterpolated( target, mappedScaledTarget );
		}
		else
		{
			final FloatProcessor smoothedTargetMask = ( FloatProcessor )targetMask.duplicate();
	    	Filter.smoothForScale( smoothedTargetMask, scale, 0.5f, 0.5f );

	    	mapAndMask( target, smoothedTargetMask, mappedScaledTarget, lTarget );
		}

		target = null;

		/* <visualization> */
//		source.setMinAndMax( 0, 1 );
//		mappedScaledTarget.setMinAndMax( 0, 1 );
//		new ImagePlus( "Scaled Source", source ).show();
//		new ImagePlus( "Mapped Target", mappedScaledTarget ).show();
		/* </visualization> */

		final Map< Point, Point > scaledSourcePoints = new HashMap< Point, Point>();
		final ArrayList< PointMatch > scaledSourceMatches = new ArrayList< PointMatch >();

		for ( final Point p : sourcePoints )
		{
			final double[] l = p.getL().clone();
			l[ 0 ] *= scale;
			l[ 1 ] *= scale;
			scaledSourcePoints.put( new Point( l ), p );
		}

		/* initialize source points and the expected place to search for them temporarily */
		final Collection< PointMatch > query = new ArrayList< PointMatch >();
		for ( final Point p : scaledSourcePoints.keySet() )
			query.add( new PointMatch( p, p.clone()) );

		matchByMaximalPMCC(
				source,
				mappedScaledTarget,
				scaledBlockRadiusX,
				scaledBlockRadiusY,
				scaledSearchRadiusX,
				scaledSearchRadiusY,
				minR,
				rod,
				maxCurvature,
				query,
				scaledSourceMatches );

		for ( final PointMatch p : scaledSourceMatches )
		{
			final double[] l1 = p.getP1().getL().clone();
			final double[] l2 = p.getP2().getL().clone();
			l1[ 0 ] /= scale;
			l1[ 1 ] /= scale;
			l2[ 0 ] /= scale;
			l2[ 1 ] /= scale;

			final double tx = l2[ 0 ] - l1[ 0 ];
			final double ty = l2[ 1 ] - l1[ 1 ];

			observer.add( Math.sqrt( tx * tx + ty * ty ) );

			transform.applyInPlace( l2 );
			sourceMatches.add( new PointMatch( scaledSourcePoints.get( p.getP1() ), new Point( l2 ) ) );
		}
	}

    /**
     * Estimate {@linkplain PointMatch point correspondences} for a
     * {@link Collection} of {@link Point Points} among two images that are
     * approximately related by an {@link InvertibleCoordinateTransform} using
     * the Pearson product-moment correlation coefficient (PMCC) <i>r</i> of
     * pixel intensities as similarity measure.  Only correspondence candidates
     * with <i>r</i> >= a given threshold are accepted.
     *
     * @param scaledSource
     * @param target
     * @param scale [0,1]
     * @param transform transfers source into target approximately
     * @param scaledBlockRadiusX horizontal radius of a block
     * @param scaledBlockRadiusY vertical radius of a block
     * @param scaledSearchRadiusX horizontal search radius
     * @param scaledSearchRadiusY vertical search radius
     * @param minR minimal accepted Cross-Correlation coefficient
     * @param sourcePoints
     * @param sourceMatches
     */
    static public void matchByMaximalPMCC(
			final FloatProcessor source,
			final FloatProcessor target,
			final FloatProcessor sourceMask,
			final FloatProcessor targetMask,
			final float scale,
			final CoordinateTransform transform,
			final int blockRadiusX,
			final int blockRadiusY,
			final int searchRadiusX,
			final int searchRadiusY,
			final Collection< ? extends Point > sourcePoints,
			final Collection< PointMatch > sourceMatches,
			final ErrorStatistic observer ) throws InterruptedException, ExecutionException
	{
    	matchByMaximalPMCC(
    			source,
    			target,
    			sourceMask,
    			targetMask,
    			scale,
    			transform,
    			blockRadiusX,
    			blockRadiusY,
    			searchRadiusX,
    			searchRadiusY,
    			0.7f,				// minR
    			0.9f,				// rod
    			10.0f,				// maxCurvature
    			sourcePoints,
    			sourceMatches,
    			observer );
	}

    public static void findMatches(
    		final FloatProcessor source,
    		final FloatProcessor target,
    		final FloatProcessor sourceMask,
			final FloatProcessor targetMask,
			final Model< ? > initialModel,
			final Class< ? extends AbstractAffineModel2D< ? > > localModelClass,
			final float maxEpsilon,
			final float maxScale,
			final float minR,
			final float rodR,
			final float maxCurvatureR,
			final int meshResolution,
			final float alpha,
			final Collection< PointMatch > sourceMatches ) throws InterruptedException, ExecutionException
	{
		CoordinateTransform ict = initialModel;
		final Collection< Point > sourcePoints = new ArrayList< Point >();

		final ErrorStatistic observer = new ErrorStatistic( 1 );

		for ( int n = Math.max( 4, Math.min( ( int )meshResolution, ( int )Math.ceil( source.getWidth() / maxEpsilon / 4 ) ) ); n <= meshResolution; n *= 2 )
		{
			n = Math.min( meshResolution, n );

			final MovingLeastSquaresTransform mlst = new MovingLeastSquaresTransform();
			try
			{
				mlst.setModel( localModelClass );
			}
			catch ( final Exception e )
			{
				IJ.error( "Invalid local model selected." );
				return;
			}

			//if ( sourceMatches )

			final int searchRadius = observer.n() < mlst.getModel().getMinNumMatches()
					? ( int )Math.ceil( maxEpsilon )
					: ( int )Math.ceil( observer.max );
			final double scale = Math.min(  maxScale, 16.0 / searchRadius );
			final int blockRadius = ( int )Math.ceil( 32 / scale );
//			final int blockRadius = Math.max( 16, 3 * p.imp1.getWidth() / n );
//			final int searchRadius = ( int )( sourceMatches.size() >= mlst.getModel().getMinNumMatches() ? Math.min( p.maxEpsilon + 0.5f, blockRadius / 3 ) : p.maxEpsilon );

			/* block match forward */
			sourcePoints.clear();
			sourceMatches.clear();

			final TransformMesh mesh = new TransformMesh( n, source.getWidth(), source.getHeight() );
			PointMatch.sourcePoints( mesh.getVA().keySet(), sourcePoints );
			observer.clear();
			BlockMatching.matchByMaximalPMCC(
					source,
					target,
					sourceMask,
					targetMask,
					//512.0f / p.imp1.getWidth(),
					scale,
					ict,
					blockRadius,
					blockRadius,
					searchRadius,
					searchRadius,
					minR,
					rodR,
					maxCurvatureR,
					sourcePoints,
					sourceMatches,
					observer );

			IJ.log( "Blockmatching at n = " + n );
			IJ.log( " average offset : " + observer.mean );
			IJ.log( " minimal offset : " + observer.min );
			IJ.log( " maximal offset : " + observer.max );

			if  ( sourceMatches.size() >= mlst.getModel().getMinNumMatches() )
			{
				mlst.setAlpha( alpha );
				try
				{
					mlst.setMatches( sourceMatches );
					ict = mlst;
				}
				catch ( final Exception e ) {}
			}
		}
		// TODO refine the search results by rematching at higher resolution with lower search radius
	}


	/**
	 * Create a Shape that illustrates a {@link Collection} of
	 * {@link PointMatch PointMatches}.
	 *
	 * @return the illustration
	 */
	static public Shape illustrateMatches( final Collection< PointMatch > matches)
	{
		final GeneralPath path = new GeneralPath();

		for ( final PointMatch m : matches )
		{
			final double[] w1 = m.getP1().getW();
			final double[] w2 = m.getP2().getW();
			path.moveTo( w1[ 0 ] - 1, w1[ 1 ] - 1 );
			path.lineTo( w1[ 0 ] - 1, w1[ 1 ] + 1 );
			path.lineTo( w1[ 0 ] + 1, w1[ 1 ] + 1 );
			path.lineTo( w1[ 0 ] + 1, w1[ 1 ] - 1 );
			path.closePath();
			path.moveTo( w1[ 0 ], w1[ 1 ] );
			path.lineTo( w2[ 0 ], w2[ 1 ] );
		}

		return path;
	}
}
