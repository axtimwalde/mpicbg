package mpicbg.ij.blockmatching;

import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.util.Collection;
import java.util.Random;

import ij.IJ;
import ij.ImagePlus;
import ij.process.FloatProcessor;
import mpicbg.ij.InverseMapping;
import mpicbg.ij.TransformMapping;
import mpicbg.ij.util.Util;
import mpicbg.models.CoordinateTransform;
import mpicbg.models.CoordinateTransformList;
import mpicbg.models.InvertibleCoordinateTransform;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
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
		
		float sum = 0;
		for ( int y = ty + blockHeight - 1; y >= ty; --y )
		{
			final int ry = y * width;
			for ( int x = tx + blockWidth - 1; x >= tx; --x )
				sum += pixels[ ry + x ];
		}
		return sum / ( blockWidth * blockHeight );
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
		
		float sum = 0;
		for ( int y = ty + blockHeight - 1; y >= ty; --y )
		{
			final int ry = y * width;
			for ( int x = tx + blockWidth - 1; x >= tx; --x )
			{
				final float a = pixels[ ry + x ] - mean;
				sum += a * a;
			}
		}
		return sum / ( blockWidth * blockHeight );
	}
	
	/**
	 * Normalize the dynamic range of a {@link FloatProcessor} to the interval [0,1].
	 * 
	 * @param fp
	 * @param scale
	 */
    static protected void normalizeContrast( final FloatProcessor fp )
    {
    	final float[] data = ( float[] )fp.getPixels();
    	float min = data[ 0 ];
    	float max = min;
    	for ( final float f : data )
    	{
    		if ( f < min ) min = f;
    		else if ( f > max ) max = f;
    	}
    	final float s = 1 / ( max - min );
    	for ( int i = 0; i < data.length; ++i )
    		data[ i ] = s * ( data[ i ] - min );
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
			final Collection< Point > sourcePoints,
			final Collection< PointMatch > sourceMatches )
	{
		normalizeContrast( source );
		normalizeContrast( target );
		
		final FloatProcessor mappedTarget = new FloatProcessor( source.getWidth() + 2 * searchRadiusX, source.getHeight() + 2 * searchRadiusY );
		Util.fillWithNaN( mappedTarget );
		
		final TranslationModel2D tTarget = new TranslationModel2D();
		tTarget.set( -searchRadiusX, -searchRadiusY );
		final CoordinateTransformList lTarget = new CoordinateTransformList();
		lTarget.add( tTarget );
		lTarget.add( transform );
		final InverseMapping< ? > targetMapping = new TransformMapping< CoordinateTransform >( lTarget );
		targetMapping.mapInverseInterpolated( target, mappedTarget );
		
		mappedTarget.setMinAndMax( 0, 1 );
		new ImagePlus( "Mapped Target", mappedTarget ).show();
		
		int k = 0;
		for ( final Point p : sourcePoints )
		{
			final float[] s = p.getL();
			final int px = Math.round( s[ 0 ] );
			final int py = Math.round( s[ 1 ] );
			if (
					px - blockRadiusX >= 0 &&
					px + blockRadiusX < source.getWidth() &&
					py - blockRadiusY >= 0 &&
					py + blockRadiusY < source.getHeight() )
			{
				float tx = 0;
				float ty = 0;
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
				final float[] t = new float[]{ tx + s[ 0 ], ty + s[ 1 ] };
				IJ.log( ++k + " : " + tx + ", " + ty );
				transform.applyInPlace( t );
				sourceMatches.add( new PointMatch( p, new Point( t ) ) );
			}
		}
	}
    
    
    /**
     * Estimate {@linkplain PointMatch point correspondences} for a
     * {@link Collection} of {@link Point Points} among two images that are
     * approximately related by an {@link InvertibleCoordinateTransform} using
     * the cross-correlation coefficient (CCC) of pixel intensities as
     * similarity measure.  Only correspondence candidates with a CCC >= a given
     * threshold are accepted.
     *  
     * @param source
     * @param target
     * @param transform transfers source into target approximately
     * @param blockRadiusX horizontal radius of a block
     * @param blockRadiusY vertical radius of a block
     * @param searchRadiusX horizontal search radius
     * @param searchRadiusY vertical search radius
     * @param minCCC minimal accepted Cross-Correlation coefficient
     * @param sourcePoints
     * @param sourceMatches
     */
    static public void matchByNormalizedCrossCorrelation(
			final FloatProcessor source,
			final FloatProcessor target,
			final InvertibleCoordinateTransform transform,
			final int blockRadiusX,
			final int blockRadiusY,
			final int searchRadiusX,
			final int searchRadiusY,
			final float minCCC,
			final Collection< Point > sourcePoints,
			final Collection< PointMatch > sourceMatches )
	{
    	final int blockWidth = 2 * blockRadiusX + 1;
    	final int blockHeight = 2 * blockRadiusY + 1;
    	
		normalizeContrast( source );
		normalizeContrast( target );
		
		final FloatProcessor mappedTarget = new FloatProcessor( source.getWidth() + 2 * searchRadiusX, source.getHeight() + 2 * searchRadiusY );
		Util.fillWithNoise( mappedTarget );
		
		final TranslationModel2D tTarget = new TranslationModel2D();
		tTarget.set( -searchRadiusX, -searchRadiusY );
		final CoordinateTransformList lTarget = new CoordinateTransformList();
		lTarget.add( tTarget );
		lTarget.add( transform );
		final InverseMapping< ? > targetMapping = new TransformMapping< CoordinateTransform >( lTarget );
		targetMapping.mapInverseInterpolated( target, mappedTarget );
		
		mappedTarget.setMinAndMax( 0, 1 );
		new ImagePlus( "Mapped Target", mappedTarget ).show();
		
		int k = 0;
		for ( final Point p : sourcePoints )
		{
			final float[] s = p.getL();
			final int px = Math.round( s[ 0 ] );
			final int py = Math.round( s[ 1 ] );
			final int ptx = px - blockRadiusX;
			final int pty = py - blockRadiusY;
			if (
					ptx >= 0 &&
					ptx + blockWidth <= source.getWidth() &&
					pty >= 0 &&
					pty + blockHeight <= source.getHeight() )
			{
				final float sourceBlockMean = blockMean( source, ptx, pty, blockWidth, blockHeight );
				final float sourceBlockStd = ( float )Math.sqrt( blockVariance( source, ptx, pty, blockWidth, blockHeight, sourceBlockMean ) );
				float tx = 0;
				float ty = 0;
				float cccMax = -Float.MAX_VALUE;
				for ( int ity = -searchRadiusY; ity <= searchRadiusY; ++ity )
				{
					final int ipty = ity + pty + searchRadiusY;
					for ( int itx = -searchRadiusX; itx <= searchRadiusX; ++itx )
					{
						final int iptx = itx + ptx + searchRadiusX;
						
						final float targetBlockMean = blockMean( mappedTarget, iptx, ipty, blockWidth, blockHeight );
						final float targetBlockStd = ( float )Math.sqrt( blockVariance( mappedTarget, iptx, ipty, blockWidth, blockHeight, targetBlockMean ) );
						
						float ccc = 0;
						for ( int iy = 0; iy <= blockHeight; ++iy )
						{
							final int ys = pty + iy;
							final int yt = ipty + iy;
							for ( int ix = 0; ix <= blockWidth; ++ix )
							{
								final int xs = ptx + ix;
								final int xt = iptx + ix;
								ccc += ( source.getf( xs, ys ) - sourceBlockMean ) * ( mappedTarget.getf( xt, yt ) - targetBlockMean );
							}
						}
						ccc /= sourceBlockStd;
						ccc /= targetBlockStd;
						ccc /= blockWidth * blockHeight;
						if ( ccc > cccMax )
						{
							cccMax = ccc;
							tx = itx;
							ty = ity;
						}
					}
				}
				if ( cccMax >= minCCC )
				{
					final float[] t = new float[]{ tx + s[ 0 ], ty + s[ 1 ] };
					IJ.log( ++k + " : " + tx + ", " + ty + "  => " + cccMax );
					transform.applyInPlace( t );
					sourceMatches.add( new PointMatch( p, new Point( t ) ) );
				}
			}
		}
	}
    
    
	/**
	 * Create a Shape that illustrates a {@link Collection} of
	 * {@link PointMatch PointMatches}. 
	 * 
	 * @return the illustration
	 */
	static public Shape illustrateMatches( final Collection< PointMatch > matches)
	{
		GeneralPath path = new GeneralPath();
		
		for ( final PointMatch m : matches )
		{
			final float[] w1 = m.getP1().getW();
			final float[] w2 = m.getP2().getW();
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
