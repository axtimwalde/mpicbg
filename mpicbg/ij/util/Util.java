package mpicbg.ij.util;

import java.util.Collection;
import java.util.Random;

import mpicbg.models.Point;

import ij.gui.PointRoi;
import ij.process.FloatProcessor;

public class Util
{
	private Util(){}
	
	final static public PointRoi pointsToPointRoi( final Collection< Point > points )
	{
		final int[] x = new int[ points.size() ];
		final int[] y = new int[ points.size() ];
		
		int i = 0;
		for ( final Point p : points )
		{
			final float[] l = p.getL();
			x[ i ] = Math.round( l[ 0 ] );
			y[ i ] = Math.round( l[ 1 ] );
			++i;
		}
		return new PointRoi( x, y, x.length );
	}
	
	final static public void fillWithNoise( final FloatProcessor fp )
    {
    	final float[] data = ( float[] )fp.getPixels();
    	final Random random = new Random( System.nanoTime() );
    	for ( int i = 0; i < data.length; ++i )
    		data[ i ] = random.nextFloat();
    }
    
    final static public void fillWithNaN( final FloatProcessor fp )
    {
    	final float[] data = ( float[] )fp.getPixels();
    	for ( int i = 0; i < data.length; ++i )
    		data[ i ] = Float.NaN;
    }
}
