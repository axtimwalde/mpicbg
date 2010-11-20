package mpicbg.models;

/**
 *
 * @param <M> the {@link InvertibleModel} class itself
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 */
public interface InvertibleModel< M extends InvertibleModel< M > > extends Model< M >, InvertibleCoordinateTransform
{ 
}
