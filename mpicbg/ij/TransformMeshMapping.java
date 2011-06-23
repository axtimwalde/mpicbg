package mpicbg.ij;

import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.models.AffineModel2D;
import mpicbg.models.PointMatch;
import mpicbg.models.TransformMesh;
import mpicbg.util.Util;

/**
 * Use a {@link TransformMesh} to map and map inversely
 * {@linkplain ImageProcessor source} into {@linkplain ImageProcessor target}
 * which is an {@link InvertibleMapping}.
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 */
public class TransformMeshMapping< T extends TransformMesh > extends InvertibleTransformMapping< T >
{
	final static private class MapTriangleThread extends Thread
	{
		final private AtomicInteger i;
		final private List< AffineModel2D > triangles;
		final private TransformMesh transform;
		final ImageProcessor source, target;
		MapTriangleThread(
				final AtomicInteger i,
				final List< AffineModel2D > triangles,
				final TransformMesh transform,
				final ImageProcessor source,
				final ImageProcessor target )
		{
			this.i = i;
			this.triangles = triangles;
			this.transform = transform;
			this.source = source;
			this.target = target;
		}
		
		final public void run()
		{
			int k = i.getAndIncrement();
			while ( !isInterrupted() && k < triangles.size() )
			{
				mapTriangle( transform, triangles.get( k ), source, target );
				k = i.getAndIncrement();
			}
		}
	}
	
	final static private class MapTriangleInterpolatedThread extends Thread
	{
		final private AtomicInteger i;
		final private List< AffineModel2D > triangles;
		final private TransformMesh transform;
		final ImageProcessor source, target;
		MapTriangleInterpolatedThread(
				final AtomicInteger i,
				final List< AffineModel2D > triangles,
				final TransformMesh transform,
				final ImageProcessor source,
				final ImageProcessor target )
		{
			this.i = i;
			this.triangles = triangles;
			this.transform = transform;
			this.source = source;
			this.target = target;
		}
		
		final public void run()
		{
			int k = i.getAndIncrement();
			while ( !isInterrupted() && k < triangles.size() )
			{
				mapTriangleInterpolated( transform, triangles.get( k ), source, target );
				k = i.getAndIncrement();
			}
		}
	}
	
	final static private class MapTriangleInverseThread extends Thread
	{
		final private AtomicInteger i;
		final private List< AffineModel2D > triangles;
		final private TransformMesh transform;
		final ImageProcessor source, target;
		MapTriangleInverseThread(
				final AtomicInteger i,
				final List< AffineModel2D > triangles,
				final TransformMesh transform,
				final ImageProcessor source,
				final ImageProcessor target )
		{
			this.i = i;
			this.triangles = triangles;
			this.transform = transform;
			this.source = source;
			this.target = target;
		}
		
		final public void run()
		{
			int k = i.getAndIncrement();
			while ( !isInterrupted() && k < triangles.size() )
			{
				mapTriangleInverse( transform, triangles.get( k ), source, target );
				k = i.getAndIncrement();
			}
		}
	}
	
	final static private class MapTriangleInverseInterpolatedThread extends Thread
	{
		final private AtomicInteger i;
		final private List< AffineModel2D > triangles;
		final private TransformMesh transform;
		final ImageProcessor source, target;
		MapTriangleInverseInterpolatedThread(
				final AtomicInteger i,
				final List< AffineModel2D > triangles,
				final TransformMesh transform,
				final ImageProcessor source,
				final ImageProcessor target )
		{
			this.i = i;
			this.triangles = triangles;
			this.transform = transform;
			this.source = source;
			this.target = target;
		}
		
		final public void run()
		{
			int k = i.getAndIncrement();
			while ( !isInterrupted() && k < triangles.size() )
			{
				mapTriangleInverseInterpolated( transform, triangles.get( k ), source, target );
				k = i.getAndIncrement();
			}
		}
	}
	
	
	public TransformMeshMapping( final T t )
	{
		super( t );
	}
	
	/**
	 * 
	 * @param pm PointMatches
	 * @param min x = min[0], y = min[1]
	 * @param max x = max[0], y = max[1]
	 */
	final static protected void calculateBoundingBox(
			final ArrayList< PointMatch > pm,
			final float[] min,
			final float[] max )
	{
		final float[] first = pm.get( 0 ).getP2().getW();
		min[ 0 ] = first[ 0 ];
		min[ 1 ] = first[ 1 ];
		max[ 0 ] = first[ 0 ];
		max[ 1 ] = first[ 1 ];
		
		for ( final PointMatch p : pm )
		{
			final float[] t = p.getP2().getW();
			if ( t[ 0 ] < min[ 0 ] ) min[ 0 ] = t[ 0 ];
			else if ( t[ 0 ] > max[ 0 ] ) max[ 0 ] = t[ 0 ];
			if ( t[ 1 ] < min[ 1 ] ) min[ 1 ] = t[ 1 ];
			else if ( t[ 1 ] > max[ 1 ] ) max[ 1 ] = t[ 1 ];
		}
	}
	
	
	/**
	 * Checks if a location is inside a given triangle.
	 * 
	 * @param pm
	 * @param t
	 * @return
	 */
	final static protected boolean isInTriangle(
			final float ax,
			final float ay,
			final float bx,
			final float by,
			final float cx,
			final float cy,
			final float tx,
			final float ty )
	{
		final boolean d;
		{
			final float x1 = bx - ax;
			final float y1 = by - ay;
			final float x2 = tx - ax;
			final float y2 = ty - ay;
			d = x1 * y2 - y1 * x2 < 0;
		}
		{
			final float x1 = cx - bx;
			final float y1 = cy - by;
			final float x2 = tx - bx;
			final float y2 = ty - by;
			if ( d ^ x1 * y2 - y1 * x2 < 0 ) return false;
		}
		{
			final float x1 = ax - cx;
			final float y1 = ay - cy;
			final float x2 = tx - cx;
			final float y2 = ty - cy;
			if ( d ^ x1 * y2 - y1 * x2 < 0 ) return false;
		}
		return true;
	}
	
	
	final static protected void mapTriangle(
			final TransformMesh m, 
			final AffineModel2D ai,
			final ImageProcessor source,
			final ImageProcessor target )
	{
		final ArrayList< PointMatch > pm = m.getAV().get( ai );
		final float[] min = new float[ 2 ];
		final float[] max = new float[ 2 ];
		calculateBoundingBox( pm, min, max );
		
		final int maxX = ( int )max[ 0 ];
		final int maxY = ( int )max[ 1 ];
		
		final float[] a = pm.get( 0 ).getP2().getW();
		final float ax = a[ 0 ];
		final float ay = a[ 1 ];
		final float[] b = pm.get( 1 ).getP2().getW();
		final float bx = b[ 0 ];
		final float by = b[ 1 ];
		final float[] c = pm.get( 2 ).getP2().getW();
		final float cx = c[ 0 ];
		final float cy = c[ 1 ];
		final float[] t = new float[ 2 ];
		for ( int y = ( int )min[ 1 ]; y <= maxY; ++y )
		{
			for ( int x = ( int )min[ 0 ]; x <= maxX; ++x )
			{
				if ( isInTriangle( ax, ay, bx, by, cx, cy, x, y ) )
				{
					t[ 0 ] = x;
					t[ 1 ] = y;
					try
					{
						ai.applyInverseInPlace( t );
					}
					catch ( Exception e )
					{
						//e.printStackTrace( System.err );
						continue;
					}
					target.putPixel( x, y, source.getPixel( ( int )( t[ 0 ] + 0.5f ), ( int )( t[ 1 ] + 0.5f ) ) );
				}
			}
		}
	}
	
	final static protected void mapTriangleInterpolated(
			final TransformMesh m, 
			final AffineModel2D ai,
			final ImageProcessor source,
			final ImageProcessor target )
	{
		final ArrayList< PointMatch > pm = m.getAV().get( ai );
		final float[] min = new float[ 2 ];
		final float[] max = new float[ 2 ];
		calculateBoundingBox( pm, min, max );
		
		final int maxX = ( int )max[ 0 ];
		final int maxY = ( int )max[ 1 ];
		
		final float[] a = pm.get( 0 ).getP2().getW();
		final float ax = a[ 0 ];
		final float ay = a[ 1 ];
		final float[] b = pm.get( 1 ).getP2().getW();
		final float bx = b[ 0 ];
		final float by = b[ 1 ];
		final float[] c = pm.get( 2 ).getP2().getW();
		final float cx = c[ 0 ];
		final float cy = c[ 1 ];
		final float[] t = new float[ 2 ];
		for ( int y = ( int )min[ 1 ]; y <= maxY; ++y )
		{
			for ( int x = ( int )min[ 0 ]; x <= maxX; ++x )
			{
				if ( isInTriangle( ax, ay, bx, by, cx, cy, x, y ) )
				{
					t[ 0 ] = x;
					t[ 1 ] = y;
					try
					{
						ai.applyInverseInPlace( t );
					}
					catch ( Exception e )
					{
						//e.printStackTrace( System.err );
						continue;
					}
					target.putPixel( x, y, source.getPixelInterpolated( t[ 0 ], t[ 1 ] ) );
				}
			}
		}
	}
	
	final public void map(
			final ImageProcessor source,
			final ImageProcessor target,
			final int numThreads )
	{
		if ( numThreads == 1 )
		{
			/* no overhead for thread creation */
			final Set< AffineModel2D > s = transform.getAV().keySet();
			for ( final AffineModel2D ai : s )
				mapTriangle( transform, ai, source, target );
		}
		else
		{
			final List< AffineModel2D > l = new ArrayList< AffineModel2D >();
			l.addAll( transform.getAV().keySet() );
			final AtomicInteger i = new AtomicInteger( 0 );
			final ArrayList< Thread > threads = new ArrayList< Thread >( numThreads );
			for ( int k = 0; k < numThreads; ++k )
			{
				final Thread mtt = new MapTriangleThread( i, l, transform, source, target );
				threads.add( mtt );
				mtt.start();
			}
			for ( final Thread mtt : threads )
			{
				try
				{
					mtt.join();
				}
				catch ( InterruptedException e ) {}
			}
		}
	}
	
	@Override
	final public void map(
			final ImageProcessor source,
			final ImageProcessor target )
	{
		map( source, target, Runtime.getRuntime().availableProcessors() );
	}
	
	final public void mapInterpolated(
			final ImageProcessor source,
			final ImageProcessor target,
			final int numThreads )
	{
		if ( numThreads == 1 )
		{
			/* no overhead for thread creation */
			final Set< AffineModel2D > s = transform.getAV().keySet();
			for ( final AffineModel2D ai : s )
				mapTriangleInterpolated( transform, ai, source, target );
		}
		else
		{
			final List< AffineModel2D > l = new ArrayList< AffineModel2D >();
			l.addAll( transform.getAV().keySet() );
			final AtomicInteger i = new AtomicInteger( 0 );
			final ArrayList< Thread > threads = new ArrayList< Thread >( numThreads );
			for ( int k = 0; k < numThreads; ++k )
			{
				final Thread mtt = new MapTriangleInterpolatedThread( i, l, transform, source, target );
				threads.add( mtt );
				mtt.start();
			}
			for ( final Thread mtt : threads )
			{
				try
				{
					mtt.join();
				}
				catch ( InterruptedException e ) {}
			}
		}
	}
	
	@Override
	final public void mapInterpolated(
			final ImageProcessor source,
			final ImageProcessor target )
	{
		mapInterpolated( source, target, Runtime.getRuntime().availableProcessors() );
	}
	
	
	/**
	 * 
	 * @param pm PointMatches
	 * @param min x = min[0], y = min[1]
	 * @param max x = max[0], y = max[1]
	 */
	final static protected void calculateBoundingBoxInverse(
			final ArrayList< PointMatch > pm,
			final float[] min,
			final float[] max )
	{
		final float[] first = pm.get( 0 ).getP1().getL();
		min[ 0 ] = first[ 0 ];
		min[ 1 ] = first[ 1 ];
		max[ 0 ] = first[ 0 ];
		max[ 1 ] = first[ 1 ];
		
		for ( final PointMatch p : pm )
		{
			final float[] t = p.getP1().getL();
			if ( t[ 0 ] < min[ 0 ] ) min[ 0 ] = t[ 0 ];
			else if ( t[ 0 ] > max[ 0 ] ) max[ 0 ] = t[ 0 ];
			if ( t[ 1 ] < min[ 1 ] ) min[ 1 ] = t[ 1 ];
			else if ( t[ 1 ] > max[ 1 ] ) max[ 1 ] = t[ 1 ];
		}
	}
	
	final static protected void mapTriangleInverse(
			final TransformMesh m, 
			final AffineModel2D ai,
			final ImageProcessor source,
			final ImageProcessor target )
	{
		final ArrayList< PointMatch > pm = m.getAV().get( ai );
		final float[] min = new float[ 2 ];
		final float[] max = new float[ 2 ];
		calculateBoundingBoxInverse( pm, min, max );
		
		final int maxX = Util.round( max[ 0 ] );
		final int maxY = Util.round( max[ 1 ] );
		
		final float[] a = pm.get( 0 ).getP1().getL();
		final float ax = a[ 0 ];
		final float ay = a[ 1 ];
		final float[] b = pm.get( 1 ).getP1().getL();
		final float bx = b[ 0 ];
		final float by = b[ 1 ];
		final float[] c = pm.get( 2 ).getP1().getL();
		final float cx = c[ 0 ];
		final float cy = c[ 1 ];
		final float[] t = new float[ 2 ];
		for ( int y = Util.round( min[ 1 ] ); y <= maxY; ++y )
		{
			for ( int x = Util.round( min[ 0 ] ); x <= maxX; ++x )
			{
				if ( isInTriangle( ax, ay, bx, by, cx, cy, x, y ) )
				{
					t[ 0 ] = x;
					t[ 1 ] = y;
					ai.applyInPlace( t );
					target.putPixel( x, y, source.getPixel( ( int )( t[ 0 ] + 0.5f ), ( int )( t[ 1 ] + 0.5f ) ) );
				}
			}
		}
	}
	
	final static protected void mapTriangleInverseInterpolated(
			final TransformMesh m, 
			final AffineModel2D ai,
			final ImageProcessor source,
			final ImageProcessor target )
	{
		final ArrayList< PointMatch > pm = m.getAV().get( ai );
		final float[] min = new float[ 2 ];
		final float[] max = new float[ 2 ];
		calculateBoundingBoxInverse( pm, min, max );
		
		final int maxX = Util.round( max[ 0 ] );
		final int maxY = Util.round( max[ 1 ] );
		
		final float[] a = pm.get( 0 ).getP1().getL();
		final float ax = a[ 0 ];
		final float ay = a[ 1 ];
		final float[] b = pm.get( 1 ).getP1().getL();
		final float bx = b[ 0 ];
		final float by = b[ 1 ];
		final float[] c = pm.get( 2 ).getP1().getL();
		final float cx = c[ 0 ];
		final float cy = c[ 1 ];
		final float[] t = new float[ 2 ];
		for ( int y = Util.round( min[ 1 ] ); y <= maxY; ++y )
		{
			for ( int x = Util.round( min[ 0 ] ); x <= maxX; ++x )
			{
				if ( isInTriangle( ax, ay, bx, by, cx, cy, x, y ) )
				{
					t[ 0 ] = x;
					t[ 1 ] = y;
					ai.applyInPlace( t );
					target.putPixel( x, y, source.getPixelInterpolated( t[ 0 ], t[ 1 ] ) );
				}
			}
		}
	}
	
	final public void mapInverse(
			final ImageProcessor source,
			final ImageProcessor target,
			final int numThreads )
	{
		if ( numThreads == 1 )
		{
			/* no overhead for thread creation */
			final Set< AffineModel2D > s = transform.getAV().keySet();
			for ( final AffineModel2D ai : s )
				mapTriangleInverse( transform, ai, source, target );
		}
		else
		{
			final List< AffineModel2D > l = new ArrayList< AffineModel2D >();
			l.addAll( transform.getAV().keySet() );
			final AtomicInteger i = new AtomicInteger( 0 );
			final ArrayList< Thread > threads = new ArrayList< Thread >( numThreads );
			for ( int k = 0; k < numThreads; ++k )
			{
				final Thread mtt = new MapTriangleInverseThread( i, l, transform, source, target );
				threads.add( mtt );
				mtt.start();
			}
			for ( final Thread mtt : threads )
			{
				try
				{
					mtt.join();
				}
				catch ( InterruptedException e ) {}
			}
		}
	}
	
	@Override
	final public void mapInverse(
			final ImageProcessor source,
			final ImageProcessor target )
	{
		mapInverse( source, target, Runtime.getRuntime().availableProcessors() );
	}
	
	final public void mapInverseInterpolated(
			final ImageProcessor source,
			final ImageProcessor target,
			final int numThreads )
	{
		if ( numThreads == 1 )
		{
			/* no overhead for thread creation */
			final Set< AffineModel2D > s = transform.getAV().keySet();
			for ( final AffineModel2D ai : s )
				mapTriangleInverseInterpolated( transform, ai, source, target );
		}
		else
		{
			final List< AffineModel2D > l = new ArrayList< AffineModel2D >();
			l.addAll( transform.getAV().keySet() );
			final AtomicInteger i = new AtomicInteger( 0 );
			final ArrayList< Thread > threads = new ArrayList< Thread >( numThreads );
			for ( int k = 0; k < numThreads; ++k )
			{
				final Thread mtt = new MapTriangleInverseInterpolatedThread( i, l, transform, source, target );
				threads.add( mtt );
				mtt.start();
			}
			for ( final Thread mtt : threads )
			{
				try
				{
					mtt.join();
				}
				catch ( InterruptedException e ) {}
			}
		}
	}
	
	@Override
	final public void mapInverseInterpolated(
			final ImageProcessor source,
			final ImageProcessor target )
	{
		mapInverseInterpolated( source, target, Runtime.getRuntime().availableProcessors() );
	}
	
}
