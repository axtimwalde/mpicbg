package mpicbg.models;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;

/**
 * 2d-affine transformation models to be applied to points in 2d-space.
 * 
 * TODO: Implement this as a non-abstract affine transformation.
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 *
 */
public abstract class AffineModel2D extends Model
{
	static final protected int MIN_SET_SIZE = 3;
	
	@Override
	public int getMinSetSize(){ return MIN_SET_SIZE; }
	
	final protected AffineTransform affine = new AffineTransform();
	public AffineTransform getAffine(){	return affine; }
	
	
	@Override
	public float[] apply( float[] point )
	{
		float[] transformed = new float[ 2 ];
		affine.transform( point, 0, transformed, 0, 1 );
		return transformed;
	}
	
	
	@Override
	public void applyInPlace( float[] point )
	{
		affine.transform( point, 0, point, 0, 1 );
	}
	
	
	@Override
	public float[] applyInverse( float[] point ) throws NoninvertibleModelException
	{
		// the brilliant java.awt.geom.AffineTransform implements transform for float[] but inverseTransform for double[] only...
		double[] double_point = new double[]{ point[ 0 ], point[ 1 ] };
		double[] transformed = new double[ 2 ];
		
		try
		{
			affine.inverseTransform( double_point, 0, transformed, 0, 1 );
		}
		catch ( NoninvertibleTransformException e )
		{
			throw new NoninvertibleModelException( e );
		}
		
		return new float[]{ ( float )transformed[ 0 ], ( float )transformed[ 1 ] };
	}


	@Override
	public void applyInverseInPlace( float[] point ) throws NoninvertibleModelException
	{
		float[] temp_point = applyInverse( point );
		point[ 0 ] = temp_point[ 0 ];
		point[ 1 ] = temp_point[ 1 ];
	}
}
