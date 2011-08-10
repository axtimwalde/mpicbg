package mpicbg.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>Smooth coordinate transformation interpolating between a set of control
 * points that are maped exactly on top of each other using landmark based
 * deformation by means of Moving Least Squares as described by
 * \citet{SchaeferAl06}.</p>
 * 
 * <p>This implementation internally stores the passed {@link PointMatch}
 * objects per reference and is thus best suited for an interactive application
 * where these matches are changed from an external context.</p>
 * 
 * <p>BibTeX:</p>
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
 * @version 0.1b
 */
public class MovingLeastSquaresTransform extends AbstractMovingLeastSquaresTransform
{
	final protected Set< PointMatch > matches = new HashSet< PointMatch >();
	final public Set< PointMatch > getMatches(){ return matches; }
	
	@Override
	final public void setMatches( final Collection< PointMatch > matches ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		this.matches.clear();
		this.matches.addAll( matches );
		model.fit( matches );
	}
	
	@Override
	final public void applyInPlace( final float[] location )
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
			final float weight = m.getWeight() * ( float )weigh( s );
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
}
