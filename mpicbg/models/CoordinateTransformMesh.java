package mpicbg.models;

import java.util.Set;

/**
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 */
public class CoordinateTransformMesh extends TransformMesh
{
	public CoordinateTransformMesh(
			final CoordinateTransform t,
			final int numX,
			final float width,
			final float height )
	{
		super( numX, numY( numX, width, height ), width, height );
		
		final Set< PointMatch > vertices = va.keySet();
		for ( final PointMatch vertex : vertices )
			vertex.getP2().apply( t );
		
		updateAffines();
	}
}
