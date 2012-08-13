package mpicbg.models;

/**
 * A {@link CoordinateTransform} that, for a given source interval in
 * <i>n</i>-space, can estimate the target interval in <i>n</i>-space.
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 */
public interface Boundable extends CoordinateTransform
{
	/**
	 * Estimate the bounds of an n-dimensional interval [min,max] with min and
	 * max being n-dimensional vectors.
	 * 
	 * @param min
	 * @param max
	 */
	public void estimateBounds( final float[] min, final float[] max );
}
