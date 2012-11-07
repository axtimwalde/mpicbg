package mpicbg.models;

import java.util.Collection;

/**
 * Smooth coordinate transformation interpolating between a set of control
 * points that are maped exactly on top of each other using landmark based deformation by means
 * of Moving Least Squares as described by \citet{SchaeferAl06}.
 * 
 * BibTeX:
 * <pre>
 * &#64;article{SchaeferAl06,
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
 */
public abstract class AbstractMovingLeastSquaresTransform implements CoordinateTransform
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
	
	abstract public void setMatches( final Collection< PointMatch > matches )
		throws NotEnoughDataPointsException, IllDefinedDataPointsException;
	
	protected double weigh( final double d )
	{
		return 1.0 / Math.pow( d, alpha );
	}
	
	@Override
	final public float[] apply( final float[] location )
	{
		final float[] a = location.clone();
		applyInPlace( a );
		return a;
	}

	@Override
	abstract public void applyInPlace( final float[] location );
}
