package mpicbg.models;


public abstract class AbstractAffineModel1D< M extends AbstractAffineModel1D< M > > extends AbstractModel< M > implements InvertibleBoundable, Affine1D< M >
{
	private static final long serialVersionUID = -1479918295700200535L;

	public abstract float[] getMatrix( final float[] m );

	@Override
	public void estimateBounds( final float[] min, final float[] max )
	{
		applyInPlace( min );
		applyInPlace( max );
		if ( min[ 0 ] > max[ 0 ] )
		{
			final float tmp = min[ 0 ];
			min[ 0 ] = max[ 0 ];
			max[ 0 ] = tmp;
		}
	}

	/**
	 * TODO not yet tested!
	 */
	@Override
	public void estimateInverseBounds( final float[] min, final float[] max ) throws NoninvertibleModelException
	{
		applyInverseInPlace( min );
		applyInverseInPlace( max );
		if ( min[ 0 ] > max[ 0 ] )
		{
			final float tmp = min[ 0 ];
			min[ 0 ] = max[ 0 ];
			max[ 0 ] = tmp;
		}
	}
}
