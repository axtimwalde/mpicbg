package mpicbg.ij;

import mpicbg.models.InvertibleCoordinateTransform;
import ij.process.ImageProcessor;

/**
 * Describes an invertible (bidirectional) mapping from
 * {@linkplain ImageProcessor source} into {@linkplain ImageProcessor target}.
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 */
public interface InvertibleMapping< T extends InvertibleCoordinateTransform > extends Mapping< T >, InverseMapping< T > {}
