package mpicbg.models;

import mpicbg.util.Util;

public abstract class AbstractAffineModel3D < M extends AbstractAffineModel3D< M > > extends AbstractModel< M > implements InvertibleBoundable, Affine3D< M > 
{
	private static final long serialVersionUID = 3560110462477641790L;

	public abstract float[] getMatrix( final float[] m );
	
	@Override
	public void estimateBounds( final float[] min, final float[] max )
	{
		final float[] rMin = new float[]{ Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE };
		final float[] rMax = new float[]{ -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE };
		
		final float[] f = min.clone();
		
		applyInPlace( f );
		Util.min( rMin, f );
		Util.max( rMax, f );
		
		f[ 0 ] = max[ 0 ];
		f[ 1 ] = min[ 1 ];
		f[ 2 ] = min[ 2 ];
		applyInPlace( f );
		Util.min( rMin, f );
		Util.max( rMax, f );
		
		f[ 0 ] = min[ 0 ];
		f[ 1 ] = max[ 1 ];
		f[ 2 ] = min[ 2 ];
		applyInPlace( f );
		Util.min( rMin, f );
		Util.max( rMax, f );
		
		f[ 0 ] = max[ 0 ];
		f[ 1 ] = max[ 1 ];
		f[ 2 ] = min[ 2 ];
		applyInPlace( f );
		Util.min( rMin, f );
		Util.max( rMax, f );
		
		f[ 0 ] = min[ 0 ];
		f[ 1 ] = min[ 1 ];
		f[ 2 ] = max[ 2 ];
		applyInPlace( f );
		Util.min( rMin, f );
		Util.max( rMax, f );
		
		f[ 0 ] = max[ 0 ];
		f[ 1 ] = min[ 1 ];
		f[ 2 ] = max[ 2 ];
		applyInPlace( f );
		Util.min( rMin, f );
		Util.max( rMax, f );
		
		f[ 0 ] = min[ 0 ];
		f[ 1 ] = max[ 1 ];
		f[ 2 ] = max[ 2 ];
		applyInPlace( f );
		Util.min( rMin, f );
		Util.max( rMax, f );
		
		f[ 0 ] = max[ 0 ];
		f[ 1 ] = max[ 1 ];
		f[ 2 ] = max[ 2 ];
		applyInPlace( f );
		Util.min( rMin, f );
		Util.max( rMax, f );
		
		min[ 0 ] = rMin[ 0 ];
		min[ 1 ] = rMin[ 1 ];
		min[ 2 ] = rMin[ 2 ];
		
		max[ 0 ] = rMax[ 0 ];
		max[ 1 ] = rMax[ 1 ];
		max[ 2 ] = rMax[ 2 ];
	}

	/**
	 * TODO not yet tested!
	 */
	@Override
	public void estimateInverseBounds( final float[] min, final float[] max ) throws NoninvertibleModelException
	{
		final float[] rMin = new float[]{ Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE };
		final float[] rMax = new float[]{ -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE };
		
		final float[] f = min.clone();
		
		applyInverseInPlace( f );
		Util.min( rMin, f );
		Util.max( rMax, f );
		
		f[ 0 ] = max[ 0 ];
		f[ 1 ] = min[ 1 ];
		f[ 2 ] = min[ 2 ];
		applyInverseInPlace( f );
		Util.min( rMin, f );
		Util.max( rMax, f );
		
		f[ 0 ] = min[ 0 ];
		f[ 1 ] = max[ 1 ];
		f[ 2 ] = min[ 2 ];
		applyInverseInPlace( f );
		Util.min( rMin, f );
		Util.max( rMax, f );
		
		f[ 0 ] = max[ 0 ];
		f[ 1 ] = max[ 1 ];
		f[ 2 ] = min[ 2 ];
		applyInverseInPlace( f );
		Util.min( rMin, f );
		Util.max( rMax, f );
		
		f[ 0 ] = min[ 0 ];
		f[ 1 ] = min[ 1 ];
		f[ 2 ] = max[ 2 ];
		applyInverseInPlace( f );
		Util.min( rMin, f );
		Util.max( rMax, f );
		
		f[ 0 ] = max[ 0 ];
		f[ 1 ] = min[ 1 ];
		f[ 2 ] = max[ 2 ];
		applyInverseInPlace( f );
		Util.min( rMin, f );
		Util.max( rMax, f );
		
		f[ 0 ] = min[ 0 ];
		f[ 1 ] = max[ 1 ];
		f[ 2 ] = max[ 2 ];
		applyInverseInPlace( f );
		Util.min( rMin, f );
		Util.max( rMax, f );
		
		f[ 0 ] = max[ 0 ];
		f[ 1 ] = max[ 1 ];
		f[ 2 ] = max[ 2 ];
		applyInverseInPlace( f );
		Util.min( rMin, f );
		Util.max( rMax, f );
		
		min[ 0 ] = rMin[ 0 ];
		min[ 1 ] = rMin[ 1 ];
		min[ 2 ] = rMin[ 2 ];
		
		max[ 0 ] = rMax[ 0 ];
		max[ 1 ] = rMax[ 1 ];
		max[ 2 ] = rMax[ 2 ];
	}
	
}
