import java.util.Arrays;

import ij.IJ;
import ij.gui.PointRoi;
import mpicbg.ij.InteractiveInvertibleCoordinateTransform;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import mpicbg.models.SimilarityModel2D;

public class Transform_Similarity extends InteractiveInvertibleCoordinateTransform< SimilarityModel2D >
{
	final protected SimilarityModel2D model = new SimilarityModel2D();

	@Override
	final protected SimilarityModel2D myModel() { return model; }

	@Override
	final protected void setHandles()
	{
		final int[] x = new int[]{ imp.getWidth() / 4, 3 * imp.getWidth() / 4 };
		final int[] y = new int[]{ imp.getHeight() / 2, imp.getHeight() / 2 };

		p = new Point[]{
				new Point( new double[]{ x[ 0 ], y[ 0 ] } ),
				new Point( new double[]{ x[ 1 ], y[ 1 ] } ) };

		q = new Point[]{
				p[ 0 ].clone(),
				p[ 1 ].clone() };

		m.add( new PointMatch( p[ 0 ], q[ 0 ] ) );
		m.add( new PointMatch( p[ 1 ], q[ 1 ] ) );

		handles = new PointRoi( x, y, 2 );
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

		handles = new PointRoi( rx, ry, 2 );
		imp.setRoi( handles );

		fq[ 0 ] = x;
		fq[ 1 ] = y;
	}

	@Override
	final protected void onReturn()
	{
		final double[] flatmatrix = new double[ 6 ];
		myModel().toArray( flatmatrix );
		IJ.log( "Matrix: " + Arrays.toString( flatmatrix ) );
	}
}
