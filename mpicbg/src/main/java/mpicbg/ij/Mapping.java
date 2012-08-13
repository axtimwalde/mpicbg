package mpicbg.ij;

import mpicbg.models.InverseCoordinateTransform;
import ij.process.ImageProcessor;

/**
 * Describes a mapping from {@linkplain ImageProcessor source} into
 * {@linkplain ImageProcessor target}.
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 */
public interface Mapping< T extends InverseCoordinateTransform >
{
	public T getTransform();
	
	/**
	 * Map {@linkplain ImageProcessor source} into
	 * {@linkplain ImageProcessor target}
	 * 
	 * @param source
	 * @param target
	 */
	public void map(
			ImageProcessor source,
			ImageProcessor target );
	
	/**
	 * Map {@linkplain ImageProcessor source} into
	 * {@linkplain ImageProcessor target} using bilinear interpolation.
	 * 
	 * @param source
	 * @param target
	 */
	public void mapInterpolated(
			ImageProcessor source,
			ImageProcessor target );
}
