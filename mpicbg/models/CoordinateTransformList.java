package mpicbg.models;

import java.util.ArrayList;
import java.util.List;

import mpicbg.util.Util;

/**
 * TODO Think about if it should really implement Boundable.  There is no
 *   adequate solution for estimating the bounding box correctly instead of
 *   approximative as implemented here.
 *   
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.4b
 */
public class CoordinateTransformList< E extends CoordinateTransform > implements Boundable
{

	final protected List< E > l = new ArrayList< E >();
	
	final public void add( E t ){ l.add( t ); }
	final public void remove( E t ){ l.remove( t ); }
	final public E remove( int i ){ return l.remove( i ); }
	final public E get( int i ){ return l.get( i ); }
	final public void clear(){ l.clear(); }
	
	//@Override
	final public float[] apply( final float[] location )
	{
		final float[] a = location.clone();
		applyInPlace( a );
		return a;
	}

	//@Override
	final public void applyInPlace( final float[] location )
	{
		for ( final E t : l )
			t.applyInPlace( location );
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * Estimate the bounds approximately by iteration over a fixed grid of
	 * exemplary locations.
	 * 
	 * TODO Find a better solution.
	 */
	//@Override
	public void estimateBounds( final float[] min, final float[] max )
	{
		assert min.length == max.length : "min and max have to have equal length.";
		
		final int g = 32;
		
		final float[] minBounds = new float[ min.length ];
		final float[] maxBounds = new float[ min.length ];
		final float[] s = new float[ min.length ];
		final int[] i = new int[ min.length ];
		final float[] l = new float[ min.length ];
		
		for ( int k = 0; k < min.length; ++k )
		{
			minBounds[ k ] = Float.MAX_VALUE;
			maxBounds[ k ] = -Float.MAX_VALUE;
			s[ k ] = ( max[ k ] - min[ k ] ) / ( g - 1 );
			l[ k ] = min[ k ];
		}
		
		final long d = Util.pow( g, min.length );
		
		for ( long j = 0; j < d; ++j )
		{
			final float[] m = apply( l );
			for ( int k = 0; k < min.length; ++k )
			{
				if ( m[ k ] < minBounds[ k ] ) minBounds[ k ] = m[ k ];
				if ( m[ k ] > maxBounds[ k ] ) maxBounds[ k ] = m[ k ];
			}
			
//			for ( final int k : i )
//				System.out.print( k + " " );
//			System.out.print( ": " );
//			for ( final float k : l )
//				System.out.print( k + " " );
//			System.out.print( "-> " );
//			for ( final float k : m )
//				System.out.print( k + " " );
//			System.out.println();
			
			for ( int k = 0; k < min.length; ++k )
			{
				++i[ k ];
				if ( i[ k ] >= g )
				{
					i[ k ] = 0;
					l[ k ] = min[ k ];
					continue;
				}
				l[ k ] = min[ k ] + i[ k ] * s[ k ];
				break;
			}
		}
		
		for ( int k = 0; k < min.length; ++k )
		{
			min[ k ] = minBounds[ k ];
			max[ k ] = maxBounds[ k ];
		}
	}
}
