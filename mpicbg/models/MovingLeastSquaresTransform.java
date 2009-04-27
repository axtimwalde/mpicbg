package mpicbg.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Smooth coordinate transformation interpolating between a set of control
 * points that are maped exactly on top of each other using landmark based deformation by means
 * of Moving Least Squares as described by \citet{SchaeferAl06}.
 * 
 * BibTeX:
 * <pre>
 * @article{SchaeferAl06,
 *   author    = {Scott Schaefer and Travis McPhail and Joe Warren},
 *   title     = {Image deformation using moving least squares},
 *   journal   = {ACM Transactions on Graphics},
 *   volume    = {25},
 *   number    = {3},
 *   month     = {July},
 *   year      = {2006},
 *   issn      = {0730-0301},
 *   pages     = {533--540},
 *   publisher = {ACM},
 *   address   = {New York, NY, USA},
 * }
 * </pre>
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 */
public class MovingLeastSquaresTransform implements CoordinateTransform
{
	protected Model< ? > model = null;
	final public Model< ? > getModel(){ return model; }
	final public void setModel( final Model< ? > model ){ this.model = model; }
	final public void setModel( final Class< ? extends Model< ? > > modelClass ) throws Exception
	{
		model = modelClass.newInstance();
	}
	
	protected float alpha = 1.0f;
	final public float getAlpha(){ return alpha; }
	final public void setAlpha( final float alpha ){ this.alpha = alpha; }
	
	final protected Set< PointMatch > matches = new HashSet< PointMatch >();
	final public Set< PointMatch > getMatches(){ return matches; }
	final public void setMatches( final Collection< PointMatch > matches )
	{
		this.matches.clear();
		this.matches.addAll( matches );
	}
	
	final protected double weigh( final double d )
	{
		return 1.0 / Math.pow( d, alpha );
	}
	
	//@Override
	public float[] apply( float[] location )
	{
		final float[] a = location.clone();
		applyInPlace( a );
		return a;
	}

	//@Override
	public void applyInPlace( final float[] location )
	{
		final Collection< PointMatch > weightedMatches = new ArrayList< PointMatch >();
		for ( final PointMatch m : matches )
		{
			final float[] l = m.getP1().getL();

//			/* specific for 2d */
//			final float dx = l[ 0 ] - location[ 0 ];
//			final float dy = l[ 1 ] - location[ 1 ];
//			
//			final float weight = m.getWeight() * ( float )weigh( 1.0f + Math.sqrt( dx * dx + dy * dy ) );
			
			float s = 0;
			for ( int i = 0; i < location.length; ++i )
			{
				final float dx = l[ i ] - location[ i ];
				s += dx * dx;
			}
			if ( s <= 0 )
			{
				final float[] w = m.getP2().getW();
				for ( int i = 0; i < location.length; ++i )
					location[ i ] = w[ i ];
				return;
			}
			final float weight = m.getWeight() * ( float )weigh( Math.sqrt( s ) );
			final PointMatch mw = new PointMatch( m.getP1(), m.getP2(), weight );
			weightedMatches.add( mw );
		}
		
		try 
		{
			model.fit( weightedMatches );
			model.applyInPlace( location );
		}
		catch ( IllDefinedDataPointsException e ){}
		catch ( NotEnoughDataPointsException e ){}
	}

	//@Override
	public void estimateBounds( final float[] min, final float[] max )
	{
		// TODO Auto-generated method stub
		
	}
}
