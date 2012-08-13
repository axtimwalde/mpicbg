package mpicbg.models;

/**
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 */
public interface CoordinateTransform
{
	/**
	 * Apply the {@link CoordinateTransform} to a location.
	 * 
	 * @param location
	 * @return transformed location
	 */
	public float[] apply( float[] location );

	
	/**
	 * Apply the {@link CoordinateTransform} to a location.
	 * 
	 * @param location
	 * @return transformed location
	 */
	public void applyInPlace( float[] location );
}
