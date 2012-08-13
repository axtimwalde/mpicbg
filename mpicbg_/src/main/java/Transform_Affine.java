import ij.gui.*;

import mpicbg.ij.InteractiveInvertibleCoordinateTransform;
import mpicbg.models.*;

public class Transform_Affine extends InteractiveInvertibleCoordinateTransform< AffineModel2D >
{
	final protected AffineModel2D model = new AffineModel2D();
	
	@Override
	final protected AffineModel2D myModel() { return model; }
	
	@Override
	final protected void setHandles()
	{
		int[] x = new int[]{ imp.getWidth() / 4, 3 * imp.getWidth() / 4, imp.getWidth() / 4 };
		int[] y = new int[]{ imp.getHeight() / 4, imp.getHeight() / 2, 3 * imp.getHeight() / 4 };
		
		p = new Point[]{
				new Point( new float[]{ ( float )x[ 0 ], ( float )y[ 0 ] } ),
				new Point( new float[]{ ( float )x[ 1 ], ( float )y[ 1 ] } ),
				new Point( new float[]{ ( float )x[ 2 ], ( float )y[ 2 ] } ) };
		
		q = new Point[]{
				p[ 0 ].clone(),
				p[ 1 ].clone(),
				p[ 2 ].clone() };
		
		m.add( new PointMatch( p[ 0 ], q[ 0 ] ) );
		m.add( new PointMatch( p[ 1 ], q[ 1 ] ) );
		m.add( new PointMatch( p[ 2 ], q[ 2 ] ) );
		
		handles = new PointRoi( x, y, 3 );
		imp.setRoi( handles );
	}
	
	@Override
	final protected void updateHandles( int x, int y )
	{
		float[] fq = q[ targetIndex ].getW();
			
		int[] rx = new int[ q.length ];
		int[] ry = new int[ q.length ];
		
		for ( int i = 0; i < q.length; ++i )
		{
			rx[ i ] = ( int )q[ i ].getW()[ 0 ];
			ry[ i ] = ( int )q[ i ].getW()[ 1 ];
		}
			
		rx[ targetIndex ] = x;
		ry[ targetIndex ] = y;
			
		handles = new PointRoi( rx, ry, 3 );
		imp.setRoi( handles );
			
		fq[ 0 ] = x;
		fq[ 1 ] = y;
	}
}
