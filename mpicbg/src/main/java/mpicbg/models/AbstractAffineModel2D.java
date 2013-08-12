package mpicbg.models;

import java.awt.geom.AffineTransform;

/**
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.2b
 * 
 */
public abstract class AbstractAffineModel2D< M extends AbstractAffineModel2D< M > > extends AbstractModel< M > implements InvertibleBoundable, InvertibleCoordinateTransform, Affine2D< M >
{
	private static final long serialVersionUID = -1885309124892374359L;

	/**
	 * Create an {@link AffineTransform} representing the current parameters
	 * the model.
	 * 
	 * @return {@link AffineTransform}
	 */
	@Override
	abstract public AffineTransform createAffine();
	
	/**
	 * Create an {@link AffineTransform} representing the inverse of the
	 * current parameters of the model.
	 * 
	 * @return {@link AffineTransform}
	 */
	@Override
	abstract public AffineTransform createInverseAffine();
	
	//@Override
	@Override
	public void estimateBounds( final float[] min, final float[] max )
	{
		assert min.length >= 2 && max.length >= 2 : "2d affine transformations can be applied to 2d points only.";
		
		float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
		
		final float[] l = min.clone();
		applyInPlace( l );
		
		if ( l[ 0 ] < minX ) minX = l[ 0 ];
		if ( l[ 0 ] > maxX ) maxX = l[ 0 ];
		if ( l[ 1 ] < minY ) minY = l[ 1 ];
		if ( l[ 1 ] > maxY ) maxY = l[ 1 ];
		
		l[ 0 ] = min[ 0 ];
		l[ 1 ] = max[ 1 ];
		applyInPlace( l );
		
		if ( l[ 0 ] < minX ) minX = l[ 0 ];
		else if ( l[ 0 ] > maxX ) maxX = l[ 0 ];
		if ( l[ 1 ] < minY ) minY = l[ 1 ];
		else if ( l[ 1 ] > maxY ) maxY = l[ 1 ];
		
		l[ 0 ] = max[ 0 ];
		l[ 1 ] = max[ 1 ];
		applyInPlace( l );
		
		if ( l[ 0 ] < minX ) minX = l[ 0 ];
		else if ( l[ 0 ] > maxX ) maxX = l[ 0 ];
		if ( l[ 1 ] < minY ) minY = l[ 1 ];
		else if ( l[ 1 ] > maxY ) maxY = l[ 1 ];
		
		l[ 0 ] = max[ 0 ];
		l[ 1 ] = min[ 1 ];
		applyInPlace( l );
		
		if ( l[ 0 ] < minX ) minX = l[ 0 ];
		else if ( l[ 0 ] > maxX ) maxX = l[ 0 ];
		if ( l[ 1 ] < minY ) minY = l[ 1 ];
		else if ( l[ 1 ] > maxY ) maxY = l[ 1 ];
		
		min[ 0 ] = minX;
		min[ 1 ] = minY;
		max[ 0 ] = maxX;
		max[ 1 ] = maxY;
	}
	
	//@Override
	@Override
	public void estimateInverseBounds( final float[] min, final float[] max ) throws NoninvertibleModelException
	{
		assert min.length >= 2 && max.length >= 2 : "2d affine transformations can be applied to 2d points only.";
		
		float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
		
		final float[] l = min.clone();
		applyInverseInPlace( l );
		
		if ( l[ 0 ] < minX ) minX = l[ 0 ];
		if ( l[ 0 ] > maxX ) maxX = l[ 0 ];
		if ( l[ 1 ] < minY ) minY = l[ 1 ];
		if ( l[ 1 ] > maxY ) maxY = l[ 1 ];
		
		l[ 0 ] = min[ 0 ];
		l[ 1 ] = max[ 1 ];
		applyInverseInPlace( l );
		
		if ( l[ 0 ] < minX ) minX = l[ 0 ];
		else if ( l[ 0 ] > maxX ) maxX = l[ 0 ];
		if ( l[ 1 ] < minY ) minY = l[ 1 ];
		else if ( l[ 1 ] > maxY ) maxY = l[ 1 ];
		
		l[ 0 ] = max[ 0 ];
		l[ 1 ] = max[ 1 ];
		applyInverseInPlace( l );
		
		if ( l[ 0 ] < minX ) minX = l[ 0 ];
		else if ( l[ 0 ] > maxX ) maxX = l[ 0 ];
		if ( l[ 1 ] < minY ) minY = l[ 1 ];
		else if ( l[ 1 ] > maxY ) maxY = l[ 1 ];
		
		l[ 0 ] = max[ 0 ];
		l[ 1 ] = min[ 1 ];
		applyInverseInPlace( l );
		
		if ( l[ 0 ] < minX ) minX = l[ 0 ];
		else if ( l[ 0 ] > maxX ) maxX = l[ 0 ];
		if ( l[ 1 ] < minY ) minY = l[ 1 ];
		else if ( l[ 1 ] > maxY ) maxY = l[ 1 ];
		
		min[ 0 ] = minX;
		min[ 1 ] = minY;
		max[ 0 ] = maxX;
		max[ 1 ] = maxY;
	}
	
	@Override
	public String toString()
	{
		return ( "[3,3](" + createAffine() + ") " + cost );
	}
	
	@Override
	abstract public void preConcatenate( final M model );
	@Override
	abstract public void concatenate( final M model );
	
	//@Override
	@Override
	abstract public M createInverse();
}
