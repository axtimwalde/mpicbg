import ij.IJ;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashMap;

import mpicbg.models.AffineModel2D;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import mpicbg.models.NotEnoughDataPointsException;


public class TransformMesh
{
	protected float width, height;
	public float getWidth(){ return width; }
	public float getHeight(){ return height; }
	
	final HashMap< AffineModel2D, ArrayList< PointMatch > > a = new HashMap< AffineModel2D, ArrayList< PointMatch > >();
	final HashMap< PointMatch, ArrayList< AffineModel2D > > l = new HashMap< PointMatch, ArrayList< AffineModel2D > >();
	
	public TransformMesh( int numX, int numY, float width, float height )
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
	
	/**
	 * Add a triangle defined by 3 PointMatches that defines an
	 * AffineTransform2D.
	 * 
	 * @param t 3 PointMatches (will not be copied, so do not reuse this list!)
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
		a.put( m, t );
		
		for ( PointMatch pm : t )
		{
			if ( !l.containsKey( pm ) )
				l.put( pm, new ArrayList< AffineModel2D >() );
			l.get( pm ).add( m );
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
	
	protected void apply( AffineModel2D ai, ImageProcessor src, ImageProcessor trg )
	{
		ArrayList< PointMatch > pm = a.get( ai );
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
	
	public void apply( ImageProcessor src, ImageProcessor trg )
	{
		trg.setColor( Color.black );
		trg.fill();
		Set< AffineModel2D > s = a.keySet();
		for ( AffineModel2D ai : s )
			apply( ai, src, trg );
	}
	
	private void illustrateTriangle( AffineModel2D ai, GeneralPath path )
	{
		ArrayList< PointMatch > m = a.get( ai );
		
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
		
		Set< AffineModel2D > s = a.keySet();
		for ( AffineModel2D ai : s )
			illustrateTriangle( ai, path );
		
		return path;
	}
	
	public void updateAffine( PointMatch p )
	{
		for ( AffineModel2D ai : l.get( p ) )
		{
			try
			{
				ai.fit( a.get( ai ) );
			}
			catch ( NotEnoughDataPointsException e )
			{
				IJ.error( e.getMessage() );
				e.printStackTrace( System.err );
			}
		}
	}
	
	public void updateAffines()
	{
		Set< AffineModel2D > s = a.keySet();
		for ( AffineModel2D ai : s )
		{
			try
			{
				ai.fit( a.get( ai ) );
			}
			catch ( NotEnoughDataPointsException e )
			{
				IJ.error( e.getMessage() );
				e.printStackTrace( System.err );
			}
		}
	}
}
