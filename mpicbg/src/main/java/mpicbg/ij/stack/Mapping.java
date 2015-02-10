package mpicbg.ij.stack;

import ij.ImageStack;
import ij.process.ImageProcessor;
import mpicbg.models.CoordinateTransform;
import mpicbg.models.InverseCoordinateTransform;

/**
 * Describes a mapping from {@linkplain ImageStack source} into
 * {@linkplain ImageProcessor target}.
 *
 * The generic parameter <em>T</em> is the function generating the mapping,
 * usually a {@link CoordinateTransform} or an
 * {@link InverseCoordinateTransform}.
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 */
public interface Mapping< T >
{
	public T getTransform();

	/**
	 * Map {@linkplain ImageStack source} into {@linkplain ImageProcessor
	 * target}
	 *
	 * @param source
	 * @param target
	 */
	public void map( ImageStack source, ImageProcessor target );

	/**
	 * Map {@linkplain ImageStack source} into {@linkplain ImageProcessor
	 * target} using bilinear interpolation.
	 *
	 * @param source
	 * @param target
	 */
	public void mapInterpolated( ImageStack source, ImageProcessor target );

	/**
	 * Set the slice
	 *
	 * @param slice
	 */
	public void setSlice( final float slice );
}
