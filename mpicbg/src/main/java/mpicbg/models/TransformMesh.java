/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package mpicbg.models;

import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import mpicbg.util.Util;

/**
 * Triangular transformation mesh.
 *
 * See an example to find out how the mesh is constructed:
 *
 * numX = 4; numY = 3:
 * <pre>
 * *---*---*---*
 * |\ / \ / \ /|
 * | *---*---* |
 * |/ \ / \ / \|
 * *---*---*---*
 * |\ / \ / \ /|
 * | *---*---* |
 * |/ \ / \ / \|
 * *---*---*---*
 * </pre>
 *
 * Each vertex is given as a {@link PointMatch} with
 * {@link PointMatch#getP1() p1} being the original point and
 * {@link PointMatch#getP2() p2} being the transferred point.  Keep in mind
 * that Points store local and world coordinates with local coordinates being
 * constant and world coordinates being mutable.  That is initially
 * {@link Point#getL() p1.l} = {@link Point#getW() p1.w} =
 * {@link Point#getL() p2.l} while {@link Point#getW() p1.w} is the transferred
 * location of the vertex.
 *
 * Three adjacent vertices span a triangle.  All pixels inside a triangle will
 * be transferred by a {@link AffineModel2D 2d affine transform} that is
 * defined by the three vertices.  Given the abovementioned definition of a
 * vertex as PointMatch, this {@link AffineModel2D 2d affine transform} is a
 * forward transform (p1.l->p2.w).
 *
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
public class TransformMesh implements InvertibleCoordinateTransform
{
	private static final long serialVersionUID = 2557331683355356863L;

	final protected double width, height;
	public double getWidth(){ return width; }
	public double getHeight(){ return height; }

	final protected HashMap< AffineModel2D, ArrayList< PointMatch > > av = new HashMap< AffineModel2D, ArrayList< PointMatch > >();
	public HashMap< AffineModel2D, ArrayList< PointMatch > > getAV(){ return av; }
	final protected HashMap< PointMatch, ArrayList< AffineModel2D > > va = new HashMap< PointMatch, ArrayList< AffineModel2D > >();
	public HashMap< PointMatch, ArrayList< AffineModel2D > > getVA(){ return va; };

	final static protected PointFactory< Point > defaultPointFactory = new PointFactory< Point >()
	{
		private static final long serialVersionUID = -8338916724246569904L;

		@Override
		final public Point createPoint( final double[] l )
		{
			return new Point( l );
		}
	};

	final static protected PointMatchFactory< PointMatch > defaultPointMatchFactory = new PointMatchFactory< PointMatch >()
	{
		private static final long serialVersionUID = -3034111068615619105L;

		@Override
		final public PointMatch createPointMatch( final Point p1, final Point p2 )
		{
			return new PointMatch( p1, p2 );
		}

		@Override
		final public PointMatch createPointMatch( final Point p1, final Point p2, final double w )
		{
			return new PointMatch( p1, p2, w );
		}
	};

	protected TransformMesh(
			final int numX,
			final int numY,
			final double width,
			final double height,
			final PointFactory< ? > pointFactory,
			final PointMatchFactory< ? > pointMatchFactory )
	{
		final int numXs = Math.max( 2, numX );
		final int numYs = Math.max( 2, numY );

		final double w = width * height / numXs / numYs;
		final PointMatch[] pq = new PointMatch[ numXs * numYs + ( numXs - 1 ) * ( numYs - 1 ) ];

		this.width = width;
		this.height = height;

		final double dy = ( height - 1 ) / ( numYs - 1 );
		final double dx = ( width - 1 ) / ( numXs - 1 );

		int i = 0;
		for ( int xi = 0; xi < numXs; ++xi )
		{
			final double xip = xi * dx;
			final Point p = pointFactory.createPoint( new double[]{ xip, 0 } );
			pq[ i ] = pointMatchFactory.createPointMatch( p, p.clone(), w );

			++i;
		}

		Point p;
		int i1, i2, i3;
		ArrayList< PointMatch > t1, t2;

		for ( int yi = 1; yi < numYs; ++yi )
		{
			// odd row
			double yip = yi * dy - dy / 2;

			p  = pointFactory.createPoint( new double[]{ dx - dx / 2, yip } );
			pq[ i ] = pointMatchFactory.createPointMatch( p, p.clone(), w );

			i1 = i - numXs;
			i2 = i1 + 1;

			t1 = new ArrayList< PointMatch >();
			t1.add( pq[ i1 ] );
			t1.add( pq[ i2 ] );
			t1.add( pq[ i ] );

			addTriangle( t1 );

			++i;

			for ( int xi = 2; xi < numXs; ++xi )
			{
				final double xip = xi * dx - dx / 2;

				p  = pointFactory.createPoint( new double[]{ xip, yip } );
				pq[ i ] = pointMatchFactory.createPointMatch( p, p.clone(), w );

				i1 = i - numXs;
				i2 = i1 + 1;
				i3 = i - 1;

				t1 = new ArrayList< PointMatch >();
				t1.add( pq[ i1 ] );
				t1.add( pq[ i2 ] );
				t1.add( pq[ i ] );

				addTriangle( t1 );

				t2 = new ArrayList< PointMatch >();
				t2.add( pq[ i1 ] );
				t2.add( pq[ i ] );
				t2.add( pq[ i3 ] );

				addTriangle( t2 );

				++i;
			}

			// even row
			yip = yi * dy;
			p  = pointFactory.createPoint( new double[]{ 0, yip } );
			pq[ i ] = pointMatchFactory.createPointMatch( p, p.clone(), w );

			i1 = i - numXs + 1;
			i2 = i1 - numXs;

			t1 = new ArrayList< PointMatch >();
			t1.add( pq[ i2 ] );
			t1.add( pq[ i1 ] );
			t1.add( pq[ i ] );

			addTriangle( t1 );

			++i;

			for ( int xi = 1; xi < numXs - 1; ++xi )
			{
				final double xip = xi * dx;

				p = pointFactory.createPoint( new double[]{ xip, yip } );
				pq[ i ] = pointMatchFactory.createPointMatch( p, p.clone(), w );

				i1 = i - numXs;
				i2 = i1 + 1;
				i3 = i - 1;

				t1 = new ArrayList< PointMatch >();
				t1.add( pq[ i1 ] );
				t1.add( pq[ i ] );
				t1.add( pq[ i3 ] );

				addTriangle( t1 );

				t2 = new ArrayList< PointMatch >();
				t2.add( pq[ i1 ] );
				t2.add( pq[ i2 ] );
				t2.add( pq[ i ] );

				addTriangle( t2 );

				++i;
			}

			p  = pointFactory.createPoint( new double[]{ width - 1, yip } );
			pq[ i ] = pointMatchFactory.createPointMatch( p, p.clone(), w );

			i1 = i - numXs;
			i2 = i1 - numXs + 1;
			i3 = i - 1;

			t1 = new ArrayList< PointMatch >();
			t1.add( pq[ i3 ] );
			t1.add( pq[ i1 ] );
			t1.add( pq[ i ] );

			addTriangle( t1 );

			t2 = new ArrayList< PointMatch >();
			t2.add( pq[ i1 ] );
			t2.add( pq[ i2 ] );
			t2.add( pq[ i ] );

			addTriangle( t2 );

			++i;
		}
	}

	public TransformMesh(
			final int numX,
			final int numY,
			final double width,
			final double height )
	{
		this(
				numX,
				numY,
				width,
				height,
				defaultPointFactory,
				defaultPointMatchFactory );
	}

	final static protected int numY(
			final int numX,
			final double width,
			final double height )
	{
		final int numXs = Math.max( 2, numX );
		final double dx = width / ( numXs - 1 );
		final double dy = 2.0f * Math.sqrt( 3.0f / 4.0f * dx * dx );
		return Math.max( 2, Util.roundPos( height / dy ) + 1 );
	}

	public TransformMesh(
			final int numX,
			final double width,
			final double height )
	{
		this( numX, numY( numX, width, height ), width, height );
	}

	/**
	 * Add a triangle defined by 3 PointMatches that defines an
	 * AffineTransform2D.
	 *
	 * @param t
	 *            3 PointMatches (will not be copied, so do not reuse this
	 *            list!)
	 */
	public void addTriangle( final ArrayList< PointMatch > t )
	{
		final AffineModel2D m = new AffineModel2D();
		try
		{
			m.fit( t );
		}
		catch ( final NotEnoughDataPointsException e ) { e.printStackTrace(); }
		catch ( final IllDefinedDataPointsException e ) { e.printStackTrace(); }
		av.put( m, t );

		for ( final PointMatch pm : t )
		{
			if ( !va.containsKey( pm ) )
				va.put( pm, new ArrayList< AffineModel2D >() );
			va.get( pm ).add( m );
		}
	}

	protected void illustrateTriangle( final AffineModel2D ai, final GeneralPath path )
	{
		final ArrayList< PointMatch > m = av.get( ai );

		final double[] w = m.get( 0 ).getP2().getW();
		path.moveTo( w[ 0 ], w[ 1 ] );

		for ( int i = 1; i < m.size(); ++i )
		{
			final double[] wi = m.get( i ).getP2().getW();
			path.lineTo( wi[ 0 ], wi[ 1 ] );
		}
		path.closePath();
	}

	/**
	 * Create a Shape that illustrates the mesh.
	 *
	 * @return the illustration
	 */
	public Shape illustrateMesh()
	{
		final GeneralPath path = new GeneralPath();

		final Set< AffineModel2D > s = av.keySet();
		for ( final AffineModel2D ai : s )
			illustrateTriangle( ai, path );

		return path;
	}

	private String illustrateTriangleSVG( final AffineModel2D ai )
	{
		String svg = "";

		final ArrayList< PointMatch > m = av.get( ai );

		final double[] w = m.get( 0 ).getP2().getW();
		svg += "M " + w[ 0 ] + " " + w[ 1 ] + " ";

		for ( int i = 1; i < m.size(); ++i )
		{
			final double[] wi = m.get( i ).getP2().getW();
			svg += "L " + wi[ 0 ] + " "  + wi[ 1 ] + " ";
		}

		svg += "Z ";

		return svg;
	}

	/**
	 * Create an SVG path that illustrates the mesh.
	 *
	 * @return svg path-definition
	 */
	public String illustrateMeshSVG()
	{
		String svg = "<path style=\"fill:none;fill-rule:evenodd;stroke:#000000;stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;stroke-opacity:1\" d=\"";
		final Set< AffineModel2D > s = av.keySet();
		for ( final AffineModel2D ai : s )
			svg += illustrateTriangleSVG( ai );

		svg += "\" />";

		return svg;
	}

	/**
	 * Create an SVG path that illustrates the best regid approximation of the
	 * mesh as a rotated square.
	 *
	 * @return svg path-definition
	 */
	public String illustrateBestRigidSVG()
	{
		String svg = "<path style=\"fill:none;fill-rule:evenodd;stroke:#000000;stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;stroke-opacity:1\" d=\"";
		final Set< PointMatch > s = va.keySet();
		final RigidModel2D m = new RigidModel2D();
		try
		{
			m.fit( s );
		}
		catch ( final NotEnoughDataPointsException ex )
		{
			ex.printStackTrace();
			return "";
		}

		final double[] l = new double[]{ 0, 0 };
		m.applyInPlace( l );
		svg += "M " + l[ 0 ] + " " + l[ 1 ] + " ";

		l[ 0 ] = width;
		l[ 1 ] = 0;
		m.applyInPlace( l );
		svg += "L " + l[ 0 ] + " " + l[ 1 ] + " ";

		l[ 0 ] = width;
		l[ 1 ] = height;
		m.applyInPlace( l );
		svg += "L " + l[ 0 ] + " " + l[ 1 ] + " ";

		l[ 0 ] = 0;
		l[ 1 ] = height;
		m.applyInPlace( l );
		svg += "L " + l[ 0 ] + " " + l[ 1 ] + " ";

		svg += "Z";

		svg += "\" />";

		return svg;
	}

	/**
	 * Update all affine transformations that would have been affected by a
	 * given {@link PointMatch Vertex}.
	 *
	 * @param p
	 */
	public void updateAffine( final PointMatch p )
	{
		for ( final AffineModel2D ai : va.get( p ) )
		{
			try
			{
				ai.fit( av.get( ai ) );
			}
			catch ( final NotEnoughDataPointsException e ) { e.printStackTrace(); }
			catch ( final IllDefinedDataPointsException e ) { e.printStackTrace(); }
		}
	}

	/**
	 * Update all affine transformations.
	 *
	 */
	public void updateAffines()
	{
		final Set< AffineModel2D > s = av.keySet();
		for ( final AffineModel2D ai : s )
		{
			try
			{
				ai.fit( av.get( ai ) );
			}
			catch ( final NotEnoughDataPointsException e ) { e.printStackTrace(); }
			catch ( final IllDefinedDataPointsException e ) { e.printStackTrace(); }
		}
	}

	/**
	 * Find the closest {@linkplain Point source point} to a given coordinate.
	 * Each vertex being defined by a {@link PointMatch}, the source
	 * coordinates are the {@linkplain Point#getL() local coordinates} of the
	 * {@linkplain PointMatch#getP1() source point}.
	 *
	 * @param there
	 * @return closest {@link PointMatch} in terms of the
	 *   {@linkplain PointMatch#getP1() source point}
	 */
	public PointMatch findClosestSourcePoint( final double[] there )
	{
		final Set< PointMatch > points = va.keySet();

		PointMatch closest = null;
		double cd = Double.MAX_VALUE;
		for ( final PointMatch m : points )
		{
			final double[] here = m.getP1().getL();
			final double dx = here[ 0 ] - there[ 0 ];
			final double dy = here[ 1 ] - there[ 1 ];
			final double d = dx * dx + dy * dy;
			if ( d < cd )
			{
				cd = d;
				closest = m;
			}
		}
		return closest;
	}

	/**
	 * Find the closest {@linkplain Point target point} to a given coordinate.
	 * Each vertex being defined by a {@link PointMatch}, the target
	 * coordinates are the {@linkplain Point#getW() world coordinates} of the
	 * {@linkplain PointMatch#getP2() target point}.
	 *
	 * @param there
	 * @return closest {@link PointMatch} in terms of the
	 *   {@linkplain PointMatch#getP2() target point}
	 */
	public PointMatch findClosestTargetPoint( final double[] there )
	{
		final Set< PointMatch > points = va.keySet();

		PointMatch closest = null;
		double cd = Double.MAX_VALUE;
		for ( final PointMatch m : points )
		{
			final double[] here = m.getP2().getW();
			final double dx = here[ 0 ] - there[ 0 ];
			final double dy = here[ 1 ] - there[ 1 ];
			final double d = dx * dx + dy * dy;
			if ( d < cd )
			{
				cd = d;
				closest = m;
			}
		}
		return closest;
	}

	/**
	 * Checks if a location is inside a given polygon at the target side or not.
	 *
	 * @param pm
	 * @param t
	 * @return
	 */
	static public boolean isInConvexTargetPolygon(
			final ArrayList< PointMatch > pm,
			final double[] t )
	{
		assert t.length == 2 : "2d transform meshs can be applied to 2d points only.";

		for ( int i = 0; i < pm.size(); ++i )
		{
			final PointMatch r1 = pm.get( i );
			final PointMatch r2 = pm.get( ( i + 1 ) % pm.size() );
			final double[] t1 = r1.getP2().getW();
			final double[] t2 = r2.getP2().getW();

			final double x1 = t2[ 0 ] - t1[ 0 ];
			final double y1 = t2[ 1 ] - t1[ 1 ];
			final double x2 = t[ 0 ] - t1[ 0 ];
			final double y2 = t[ 1 ] - t1[ 1 ];

			if ( x1 * y2 - y1 * x2 < 0 ) return false;
		}
		return true;
	}


	/**
	 * Checks if a location is inside a given polygon at the source side or not.
	 *
	 * @param pm
	 * @param t
	 * @return
	 */
	final static public boolean isInSourcePolygon(
			final ArrayList< PointMatch > pm,
			final double[] t )
	{
		assert t.length == 2 : "2d transform meshs can be applied to 2d points only.";

		for ( int i = 0; i < pm.size(); ++i )
		{
			final PointMatch r1 = pm.get( i );
			final PointMatch r2 = pm.get( ( i + 1 ) % pm.size() );
			final double[] t1 = r1.getP1().getL();
			final double[] t2 = r2.getP1().getL();

			final double x1 = t2[ 0 ] - t1[ 0 ];
			final double y1 = t2[ 1 ] - t1[ 1 ];
			final double x2 = t[ 0 ] - t1[ 0 ];
			final double y2 = t[ 1 ] - t1[ 1 ];

			if ( x1 * y2 - y1 * x2 < 0 ) return false;
		}
		return true;
	}

	@Override
	public double[] apply( final double[] location )
	{
		assert location.length == 2 : "2d transform meshs can be applied to 2d points only.";

		final double[] transformed = location.clone();
		applyInPlace( transformed );
		return transformed;
	}

	@Override
	public void applyInPlace( final double[] location )
	{
		assert location.length == 2 : "2d transform meshs can be applied to 2d points only.";

		final Set< AffineModel2D > s = av.keySet();
		for ( final AffineModel2D ai : s )
		{
			final ArrayList< PointMatch > pm = av.get( ai );
			if ( isInSourcePolygon( pm, location ) )
			{
				ai.applyInPlace( location );
				return;
			}
		}
	}

	//@Override
	@Override
	public double[] applyInverse( final double[] location ) throws NoninvertibleModelException
	{
		assert location.length == 2 : "2d transform meshs can be applied to 2d points only.";

		final double[] transformed = location.clone();
		applyInverseInPlace( transformed );
		return transformed;
	}

	//@Override
	@Override
	public void applyInverseInPlace( final double[] location ) throws NoninvertibleModelException
	{
		assert location.length == 2 : "2d transform meshs can be applied to 2d points only.";

		final Set< AffineModel2D > s = av.keySet();
		for ( final AffineModel2D ai : s )
		{
			final ArrayList< PointMatch > pm = av.get( ai );
			if ( isInConvexTargetPolygon( pm, location ) )
			{
				ai.applyInverseInPlace( location );
				return;
			}
		}
		throw new NoninvertibleModelException( "Noninvertible location ( " + location[ 0 ] + ", " + location[ 1 ] + " )" );
	}

	/**
	 * TODO Not yet tested
	 */
	//@Override
	@Override
	public TransformMesh createInverse()
	{
		final TransformMesh ict = new TransformMesh( 0, 0, width, height );

		final Set< PointMatch > v = va.keySet();
		final HashMap< PointMatch, PointMatch > vv = new HashMap< PointMatch, PointMatch >();

		for ( final PointMatch p : v )
		{
			final double[] l = p.getP1().getL();
			final double[] w = p.getP2().getW();

			final Point pi1 = new Point( w.clone() );
			final Point pi2 = new Point( w.clone() );
			final double[] pi2w = pi2.getW();
			for ( int i = 0; i < pi2w.length; ++i )
				pi2w[ i ] = l[ i ];

			final PointMatch pim = new PointMatch( pi1, pi2 );

			vv.put( p, pim );
		}

		ict.va.clear();
		ict.av.clear();

		for ( final Entry< PointMatch, PointMatch > e : vv.entrySet() )
			ict.va.put( e.getValue(), new ArrayList< AffineModel2D >() );

		for ( final Entry< AffineModel2D, ArrayList< PointMatch > > e : av.entrySet() )
		{
			final ArrayList< PointMatch > pm = new ArrayList< PointMatch >();
			final AffineModel2D a = new AffineModel2D();
			for ( final PointMatch p : e.getValue() )
			{
				final PointMatch q = vv.get( p );
				va.get( q ).add( a );
				pm.add( q );
			}
			ict.av.put( a, pm );
		}

		ict.updateAffines();

		return ict;
	}

	/**
	 * Initialize the mesh with a {@link CoordinateTransform}.
	 *
	 * @param t
	 */
	public void init( final CoordinateTransform t )
	{
		final Set< PointMatch > vertices = va.keySet();
		for ( final PointMatch vertex : vertices )
			vertex.getP2().apply( t );

		updateAffines();
	}

	/**
	 * Scale all vertex coordinates
	 *
	 * @param scale
	 */
	public void scale( final double scale )
	{
		for ( final PointMatch m : va.keySet() )
		{
			final Point p1 = m.getP1();
			final Point p2 = m.getP2();

			final double[] l1 = p1.getL();
			final double[] w1 = p1.getW();
			final double[] l2 = p2.getL();
			final double[] w2 = p2.getW();

			for ( int i = 0; i < l1.length; ++i )
			{
				l1[ i ] *= scale;
				w1[ i ] *= scale;
				l2[ i ] *= scale;
				w2[ i ] *= scale;
			}

			updateAffines();
		}
	}

	/**
	 * Calculate bounding box
	 *
	 * @param min
	 * @param max
	 */
	public void bounds( final double[] min, final double[] max )
	{
		min[ 0 ] = min[ 1 ] = Double.MAX_VALUE;
		max[ 0 ] = max[ 1 ] = -Double.MAX_VALUE;

		final Set< PointMatch > vertices = va.keySet();
		for ( final PointMatch vertex : vertices )
		{
			final double[] w = vertex.getP2().getW();

			if ( w[ 0 ] < min[ 0 ] ) min[ 0 ] = w[ 0 ];
			if ( w[ 0 ] > max[ 0 ] ) max[ 0 ] = w[ 0 ];
			if ( w[ 1 ] < min[ 1 ] ) min[ 1 ] = w[ 1 ];
			if ( w[ 1 ] > max[ 1 ] ) max[ 1 ] = w[ 1 ];
		}
	}
}
