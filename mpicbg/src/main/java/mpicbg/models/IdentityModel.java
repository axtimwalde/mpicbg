package mpicbg.models;

import java.awt.geom.AffineTransform;
import java.util.Collection;

/**
 * nd-identity {@link AbstractModel}.
 * 
 */
public class IdentityModel extends AbstractModel< IdentityModel > implements
		Affine1D< IdentityModel >, Affine2D< IdentityModel >, Affine3D< IdentityModel >, InvertibleBoundable
{
	final static private long serialVersionUID = -1669311419288285990L;
	
	static final protected int MIN_NUM_MATCHES = 0;
	
	@Override
	public int getMinNumMatches()
	{
		return MIN_NUM_MATCHES;
	}

	@Override
	final public float[] apply( final float[] l )
	{
		return l.clone();
	}
	
	@Override
	final public void applyInPlace( final float[] l ) {}
	
	@Override
	final public float[] applyInverse( final float[] l )
	{
		return l.clone();
	}

	@Override
	final public void applyInverseInPlace( final float[] l ) {}
	
	@Override
	final public void fit(
			final float[][] p,
			final float[][] q,
			final float[] w ) {}
	
	@Override
	final public < P extends PointMatch >void fit( final Collection< P > matches ) {}
	
	@Override
	public IdentityModel copy()
	{
		final IdentityModel m = new IdentityModel();
		m.cost = cost;
		return m;
	}
	
	@Override
	final public void set( final IdentityModel m )
	{
		cost = m.getCost();
	}

	@Override
	final public void preConcatenate( final IdentityModel m ) {}
	
	@Override
	final public void concatenate( final IdentityModel m ) {}
	
	/**
	 * TODO Not yet tested
	 */
	//@Override
	@Override
	public IdentityModel createInverse()
	{
		final IdentityModel ict = new IdentityModel();
		
		ict.cost = cost;
		
		return ict;
	}
	
	@Override
	public void toArray( final float[] data )
	{
		assert data.length > 1 : "Array must be at least 2 fields long.";
				
		data[ 0 ] = 1;
		data[ 1 ] = 0;
		
		if ( data.length > 5 )
		{
			data[ 2 ] = 0;
			if ( data.length > 11 )
			{
				data[ 3 ] = 0;
				data[ 4 ] = 1;
				data[ 5 ] = 0;
				data[ 6 ] = 0;
				data[ 7 ] = 0;
				data[ 8 ] = 1;
				data[ 9 ] = 0;
				data[ 10 ] = 0;
				data[ 11 ] = 0;
			}
			else
			{
				data[ 3 ] = 1;
				data[ 4 ] = 0;
				data[ 5 ] = 0;
			}
		}
	}

	@Override
	public void toArray( final double[] data )
	{
		assert data.length > 1 : "Array must be at least 2 fields long.";
		
		data[ 0 ] = 1;
		data[ 1 ] = 0;
		
		if ( data.length > 5 )
		{
			data[ 2 ] = 0;
			if ( data.length > 11 )
			{
				data[ 3 ] = 0;
				data[ 4 ] = 1;
				data[ 5 ] = 0;
				data[ 6 ] = 0;
				data[ 7 ] = 0;
				data[ 8 ] = 1;
				data[ 9 ] = 0;
				data[ 10 ] = 0;
				data[ 11 ] = 0;
			}
			else
			{
				data[ 3 ] = 1;
				data[ 4 ] = 0;
				data[ 5 ] = 0;
			}
		}
	}

	@Override
	public void toMatrix( final float[][] data )
	{
		assert data.length > 0 && data[ 0 ].length > 1 : "Matrix must be at least 1x2 fields large.";
		
		data[ 0 ][ 0 ] = 1;
		data[ 0 ][ 1 ] = 0;
		
		if ( data.length > 1 )
		{
			data[ 0 ][ 2 ] = 0;
			data[ 1 ][ 0 ] = 0;
			data[ 1 ][ 1 ] = 1;
			data[ 1 ][ 2 ] = 0;
		}
		
		if ( data.length > 2 )
		{
			data[ 0 ][ 3 ] = 0;
			data[ 1 ][ 3 ] = 0;
			data[ 2 ][ 0 ] = 0;
			data[ 2 ][ 1 ] = 0;
			data[ 2 ][ 2 ] = 1;
			data[ 2 ][ 3 ] = 0;
		}
	}

	@Override
	public void toMatrix( final double[][] data )
	{
		assert data.length > 0 && data[ 0 ].length > 1 : "Matrix must be at least 1x2 fields large.";
		
		data[ 0 ][ 0 ] = 1;
		data[ 0 ][ 1 ] = 0;
		
		if ( data.length > 1 )
		{
			data[ 0 ][ 2 ] = 0;
			data[ 1 ][ 0 ] = 0;
			data[ 1 ][ 1 ] = 1;
			data[ 1 ][ 2 ] = 0;
		}
		
		if ( data.length > 2 )
		{
			data[ 0 ][ 3 ] = 0;
			data[ 1 ][ 3 ] = 0;
			data[ 2 ][ 0 ] = 0;
			data[ 2 ][ 1 ] = 0;
			data[ 2 ][ 2 ] = 1;
			data[ 2 ][ 3 ] = 0;
		}
	}

	@Override
	public AffineTransform createAffine()
	{
		return new AffineTransform();
	}

	@Override
	public AffineTransform createInverseAffine()
	{
		return new AffineTransform();
	}

	@Override
	public void estimateBounds( final float[] min, final float[] max ) {}

	@Override
	public void estimateInverseBounds( final float[] min, final float[] max ) {}
}
