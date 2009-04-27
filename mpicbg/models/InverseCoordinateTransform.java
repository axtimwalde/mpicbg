package mpicbg.models;

/**
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 */
public interface InverseCoordinateTransform
{
	/**
	 * Apply the inverse of the model to a point location
	 * 
	 * @param point
	 * @return transformed point
	 */
	public float[] applyInverse( float[] point ) throws NoninvertibleModelException;

	
	/**
	 * apply the inverse of the model to a point location
	 * 
	 * @param point
	 */
	public void applyInverseInPlace( float[] point ) throws NoninvertibleModelException;
}
