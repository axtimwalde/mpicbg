package mpicbg.models;

/**
 *
 * @param <M> the {@link InvertibleModel} class itself
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 */
public abstract class InvertibleModel< M extends InvertibleModel< M > > extends Model< M > implements InvertibleCoordinateTransform
{
	@Override
	abstract public M clone(); 
}
