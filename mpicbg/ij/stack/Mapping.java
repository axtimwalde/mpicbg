package mpicbg.ij.stack;

import mpicbg.models.InverseCoordinateTransform;
import ij.ImageStack;
import ij.process.ImageProcessor;

/**
 * Describes a mapping from {@linkplain ImageStack source} into
 * {@linkplain ImageProcessor target}.
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 */
public interface Mapping< T extends InverseCoordinateTransform >
{
	public T getTransform();
	
	/**
	 * Map {@linkplain ImageStack source} into
	 * {@linkplain ImageProcessor target}
	 * 
	 * @param source
	 * @param target
	 */
	public void map(
			ImageStack source,
			ImageProcessor target );
	
	/**
	 * Map {@linkplain ImageStack source} into
	 * {@linkplain ImageProcessor target} using bilinear interpolation.
	 * 
	 * @param source
	 * @param target
	 */
	public void mapInterpolated(
			ImageStack source,
			ImageProcessor target );
}
