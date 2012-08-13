package mpicbg.models;

/**
 * An {@link InverseCoordinateTransform} that, for a given source interval in
 * <i>n</i>-space, can estimate the target interval in <i>n</i>-space.
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 */
public interface InverseBoundable extends InverseCoordinateTransform
{
	/**
	 * Estimate the bounds of an n-dimensional interval [min,max] with min and
	 * max being n-dimensional vectors.
	 * 
	 * @param min
	 * @param max
	 */
	public void estimateInverseBounds( final float[] min, final float[] max ) throws NoninvertibleModelException;
}
