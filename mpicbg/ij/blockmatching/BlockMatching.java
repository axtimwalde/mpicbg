package mpicbg.ij.blockmatching;

import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.util.Collection;
import java.util.Random;

import ij.IJ;
import ij.process.FloatProcessor;
import mpicbg.ij.InverseMapping;
import mpicbg.ij.TransformMapping;
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
    
    static protected void fillWithNoise( final FloatProcessor fp )
    {
    	final float[] data = ( float[] )fp.getPixels();
    	final Random random = new Random( System.nanoTime() );
    	for ( int i = 0; i < data.length; ++i )
    		data[ i ] = random.nextFloat();
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
		fillWithNoise( mappedTarget );
		
		final TranslationModel2D tTarget = new TranslationModel2D();
		tTarget.set( -searchRadiusX, -searchRadiusY );
		final CoordinateTransformList lTarget = new CoordinateTransformList();
		lTarget.add( tTarget );
		lTarget.add( transform );
		final InverseMapping< ? > targetMapping = new TransformMapping< CoordinateTransform >( lTarget );
		targetMapping.mapInverseInterpolated( target, mappedTarget );
		
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
						for ( int iy = -blockRadiusY; iy <= blockRadiusY; ++iy )
						{
							final int y = py + iy;
							for ( int ix = -blockRadiusX; ix <= blockRadiusX; ++ix )
							{
								final int x = px + ix;
								final float a = source.getf( x, y ) - mappedTarget.getf( x + itx + searchRadiusX, y + ity + searchRadiusY );
								d += a * a;
							}
							if ( d < dMin )
							{
								dMin = d;
								tx = itx;
								ty = ity;
							}
						}
					}
				final float[] t = new float[]{ tx + s[ 0 ], ty + s[ 1 ] };
				IJ.log( tx + ", " + ty );
				transform.applyInPlace( t );
				sourceMatches.add( new PointMatch( p, new Point( t ) ) );
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
