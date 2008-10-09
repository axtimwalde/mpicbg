/**
 * 
 */
package mpicbg.models;

import java.util.ArrayList;
import java.util.Set;

/**
 * @author saalfeld
 *
 */
public class TransformMeshMap2D extends CoordinateTransformMap2D
{
	public TransformMeshMap2D( final TransformMesh t, int width, int height )
	{
		super( new float[ height ][ width * 2 ] );
		final float[] min = new float[ 2 ];
		final float[] max = new float[ 2 ];
		final float[] l = new float[ 2 ];
		
		for ( int y = 0; y < map.length; ++y )
			for ( int x = 0; x < map[ y ].length; ++x )
				map[ y ][ x ] = Float.NaN;
			
		Set< AffineModel2D > s = t.getAV().keySet();
		for ( AffineModel2D ai : s )
		{
			final ArrayList< PointMatch > pm = t.getAV().get( ai );
			calculateBoundingBoxInverse( pm, min, max );
				
			for ( int y = ( int )min[ 1 ]; y <= max[ 1 ]; ++y )
			{
				if ( y >= 0 && y < map.length )
					for ( int x = ( int )min[ 0 ]; x <= max[ 0 ]; ++x )
					{
						final int xi = 2 * x;
						if ( x >= 0 && xi < map[ y ].length )
						{
							l[ 0 ] = x;
							l[ 1 ] = y;
							if ( TransformMesh.isInSourcePolygon( pm, l ) )
							{
								ai.applyInPlace( l );
								map[ y ][ xi ] = l[ 0 ];
								map[ y ][ xi + 1 ] = l[ 1 ];
							}
						}
					}
			}
		}
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
}
