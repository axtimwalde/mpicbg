package mpicbg.models;

/**
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 */
public interface InvertibleCoordinateTransform extends CoordinateTransform, InverseCoordinateTransform
{
	public InvertibleCoordinateTransform createInverse();
}
