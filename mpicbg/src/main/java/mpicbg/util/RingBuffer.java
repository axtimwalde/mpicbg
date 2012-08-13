package mpicbg.util;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 */
public class RingBuffer< T > extends AbstractList< T >
{
	final static protected IndexOutOfBoundsException indexOutOfBoundsException = new IndexOutOfBoundsException( "RingBuffer index out of bounds" );
	final static protected NoSuchElementException noSuchElementException = new NoSuchElementException( "RingBuffer has no such element" );
	final static protected IllegalStateException illegalStateException = new IllegalStateException( "RingBuffer is in an illegal state" );
	
	protected class RingBufferIterator implements Iterator< T >
	{
		protected int index = 0;
		protected int elementIndex = -1;
		
		//@Override
		public boolean hasNext() { return ( index < size ); }

		//@Override
		public T next()
		{
			if ( !hasNext() ) throw noSuchElementException;
			elementIndex = index++;
			return  buffer[ elementIndex ];
		}
		
		/**
		 * TODO Should we implement this?  It is very random, where an iterator ...
		 */
		//@Override
		public void remove()
		{
			throw new UnsupportedOperationException();
		}
	}
	
	protected class RingBufferListIterator implements ListIterator< T >
	{
		protected int realIndex;
		protected int bufferIndex;
		protected int elementIndex;
		
		protected RingBufferListIterator()
		{
			realIndex = nextRealIndex - size;
			bufferIndex = realIndex % buffer.length;
			elementIndex = -1;
		}
		
		protected RingBufferListIterator( final int index )
		{
			if ( indexOutOfBounds( index ) ) throw indexOutOfBoundsException;
			realIndex = index;
			bufferIndex = realIndex % buffer.length;
			elementIndex = -1;
		}
		
		public int nextIndex() { return realIndex + 1; }
		public int previousIndex() { return realIndex - 1; }
		
		//@Override
		public boolean hasNext() { return ( realIndex < nextRealIndex ); }

		//@Override
		public boolean hasPrevious() { return ( realIndex > nextRealIndex - size ); }

		//@Override
		public T next()
		{
			if ( !hasNext() ) throw noSuchElementException;
			elementIndex = bufferIndex;
			bufferIndex = ++realIndex % buffer.length;
			return  buffer[ elementIndex ];
		}
		
		//@Override
		public T previous()
		{
			if ( !hasPrevious() ) throw noSuchElementException;
			bufferIndex = --realIndex % buffer.length;
			elementIndex = bufferIndex;
			return  buffer[ elementIndex ];
		}

		//@Override
		public void add( final T element)
		{
			RingBuffer.this.add( realIndex, element );
		}
		
		//@Override
		public void remove()
		{
			if ( elementIndex == -1 ) throw illegalStateException;
			RingBuffer.this.removeUnsafe( realIndex );
			bufferIndex = --realIndex % buffer.length;
			elementIndex = -1;
		}
		
		//@Override
		public void set( final T element )
		{
			if ( elementIndex == -1 ) throw illegalStateException;
			buffer[ elementIndex ] = element;
		}
	}
	
	final protected T[] buffer;
	
	/* real index */
	protected int nextRealIndex = 0;
	
	/* buffer index  */
	protected int size = 0;
	protected int nextBufferIndex = 0;
	
	@SuppressWarnings( "unchecked" )
	public RingBuffer( final int capacity )
	{
		buffer = ( T[] )new Object[ capacity ];
	}
	
	public boolean containsIndex( final int index )
	{
		return !indexOutOfBounds( index );
	}
	
	protected boolean indexOutOfBounds( final int index )
	{
		return index >= nextRealIndex || nextRealIndex - index - 1 >= size;
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * Doesn not check for index < 0 for efficiency resons, so be sure not to
	 * pass negative indices.
	 */
	@Override
	public T get( final int index ) throws IndexOutOfBoundsException
	{
		//if ( indexOutOfBounds( index ) ) throw indexOutOfBoundsException;
		if ( indexOutOfBounds( index ) ) throw new IndexOutOfBoundsException();
		final int bufferIndex = index % buffer.length;
		return buffer[ bufferIndex ]; 
	}
	
	@Override
	public boolean add( final T element )
	{
		buffer[ nextBufferIndex ] = element;
		nextBufferIndex = ++nextRealIndex % buffer.length;
		size = Math.min( ++size, buffer.length );
		return true;
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * Doesn not check for index < 0 for efficiency resons, so be sure not to
	 * pass negative indices.
	 */
	@Override
	public void add( final int index, final T element ) throws IndexOutOfBoundsException
	{
		if ( index > nextRealIndex || nextRealIndex - index - 1 >= size ) throw indexOutOfBoundsException;
		for ( int k = nextRealIndex; k > index; --k )
			buffer[ k % buffer.length ] = buffer[ ( k - 1 ) % buffer.length ];
		nextBufferIndex = ++nextRealIndex % buffer.length;
		size = Math.min( ++size, buffer.length );
		buffer[ index % buffer.length ] = element;
	}
	
	protected T removeUnsafe( final int index )
	{
		final T element = buffer[ index % buffer.length ];
		for ( int k = index + 1; k <= nextRealIndex; ++k )
			buffer[ ( k - 1 ) % buffer.length ] = buffer[ k % buffer.length ];
			
		nextBufferIndex = Math.max( 0, --nextRealIndex ) % buffer.length;
		size = Math.min( Math.max( 0, --size ), buffer.length );
		return element;
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * Doesn not check for index < 0 for efficiency resons, so be sure not to
	 * pass negative indices.
	 */
	@Override
	public T remove( final int index ) throws IndexOutOfBoundsException
	{
		if ( indexOutOfBounds( index ) ) throw indexOutOfBoundsException;
		return removeUnsafe( index );
	}
	
	@Override
	public void clear()
	{
		for ( int i = 0; i < buffer.length; ++i )
			buffer[ i ] = null;
		nextRealIndex = 0;
		nextBufferIndex = 0;
		size = 0;
	}
	
	@Override
	public Iterator< T > iterator() { return new RingBufferIterator(); }

	@Override
	public ListIterator< T > listIterator() { return new RingBufferListIterator(); }
	
	@Override
	public ListIterator< T > listIterator( final int index ) { return new RingBufferListIterator( index ); }
	
	@Override
	public T[] toArray()
	{
		@SuppressWarnings( "unchecked" )
		final T[] array = ( T[] )new Object[ size ];
		
		final ListIterator< T > i = listIterator();
		int k = -1;
		while ( i.hasNext() )
			array[ ++k ] = i.next();
		return array;
	}

	@Override
	public int size(){ return size; }
	
	public int nextIndex(){ return nextRealIndex; }
	
	public int lastIndex(){ return nextRealIndex - 1; }
}
