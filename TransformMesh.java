/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 *
 */
import ij.IJ;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashMap;

import mpicbg.models.AffineModel2D;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.IllDefinedDataPointsException;

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
 * Each vertex is given as a PointMatch with p1 being the original point and p2
 * being the transferred point.  Keep in mind that Points store local and world
 * coordinates with local coordinates being constant and world coordinates
 * being mutable.  That is typically p1.l = p1.w = p2.l while p2.w is the
 * transferred location of the vertex.
 * 
 * Three adjacent vertices span a triangle.  All pixels inside a triangle will
 * be transferred by a 2d affine transform that is defined by the three
 * vertices.  Given the abovementioned definition of a vertex as PointMatch,
 * this 2d affine transform is a forward transform (p1.l->p2.w). 
 * 
 * @version 0.2b
 */
public class TransformMesh
{
	final protected float width, height;
	public float getWidth(){ return width; }
	public float getHeight(){ return height; }
	
	final protected HashMap< AffineModel2D, ArrayList< PointMatch > > av = new HashMap< AffineModel2D, ArrayList< PointMatch > >();
	final protected HashMap< PointMatch, ArrayList< AffineModel2D > > va = new HashMap< PointMatch, ArrayList< AffineModel2D > >();
	
	public TransformMesh(
			final int numX,
			final int numY,
			final float width,
			final float height )
	{
		float w = width * height / numX / numY;
		PointMatch[] pq = new PointMatch[ numX * numY + ( numX - 1 ) * ( numY - 1 ) ];
		
		this.width = width;
		this.height = height;
		
		float dy = height / ( numY - 1 );
		float dx = width / ( numX - 1 );
		
		int i = 0;
		for ( int xi = 0; xi < numX; ++xi )
		{
			float xip = xi * dx;
			Point p = new Point( new float[]{ xip, 0 } );
			pq[ i ] = new PointMatch( p, p.clone() );
			
			++i;
		}
		
		Point p;
		int i1, i2, i3;
		ArrayList< PointMatch > t1, t2;
		
		for ( int yi = 1; yi < numY; ++yi )
		{
			// odd row
			float yip = yi * dy - dy / 2;

			p  = new Point( new float[]{ dx - dx / 2, yip } );
			pq[ i ] = new PointMatch( p, p.clone(), w );
			
			i1 = i - numX;
			i2 = i1 + 1;
			
			t1 = new ArrayList< PointMatch >();
			t1.add( pq[ i1 ] );
			t1.add( pq[ i2 ] );
			t1.add( pq[ i ] );
			
			addTriangle( t1 );
			
			++i;
			
			for ( int xi = 2; xi < numX; ++xi )
			{
				float xip = xi * dx - dx / 2;
				
				p  = new Point( new float[]{ xip, yip } );
				pq[ i ] = new PointMatch( p, p.clone(), w );
				
				i1 = i - numX;
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
			
			i1 = i - numX + 1;
			i2 = i1 - numX;
			
			t1 = new ArrayList< PointMatch >();
			t1.add( pq[ i2 ] );
			t1.add( pq[ i1 ] );
			t1.add( pq[ i ] );
			
			addTriangle( t1 );
			
			++i;
			
			for ( int xi = 1; xi < numX - 1; ++xi )
			{
				float xip = xi * dx;
								
				p = new Point( new float[]{ xip, yip } );
				pq[ i ] = new PointMatch( p, p.clone(), w );
				
				i1 = i - numX;
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
			
			i1 = i - numX;
			i2 = i1 - numX + 1;
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
		final float dx = width / ( float )( numX - 1 );
		final float dy = 2.0f * ( float )Math.sqrt(4.0f / 5.0f * dx * dx );
		return ( int )Math.round( height / dy ) + 1;
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
		catch ( NotEnoughDataPointsException e )
		{
			IJ.error( e.getMessage() );
			e.printStackTrace( System.err );
		}
		catch ( IllDefinedDataPointsException e )
		{
			IJ.error( e.getMessage() );
			e.printStackTrace( System.err );
		}
		av.put( m, t );
		
		for ( PointMatch pm : t )
		{
			if ( !va.containsKey( pm ) )
				va.put( pm, new ArrayList< AffineModel2D >() );
			va.get( pm ).add( m );
		}
	}	
	
	/**
	 * 
	 * @param pm PointMatches
	 * @return bounding box with
	 *   min(x,y) = box[0][0],box[0][1] and 
	 *   max(x,y) = box[1][0],box[1][1]
	 */
	protected float[][] getBoundingBox( ArrayList< PointMatch > pm )
	{
		float[] first = pm.get( 0 ).getP1().getW();
		float[][] box = new float[ 2 ][];
		box[ 0 ] = first.clone();
		box[ 1 ] = first.clone();
		
		for ( PointMatch p : pm )
		{
			float[] t = p.getP2().getW();
			if ( t[ 0 ] < box[ 0 ][ 0 ] ) box[ 0 ][ 0 ] = t[ 0 ];
			else if ( t[ 0 ] > box[ 1 ][ 0 ] ) box[ 1 ][ 0 ] = t[ 0 ];
			if ( t[ 1 ] < box[ 0 ][ 1 ] ) box[ 0 ][ 1 ] = t[ 1 ];
			else if ( t[ 1 ] > box[ 1 ][ 1 ] ) box[ 1 ][ 1 ] = t[ 1 ];
		}
		
		return box;
	}
	
	protected void paint( AffineModel2D ai, ImageProcessor src, ImageProcessor trg )
	{
		ArrayList< PointMatch > pm = av.get( ai );
		float[][] box = getBoundingBox( pm );
		for ( int y = ( int )box[ 0 ][ 1 ]; y <= ( int )box[ 1 ][ 1 ]; ++y )
		{
X:			for ( int x = ( int )box[ 0 ][ 0 ]; x <= ( int )box[ 1 ][ 0 ]; ++x )
			{
				for ( int i = 0; i < pm.size(); ++i )
				{
					PointMatch r1 = pm.get( i );
					PointMatch r2 = pm.get( ( i + 1 ) % pm.size() );
					float[] t1 = r1.getP2().getW();
					float[] t2 = r2.getP2().getW();
					
					float x1 = t2[ 0 ] - t1[ 0 ];
					float y1 = t2[ 1 ] - t1[ 1 ];
					float x2 = ( float )x - t1[ 0 ];
					float y2 = ( float )y - t1[ 1 ];
					
					if ( x1 * y2 - y1 * x2 < 0 ) continue X;
				}
				float[] t = new float[]{ x, y };
				try
				{
					ai.applyInverseInPlace( t );
				}
				catch ( Exception e )
				{
					e.printStackTrace( System.err );
				}
				trg.putPixel( x, y, src.getPixel( ( int )t[ 0 ], ( int )t[ 1 ] ) );
			}
		}
	}
	
	public void paint( ImageProcessor src, ImageProcessor trg )
	{
		trg.setColor( Color.black );
		trg.fill();
		Set< AffineModel2D > s = av.keySet();
		for ( AffineModel2D ai : s )
			paint( ai, src, trg );
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
	
	public void updateAffine( PointMatch p )
	{
		for ( AffineModel2D ai : va.get( p ) )
		{
			try
			{
				ai.fit( av.get( ai ) );
			}
			catch ( NotEnoughDataPointsException e )
			{
				IJ.error( e.getMessage() );
				e.printStackTrace( System.err );
			}
			catch ( IllDefinedDataPointsException e )
			{
				IJ.error( e.getMessage() );
				e.printStackTrace( System.err );
			}
		}
	}
	
	public void updateAffines()
	{
		Set< AffineModel2D > s = av.keySet();
		for ( AffineModel2D ai : s )
		{
			try
			{
				ai.fit( av.get( ai ) );
			}
			catch ( NotEnoughDataPointsException e )
			{
				IJ.error( e.getMessage() );
				e.printStackTrace( System.err );
			}
			catch ( IllDefinedDataPointsException e )
			{
				IJ.error( e.getMessage() );
				e.printStackTrace( System.err );
			}
		}
	}
}
