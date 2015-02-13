import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import mpicbg.ij.InteractiveInvertibleCoordinateTransform;
import mpicbg.models.HomographyModel2D;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;

public class Transform_Perspective extends InteractiveInvertibleCoordinateTransform< HomographyModel2D >
{
	final protected HomographyModel2D model = new HomographyModel2D();

	@Override
	final protected HomographyModel2D myModel(){ return model; }

	@Override
	final protected void setHandles()
	{
		final Roi currentRoi = imp.getRoi();
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
				new Point( new double[]{ x[ 0 ], y[ 0 ] } ),
				new Point( new double[]{ x[ 1 ], y[ 1 ] } ),
				new Point( new double[]{ x[ 2 ], y[ 2 ] } ),
				new Point( new double[]{ x[ 3 ], y[ 3 ] } ) };

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
	final protected void updateHandles( final int x, final int y )
	{
		final double[] fq = q[ targetIndex ].getW();

		final int[] rx = new int[ q.length ];
		final int[] ry = new int[ q.length ];

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
