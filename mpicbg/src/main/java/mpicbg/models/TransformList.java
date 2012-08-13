package mpicbg.models;

import java.util.List;

/**
 * A generic list of transforms
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 */
public interface TransformList< E >
{
	public void add( E t );
	public void remove( E t );
	E remove( int i );
	public E get( int i );
	public void clear();
	public List< E > getList( final List< E > preAllocatedList );
}
