package mpicbg.ij;

import ij.process.ImageProcessor;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Set;
import mpicbg.models.AffineModel2D;
import mpicbg.models.PointMatch;
import mpicbg.models.TransformMesh;

/**
 * Use a {@link TransformMesh} to map and map inversely
 * {@linkplain ImageProcessor source} into {@linkplain ImageProcessor target}
 * which is an {@link InvertibleMapping}.
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 */
public class TransformMeshMapping extends InvertibleTransformMapping< TransformMesh >
{
	public TransformMeshMapping( final TransformMesh t )
	{
		super( t );
	}
	
	/**
	 * 
	 * @param pm PointMatches
	 * @param min x = min[0], y = min[1]
	 * @param max x = max[0], y = max[1]
	 */
	final static private void calculateBoundingBox(
			final ArrayList< PointMatch > pm,
			final float[] min,
			final float[] max )
	{
		final float[] first = pm.get( 0 ).getP2().getW();
		min[ 0 ] = first[ 0 ];
		min[ 1 ] = first[ 1 ];
		max[ 0 ] = first[ 0 ];
		max[ 1 ] = first[ 1 ];
		
		for ( final PointMatch p : pm )
		{
			final float[] t = p.getP2().getW();
			if ( t[ 0 ] < min[ 0 ] ) min[ 0 ] = t[ 0 ];
			else if ( t[ 0 ] > max[ 0 ] ) max[ 0 ] = t[ 0 ];
			if ( t[ 1 ] < min[ 1 ] ) min[ 1 ] = t[ 1 ];
			else if ( t[ 1 ] > max[ 1 ] ) max[ 1 ] = t[ 1 ];
		}
	}
	
	final static protected void mapTriangle(
			final TransformMesh m, 
			final AffineModel2D ai,
			final ImageProcessor source,
			final ImageProcessor target )
	{
		final ArrayList< PointMatch > pm = m.getAV().get( ai );
		final float[] min = new float[ 2 ];
		final float[] max = new float[ 2 ];
		calculateBoundingBox( pm, min, max );
		
		final int maxX = ( int )max[ 0 ];
		final int maxY = ( int )max[ 1 ];
		
		final float[] t = new float[ 2 ];
		for ( int y = ( int )min[ 1 ]; y <= maxY; ++y )
		{
			for ( int x = ( int )min[ 0 ]; x <= maxX; ++x )
			{
				t[ 0 ] = x;
				t[ 1 ] = y;
				if ( TransformMesh.isInTargetPolygon( pm, t ) )
				{
					try
					{
						ai.applyInverseInPlace( t );
					}
					catch ( Exception e )
					{
						//e.printStackTrace( System.err );
						continue;
					}
					target.putPixel( x, y, source.getPixel( ( int )( t[ 0 ] + 0.5f ), ( int )( t[ 1 ] + 0.5f ) ) );
				}
			}
		}
	}
	
	final static protected void mapTriangleInterpolated(
			final TransformMesh m, 
			final AffineModel2D ai,
			final ImageProcessor source,
			final ImageProcessor target )
	{
		final ArrayList< PointMatch > pm = m.getAV().get( ai );
		final float[] min = new float[ 2 ];
		final float[] max = new float[ 2 ];
		calculateBoundingBox( pm, min, max );
		
		final int maxX = ( int )max[ 0 ];
		final int maxY = ( int )max[ 1 ];
		
		final float[] t = new float[ 2 ];
		for ( int y = ( int )min[ 1 ]; y <= maxY; ++y )
		{
			for ( int x = ( int )min[ 0 ]; x <= maxX; ++x )
			{
				t[ 0 ] = x;
				t[ 1 ] = y;
				if ( TransformMesh.isInTargetPolygon( pm, t ) )
				{
					try
					{
						ai.applyInverseInPlace( t );
					}
					catch ( Exception e )
					{
						//e.printStackTrace( System.err );
						continue;
					}
					target.putPixel( x, y, source.getPixelInterpolated( t[ 0 ], t[ 1 ] ) );
				}
			}
		}
	}
	
	@Override
	final public void map(
			final ImageProcessor source,
			final ImageProcessor target )
	{
		final Set< AffineModel2D > s = transform.getAV().keySet();
		for ( final AffineModel2D ai : s )
			mapTriangle( transform, ai, source, target );
	}
	
	@Override
	final public void mapInterpolated(
			final ImageProcessor source,
			final ImageProcessor target )
	{
		final Set< AffineModel2D > s = transform.getAV().keySet();
		for ( final AffineModel2D ai : s )
			mapTriangleInterpolated( transform, ai, source, target );
	}
	
	
	/**
	 * 
	 * @param pm PointMatches
	 * @param min x = min[0], y = min[1]
	 * @param max x = max[0], y = max[1]
	 */
	final static private void calculateBoundingBoxInverse(
			final ArrayList< PointMatch > pm,
			final float[] min,
			final float[] max )
	{
		final float[] first = pm.get( 0 ).getP1().getL();
		min[ 0 ] = first[ 0 ];
		min[ 1 ] = first[ 1 ];
		max[ 0 ] = first[ 0 ];
		max[ 1 ] = first[ 1 ];
		
		for ( final PointMatch p : pm )
		{
			final float[] t = p.getP1().getL();
			if ( t[ 0 ] < min[ 0 ] ) min[ 0 ] = t[ 0 ];
			else if ( t[ 0 ] > max[ 0 ] ) max[ 0 ] = t[ 0 ];
			if ( t[ 1 ] < min[ 1 ] ) min[ 1 ] = t[ 1 ];
			else if ( t[ 1 ] > max[ 1 ] ) max[ 1 ] = t[ 1 ];
		}
	}
	
	final static protected void mapTriangleInverse(
			final TransformMesh m, 
			final AffineModel2D ai,
			final ImageProcessor source,
			final ImageProcessor target )
	{
		final ArrayList< PointMatch > pm = m.getAV().get( ai );
		final float[] min = new float[ 2 ];
		final float[] max = new float[ 2 ];
		calculateBoundingBoxInverse( pm, min, max );
		
		final int maxX = ( int )max[ 0 ];
		final int maxY = ( int )max[ 1 ];
		
		final float[] t = new float[ 2 ];
		for ( int y = ( int )min[ 1 ]; y <= maxY; ++y )
		{
			for ( int x = ( int )min[ 0 ]; x <= maxX; ++x )
			{
				t[ 0 ] = x;
				t[ 1 ] = y;
				if ( TransformMesh.isInSourcePolygon( pm, t ) )
				{
					ai.applyInPlace( t );
					target.putPixel( x, y, source.getPixel( ( int )( t[ 0 ] + 0.5f ), ( int )( t[ 1 ] + 0.5f ) ) );
				}
			}
		}
	}
	
	final static protected void mapTriangleInverseInterpolated(
			final TransformMesh m, 
			final AffineModel2D ai,
			final ImageProcessor source,
			final ImageProcessor target )
	{
		final ArrayList< PointMatch > pm = m.getAV().get( ai );
		final float[] min = new float[ 2 ];
		final float[] max = new float[ 2 ];
		calculateBoundingBoxInverse( pm, min, max );
		
		final int maxX = ( int )max[ 0 ];
		final int maxY = ( int )max[ 1 ];
		
		final float[] t = new float[ 2 ];
		for ( int y = ( int )min[ 1 ]; y <= maxY; ++y )
		{
			for ( int x = ( int )min[ 0 ]; x <= maxX; ++x )
			{
				t[ 0 ] = x;
				t[ 1 ] = y;
				if ( TransformMesh.isInSourcePolygon( pm, t ) )
				{
					ai.applyInPlace( t );
					target.putPixel( x, y, source.getPixelInterpolated( t[ 0 ], t[ 1 ] ) );
				}
			}
		}
	}
	
	//@Override
	final public void mapInverse(
			final ImageProcessor source,
			final ImageProcessor target )
	{
		target.setColor( Color.black );
		target.fill();
		final Set< AffineModel2D > s = transform.getAV().keySet();
		for ( final AffineModel2D ai : s )
			mapTriangleInverse( transform, ai, source, target );
	}
	
	//@Override
	final public void mapInverseInterpolated(
			final ImageProcessor source,
			final ImageProcessor target )
	{
		target.setColor( Color.black );
		target.fill();
		final Set< AffineModel2D > s = transform.getAV().keySet();
		for ( final AffineModel2D ai : s )
			mapTriangleInverseInterpolated( transform, ai, source, target );
	}
}
