import ij.gui.*;

import mpicbg.ij.InteractiveInvertibleCoordinateTransform;
import mpicbg.models.*;

public class Transform_Perspective extends InteractiveInvertibleCoordinateTransform< HomographyModel2D >
{
	final protected HomographyModel2D model = new HomographyModel2D();
	
	@Override
	final protected HomographyModel2D myModel(){ return model; }
	
	@Override
	final protected void setHandles()
	{
		Roi currentRoi = imp.getRoi();
		int[] x;
		int[] y;
		if (currentRoi instanceof PolygonRoi && ((PolygonRoi)currentRoi).getNCoordinates() == 4 ) {
			x = currentRoi.getPolygon().xpoints;
			y = currentRoi.getPolygon().ypoints;
		} else {
			x = new int[]{ imp.getWidth() / 4, 3 * imp.getWidth() / 4, 3 * imp.getWidth() / 4, imp.getWidth() / 4 };
			y = new int[]{ imp.getHeight() / 4, imp.getHeight() / 4, 3 * imp.getHeight() / 4, 3 * imp.getHeight() / 4 };
		}

		p = new Point[]{
				new Point( new float[]{ ( float )x[ 0 ], ( float )y[ 0 ] } ),
				new Point( new float[]{ ( float )x[ 1 ], ( float )y[ 1 ] } ),
				new Point( new float[]{ ( float )x[ 2 ], ( float )y[ 2 ] } ),
				new Point( new float[]{ ( float )x[ 3 ], ( float )y[ 3 ] } ) };
		
		q = new Point[]{
				p[ 0 ].clone(),
				p[ 1 ].clone(),
				p[ 2 ].clone(),
				p[ 3 ].clone() };
		
		m.add( new PointMatch( p[ 0 ], q[ 0 ] ) );
		m.add( new PointMatch( p[ 1 ], q[ 1 ] ) );
		m.add( new PointMatch( p[ 2 ], q[ 2 ] ) );
		m.add( new PointMatch( p[ 3 ], q[ 3 ] ) );
		
		handles = new PointRoi( x, y, 4 );
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
				
		handles = new PointRoi( rx, ry, 4 );
		imp.setRoi( handles );
				
		fq[ 0 ] = x;
		fq[ 1 ] = y;
	}
}
