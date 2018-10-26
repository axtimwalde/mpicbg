package mpicbg.ij;

import mpicbg.models.CoordinateTransform;
import ij.process.ImageProcessor;

/**
 * Describes an inverse mapping from {@linkplain ImageProcessor source} into
 * {@linkplain ImageProcessor target}.
 * 
 * @author Stephan Saalfeld &lt;saalfeld@mpi-cbg.de&gt;
 * @version 0.1b
 */
public interface InverseMapping< T extends CoordinateTransform >
{
	public T getTransform();
	
	/**
	 * Map inversely {@linkplain ImageProcessor source} into
	 * {@linkplain ImageProcessor target}
	 * 
	 * @param source
	 * @param target
	 */
	public void mapInverse(
			ImageProcessor source,
			ImageProcessor target );
	
	/**
	 * Map inversely {@linkplain ImageProcessor source} into
	 * {@linkplain ImageProcessor target} using bilinear interpolation.
	 * 
	 * @param source
	 * @param target
	 */
	public void mapInverseInterpolated(
			ImageProcessor source,
			ImageProcessor target );
}
