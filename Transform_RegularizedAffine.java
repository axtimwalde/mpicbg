import ij.gui.PointRoi;
import mpicbg.ij.InteractiveInvertibleCoordinateTransform;
import mpicbg.models.AffineModel2D;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import mpicbg.models.RegularizedAffineModel2D;
import mpicbg.models.RigidModel2D;

public class Transform_RegularizedAffine extends InteractiveInvertibleCoordinateTransform< RegularizedAffineModel2D< AffineModel2D, RigidModel2D > >
{
	final protected RegularizedAffineModel2D< AffineModel2D, RigidModel2D > model =
			new RegularizedAffineModel2D< AffineModel2D, RigidModel2D >( new AffineModel2D(), new RigidModel2D(), 0.5f );
	
	@Override
	final protected RegularizedAffineModel2D< AffineModel2D, RigidModel2D > myModel() { return model; }
	
	@Override
	final protected void setHandles()
	{
		final int[] x = new int[]{ imp.getWidth() / 4, 3 * imp.getWidth() / 4, imp.getWidth() / 4 };
		final int[] y = new int[]{ imp.getHeight() / 4, imp.getHeight() / 2, 3 * imp.getHeight() / 4 };
		
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
	final protected void updateHandles( final int x, final int y )
	{
		final float[] fq = q[ targetIndex ].getW();
			
		final int[] rx = new int[ q.length ];
		final int[] ry = new int[ q.length ];
		
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
