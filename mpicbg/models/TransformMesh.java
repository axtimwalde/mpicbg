package mpicbg.models;

import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashMap;

/**
 * Trianguar transformation mesh.
 * 
 * See an example to find out how the mesh is constructed:
 * 
 * numX = 4; numY = 3:
 * <pre>
 * *---*---*---*
 * |\ / \ / \ /|
 * | *---*---* |
 * |/ \ / \ / \|
 * *---*---*---* 
 * |\ / \ / \ /|
 * | *---*---* |
 * |/ \ / \ / \|
 * *---*---*---* 
 * </pre>
 * 
 * Each vertex is given as a {@link PointMatch} with
 * {@link PointMatch#getP1() p1} being the original point and
 * {@link PointMatch#getP2() p2} being the transferred point.  Keep in mind
 * that Points store local and world coordinates with local coordinates being
 * constant and world coordinates being mutable.  That is initially
 * {@link Point#getL() p1.l} = {@link Point#getW() p1.w} =
 * {@link Point#getL() p2.l} while {@link Point#getW() p1.w} is the transferred
 * location of the vertex.
 * 
 * Three adjacent vertices span a triangle.  All pixels inside a triangle will
 * be transferred by a {@link AffineModel2D 2d affine transform} that is
 * defined by the three vertices.  Given the abovementioned definition of a
 * vertex as PointMatch, this {@link AffineModel2D 2d affine transform} is a
 * forward transform (p1.l->p2.w). 
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.2b
 */
public class TransformMesh implements InvertibleCoordinateTransform
{
	final protected float width, height;
	public float getWidth(){ return width; }
	public float getHeight(){ return height; }
	
	final protected HashMap< AffineModel2D, ArrayList< PointMatch > > av = new HashMap< AffineModel2D, ArrayList< PointMatch > >();
	final public HashMap< AffineModel2D, ArrayList< PointMatch > > getAV(){ return av; }
	final protected HashMap< PointMatch, ArrayList< AffineModel2D > > va = new HashMap< PointMatch, ArrayList< AffineModel2D > >();
	final public HashMap< PointMatch, ArrayList< AffineModel2D > > getVA(){ return va; };
	
	public TransformMesh(
			final int numX,
			final int numY,
			final float width,
			final float height )
	{
		final int numXs = Math.max( 2, numX );
		final int numYs = Math.max( 2, numY );
		
		float w = width * height / numXs / numYs;
		PointMatch[] pq = new PointMatch[ numXs * numYs + ( numXs - 1 ) * ( numYs - 1 ) ];
		
		this.width = width;
		this.height = height;
		
		float dy = height / ( numYs - 1 );
		float dx = width / ( numXs - 1 );
		
		int i = 0;
		for ( int xi = 0; xi < numXs; ++xi )
		{
			float xip = xi * dx;
			Point p = new Point( new float[]{ xip, 0 } );
			pq[ i ] = new PointMatch( p, p.clone() );
			
			++i;
		}
		
		Point p;
		int i1, i2, i3;
		ArrayList< PointMatch > t1, t2;
		
		for ( int yi = 1; yi < numYs; ++yi )
		{
			// odd row
			float yip = yi * dy - dy / 2;

			p  = new Point( new float[]{ dx - dx / 2, yip } );
			pq[ i ] = new PointMatch( p, p.clone(), w );
			
			i1 = i - numXs;
			i2 = i1 + 1;
			
			t1 = new ArrayList< PointMatch >();
			t1.add( pq[ i1 ] );
			t1.add( pq[ i2 ] );
			t1.add( pq[ i ] );
			
			addTriangle( t1 );
			
			++i;
			
			for ( int xi = 2; xi < numXs; ++xi )
			{
				float xip = xi * dx - dx / 2;
				
				p  = new Point( new float[]{ xip, yip } );
				pq[ i ] = new PointMatch( p, p.clone(), w );
				
				i1 = i - numXs;
				i2 = i1 + 1;
				i3 = i - 1;
				
				t1 = new ArrayList< PointMatch >();
				t1.add( pq[ i1 ] );
				t1.add( pq[ i2 ] );
				t1.add( pq[ i ] );
				
				addTriangle( t1 );
				
				t2 = new ArrayList< PointMatch >();
				t2.add( pq[ i1 ] );
				t2.add( pq[ i ] );
				t2.add( pq[ i3 ] );
				
				addTriangle( t2 );
				
				++i;
			}
			
			// even row
			yip = yi * dy;
			p  = new Point( new float[]{ 0, yip } );
			pq[ i ] = new PointMatch( p, p.clone(), w );
			
			i1 = i - numXs + 1;
			i2 = i1 - numXs;
			
			t1 = new ArrayList< PointMatch >();
			t1.add( pq[ i2 ] );
			t1.add( pq[ i1 ] );
			t1.add( pq[ i ] );
			
			addTriangle( t1 );
			
			++i;
			
			for ( int xi = 1; xi < numXs - 1; ++xi )
			{
				float xip = xi * dx;
								
				p = new Point( new float[]{ xip, yip } );
				pq[ i ] = new PointMatch( p, p.clone(), w );
				
				i1 = i - numXs;
				i2 = i1 + 1;
				i3 = i - 1;
				
				t1 = new ArrayList< PointMatch >();
				t1.add( pq[ i1 ] );
				t1.add( pq[ i ] );
				t1.add( pq[ i3 ] );
				
				addTriangle( t1 );
				
				t2 = new ArrayList< PointMatch >();
				t2.add( pq[ i1 ] );
				t2.add( pq[ i2 ] );
				t2.add( pq[ i ] );
				
				addTriangle( t2 );
				
				++i;
			}
			
			p  = new Point( new float[]{ width, yip } );
			pq[ i ] = new PointMatch( p, p.clone(), w );
			
			i1 = i - numXs;
			i2 = i1 - numXs + 1;
			i3 = i - 1;
			
			t1 = new ArrayList< PointMatch >();
			t1.add( pq[ i3 ] );
			t1.add( pq[ i1 ] );
			t1.add( pq[ i ] );
			
			addTriangle( t1 );
			
			t2 = new ArrayList< PointMatch >();
			t2.add( pq[ i1 ] );
			t2.add( pq[ i2 ] );
			t2.add( pq[ i ] );
			
			addTriangle( t2 );
			
			++i;
		}
	}
	
	final static protected int numY(
			final int numX,
			final float width,
			final float height )
	{
		final int numXs = Math.max( 2, numX );
		final float dx = width / ( float )( numXs - 1 );
		final float dy = 2.0f * ( float )Math.sqrt(4.0f / 5.0f * dx * dx );
		return ( int )Math.max( 2, Math.round( height / dy ) + 1 );
	}
	
	public TransformMesh(
			final int numX,
			final float width,
			final float height )
	{
		this( numX, numY( numX, width, height ), width, height );
	}
	
	/**
	 * Add a triangle defined by 3 PointMatches that defines an
	 * AffineTransform2D.
	 * 
	 * @param t
	 *            3 PointMatches (will not be copied, so do not reuse this
	 *            list!)
	 */
	public void addTriangle( ArrayList< PointMatch > t )
	{
		AffineModel2D m = new AffineModel2D();
		try
		{
			m.fit( t );
		}
		catch ( NotEnoughDataPointsException e ) { e.printStackTrace(); }
		catch ( IllDefinedDataPointsException e ) { e.printStackTrace(); }
		av.put( m, t );
		
		for ( PointMatch pm : t )
		{
			if ( !va.containsKey( pm ) )
				va.put( pm, new ArrayList< AffineModel2D >() );
			va.get( pm ).add( m );
		}
	}	
	
	private void illustrateTriangle( AffineModel2D ai, GeneralPath path )
	{
		ArrayList< PointMatch > m = av.get( ai );
		
		float[] w = m.get( 0 ).getP2().getW();
		path.moveTo( w[ 0 ], w[ 1 ] );
		
		for ( int i = 1; i < m.size(); ++i )
		{
			w = m.get( i ).getP2().getW();
			path.lineTo( w[ 0 ], w[ 1 ] );
		}
		path.closePath();
	}
	
	/**
	 * Create a Shape that illustrates the mesh.
	 * 
	 * @return the illustration
	 */
	public Shape illustrateMesh()
	{
		GeneralPath path = new GeneralPath();
		
		Set< AffineModel2D > s = av.keySet();
		for ( AffineModel2D ai : s )
			illustrateTriangle( ai, path );
		
		return path;
	}
	
	private String illustrateTriangleSVG( AffineModel2D ai )
	{
		String svg = "";
		
		ArrayList< PointMatch > m = av.get( ai );
		
		float[] w = m.get( 0 ).getP2().getW();
		svg += "M " + w[ 0 ] + " " + w[ 1 ] + " ";
		
		for ( int i = 1; i < m.size(); ++i )
		{
			w = m.get( i ).getP2().getW();
			svg += "L " + w[ 0 ] + " "  + w[ 1 ] + " ";
		}
		
		svg += "Z ";
		
		return svg;
	}
	
	/**
	 * Create an SVG path that illustrates the mesh.
	 * 
	 * @return svg path-definition
	 */
	public String illustrateMeshSVG()
	{
		String svg = "<path style=\"fill:none;fill-rule:evenodd;stroke:#000000;stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;stroke-opacity:1\" d=\"";
		Set< AffineModel2D > s = av.keySet();
		for ( AffineModel2D ai : s )
			svg += illustrateTriangleSVG( ai );
		
		svg += "\" />";
		
		return svg;
	}
	
	/**
	 * Create an SVG path that illustrates the best regid approximation of the
	 * mesh as a rotated square.
	 * 
	 * @return svg path-definition
	 */
	public String illustrateBestRigidSVG()
	{
		String svg = "<path style=\"fill:none;fill-rule:evenodd;stroke:#000000;stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;stroke-opacity:1\" d=\"";
		Set< PointMatch > s = va.keySet();
		RigidModel2D m = new RigidModel2D();
		try
		{
			m.fit( s );
		}
		catch ( NotEnoughDataPointsException ex )
		{
			ex.printStackTrace();
			return "";
		}
		
		float[] l = new float[]{ 0, 0 };
		m.applyInPlace( l );
		svg += "M " + l[ 0 ] + " " + l[ 1 ] + " ";
		
		l[ 0 ] = width;
		l[ 1 ] = 0;
		m.applyInPlace( l );
		svg += "L " + l[ 0 ] + " " + l[ 1 ] + " ";
		
		l[ 0 ] = width;
		l[ 1 ] = height;
		m.applyInPlace( l );
		svg += "L " + l[ 0 ] + " " + l[ 1 ] + " ";
		
		l[ 0 ] = 0;
		l[ 1 ] = height;
		m.applyInPlace( l );
		svg += "L " + l[ 0 ] + " " + l[ 1 ] + " ";
		
		svg += "Z";
		
		svg += "\" />";
		
		return svg;
	}
	
	/**
	 * Update all affine transformations that would have been affected by a
	 * given {@link PointMatch Vertex}.
	 * 
	 * @param p
	 */
	public void updateAffine( PointMatch p )
	{
		for ( AffineModel2D ai : va.get( p ) )
		{
			try
			{
				ai.fit( av.get( ai ) );
			}
			catch ( NotEnoughDataPointsException e ) { e.printStackTrace(); }
			catch ( IllDefinedDataPointsException e ) { e.printStackTrace(); }
		}
	}
	
	/**
	 * Update all affine transformations.
	 *
	 */
	public void updateAffines()
	{
		Set< AffineModel2D > s = av.keySet();
		for ( AffineModel2D ai : s )
		{
			try
			{
				ai.fit( av.get( ai ) );
			}
			catch ( NotEnoughDataPointsException e ) { e.printStackTrace(); }
			catch ( IllDefinedDataPointsException e ) { e.printStackTrace(); }
		}
	}
	
	/**
	 * Checks if a location is inside a given polygon at the target side or not.
	 * 
	 * @param pm
	 * @param t
	 * @return
	 */
	final static public boolean isInTargetPolygon(
			final ArrayList< PointMatch > pm,
			final float[] t )
	{
		assert t.length == 2 : "2d transform meshs can be applied to 2d points only.";
		
		for ( int i = 0; i < pm.size(); ++i )
		{
			final PointMatch r1 = pm.get( i );
			final PointMatch r2 = pm.get( ( i + 1 ) % pm.size() );
			final float[] t1 = r1.getP2().getW();
			final float[] t2 = r2.getP2().getW();
			
			final float x1 = t2[ 0 ] - t1[ 0 ];
			final float y1 = t2[ 1 ] - t1[ 1 ];
			final float x2 = ( float )t[ 0 ] - t1[ 0 ];
			final float y2 = ( float )t[ 1 ] - t1[ 1 ];
			
			if ( x1 * y2 - y1 * x2 < 0 ) return false;
		}
		return true;
	}
	
	
	/**
	 * Checks if a location is inside a given polygon at the source side or not.
	 * 
	 * @param pm
	 * @param t
	 * @return
	 */
	final static public boolean isInSourcePolygon(
			final ArrayList< PointMatch > pm,
			final float[] t )
	{
		assert t.length == 2 : "2d transform meshs can be applied to 2d points only.";
		
		for ( int i = 0; i < pm.size(); ++i )
		{
			final PointMatch r1 = pm.get( i );
			final PointMatch r2 = pm.get( ( i + 1 ) % pm.size() );
			final float[] t1 = r1.getP2().getL();
			final float[] t2 = r2.getP2().getL();
			
			final float x1 = t2[ 0 ] - t1[ 0 ];
			final float y1 = t2[ 1 ] - t1[ 1 ];
			final float x2 = ( float )t[ 0 ] - t1[ 0 ];
			final float y2 = ( float )t[ 1 ] - t1[ 1 ];
			
			if ( x1 * y2 - y1 * x2 < 0 ) return false;
		}
		return true;
	}
	
	//@Override
	public float[] apply( final float[] location )
	{
		assert location.length == 2 : "2d transform meshs can be applied to 2d points only.";
		
		final float[] transformed = location.clone();
		applyInPlace( transformed );
		return transformed;
	}

	//@Override
	public void applyInPlace( final float[] location )
	{
		assert location.length == 2 : "2d transform meshs can be applied to 2d points only.";
		
		Set< AffineModel2D > s = av.keySet();
		for ( AffineModel2D ai : s )
		{
			final ArrayList< PointMatch > pm = av.get( ai );
			if ( isInSourcePolygon( pm, location ) )
			{
				ai.applyInPlace( location );
				return;
			}
		} 
	}

	//@Override
	public float[] applyInverse( float[] location ) throws NoninvertibleModelException
	{
		assert location.length == 2 : "2d transform meshs can be applied to 2d points only.";
		
		final float[] transformed = location.clone();
		applyInverseInPlace( transformed );
		return transformed;
	}

	//@Override
	public void applyInverseInPlace( float[] location ) throws NoninvertibleModelException
	{
		assert location.length == 2 : "2d transform meshs can be applied to 2d points only.";
		
		Set< AffineModel2D > s = av.keySet();
		for ( AffineModel2D ai : s )
		{
			final ArrayList< PointMatch > pm = av.get( ai );
			if ( isInTargetPolygon( pm, location ) )
			{
				ai.applyInverseInPlace( location );
				return;
			}
		}
		throw new NoninvertibleModelException( "Noninvertible location ( " + location[ 0 ] + ", " + location[ 1 ] + " )" );
	}
}
