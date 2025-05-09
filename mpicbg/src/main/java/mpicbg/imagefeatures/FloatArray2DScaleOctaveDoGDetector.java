/*-
 * #%L
 * MPICBG Core Library.
 * %%
 * Copyright (C) 2008 - 2025 Stephan Saalfeld et. al.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package mpicbg.imagefeatures;

import java.util.Vector;

import mpicbg.util.Matrix3x3;


/**
 * Difference Of Gaussian detector on top of a scale space octave as described
 * by Lowe (2004).
 *
 * BibTeX:
 * <pre>
 * &#64;article{Lowe04,
 *   author  = {David G. Lowe},
 *   title   = {Distinctive Image Features from Scale-Invariant Keypoints},
 *   journal = {International Journal of Computer Vision},
 *   year    = {2004},
 *   volume  = {60},
 *   number  = {2},
 *   pages   = {91--110},
 * }
 * </pre>
 *
 * @author Stephan Saalfeld &lt;saalfeld@mpi-cbg.de&gt;
 * @version 0.1b
 */
public class FloatArray2DScaleOctaveDoGDetector
{
	/**
	 * minimal contrast of a candidate
	 */
	private static final float MIN_CONTRAST = 0.025f;

	/**
	 * maximal curvature ratio, higher values allow more edge-like responses
	 */
	private static final float MAX_CURVATURE = 10;
	private static final float MAX_CURVATURE_RATIO = ( MAX_CURVATURE + 1 ) * ( MAX_CURVATURE + 1 ) / MAX_CURVATURE;

	private FloatArray2DScaleOctave octave;

	/**
	 * detected candidates as float triples 0=>x, 1=>y, 2=>scale index
	 */
	private Vector< double[] > candidates;
	public Vector< double[] > getCandidates()
	{
		return candidates;
	}

	/**
	 * Constructor
	 */
	public FloatArray2DScaleOctaveDoGDetector()
	{
		octave = null;
		candidates = null;
	}

	public void run( final FloatArray2DScaleOctave o )
	{
		octave = o;
		candidates = new Vector< double[] >();
		detectCandidates();
	}

	private void detectCandidates()
	{
		final FloatArray2D[] d = octave.getD();

		for ( int i = d.length - 2; i >= 1; --i )
		{
			final int ia = i - 1;
			final int ib = i + 1;
			for ( int y = d[ i ].height - 2; y >= 1; --y )
			{
				final int r = y * d[ i ].width;
				final int ra = r - d[ i ].width;
				final int rb = r + d[ i ].width;

				X : for ( int x = d[ i ].width - 2; x >= 1; --x )
				{
					int ic = i;
					int iac = ia;
					int ibc = ib;
					int yc = y;
					int rc = r;
					int rac = ra;
					int rbc = rb;
					int xc = x;
					int xa = xc - 1;
					int xb = xc + 1;
					double e111 = d[ ic ].data[ r + xc ];

					// check if d(x, y, i) is an extremum
					// do it pipeline-friendly ;)

					double   e000 = d[ iac ].data[ rac + xa ];
					boolean isMax = e000 < e111;
					boolean isMin = e000 > e111;
					if ( !( isMax || isMin ) ) continue;
					double   e100 = d[ iac ].data[ rac + xc ];
					isMax &= e100 < e111;
					isMin &= e100 > e111;
					if ( !( isMax || isMin ) ) continue;
					double e200 = d[ iac ].data[ rac + xb ];
					isMax &= e200 < e111;
					isMin &= e200 > e111;
					if ( !( isMax || isMin ) ) continue;

					double   e010 = d[ iac ].data[ rc + xa ];
					isMax &= e010 < e111;
					isMin &= e010 > e111;
					if ( !( isMax || isMin ) ) continue;
					double   e110 = d[ iac ].data[ rc + xc ];
					isMax &= e110 < e111;
					isMin &= e110 > e111;
					if ( !( isMax || isMin ) ) continue;
					double   e210 = d[ iac ].data[ rc + xb ];
					isMax &= e210 < e111;
					isMin &= e210 > e111;
					if ( !( isMax || isMin ) ) continue;

					double   e020 = d[ iac ].data[ rbc + xa ];
					isMax &= e020 < e111;
					isMin &= e020 > e111;
					if ( !( isMax || isMin ) ) continue;
					double   e120 = d[ iac ].data[ rbc + xc ];
					isMax &= e120 < e111;
					isMin &= e120 > e111;
					if ( !( isMax || isMin ) ) continue;
					double   e220 = d[ iac ].data[ rbc + xb ];
					isMax &= e220 < e111;
					isMin &= e220 > e111;
					if ( !( isMax || isMin ) ) continue;


					double   e001 = d[ ic ].data[ rac + xa ];
					isMax &= e001 < e111;
					isMin &= e001 > e111;
					if ( !( isMax || isMin ) ) continue;
					double   e101 = d[ ic ].data[ rac + xc ];
					isMax &= e101 < e111;
					isMin &= e101 > e111;
					if ( !( isMax || isMin ) ) continue;
					double   e201 = d[ ic ].data[ rac + xb ];
					isMax &= e201 < e111;
					isMin &= e201 > e111;
					if ( !( isMax || isMin ) ) continue;

					double   e011 = d[ ic ].data[ rc + xa ];
					isMax &= e011 < e111;
					isMin &= e011 > e111;
					if ( !( isMax || isMin ) ) continue;
					double   e211 = d[ ic ].data[ rc + xb ];
					isMax &= e211 < e111;
					isMin &= e211 > e111;
					if ( !( isMax || isMin ) ) continue;

					double   e021 = d[ ic ].data[ rbc + xa ];
					isMax &= e021 < e111;
					isMin &= e021 > e111;
					if ( !( isMax || isMin ) ) continue;
					double   e121 = d[ ic ].data[ rbc + xc ];
					isMax &= e121 < e111;
					isMin &= e121 > e111;
					if ( !( isMax || isMin ) ) continue;
					double   e221 = d[ ic ].data[ rbc + xb ];
					isMax &= e221 < e111;
					isMin &= e221 > e111;
					if ( !( isMax || isMin ) ) continue;


					double   e002 = d[ ibc ].data[ rac + xa ];
					isMax &= e002 < e111;
					isMin &= e002 > e111;
					if ( !( isMax || isMin ) ) continue;
					double   e102 = d[ ibc ].data[ rac + xc ];
					isMax &= e102 < e111;
					isMin &= e102 > e111;
					if ( !( isMax || isMin ) ) continue;
					double   e202 = d[ ibc ].data[ rac + xb ];
					isMax &= e202 < e111;
					isMin &= e202 > e111;
					if ( !( isMax || isMin ) ) continue;

					double   e012 = d[ ibc ].data[ rc + xa ];
					isMax &= e012 < e111;
					isMin &= e012 > e111;
					if ( !( isMax || isMin ) ) continue;
					double   e112 = d[ ibc ].data[ rc + xc ];
					isMax &= e112 < e111;
					isMin &= e112 > e111;
					if ( !( isMax || isMin ) ) continue;
					double   e212 = d[ ibc ].data[ rc + xb ];
					isMax &= e212 < e111;
					isMin &= e212 > e111;
					if ( !( isMax || isMin ) ) continue;

					double   e022 = d[ ibc ].data[ rbc + xa ];
					isMax &= e022 < e111;
					isMin &= e022 > e111;
					if ( !( isMax || isMin ) ) continue;
					double   e122 = d[ ibc ].data[ rbc + xc ];
					isMax &= e122 < e111;
					isMin &= e122 > e111;
					if ( !( isMax || isMin ) ) continue;
					double   e222 = d[ ibc ].data[ rbc + xb ];
					isMax &= e222 < e111;
					isMin &= e222 > e111;
					if ( !( isMax || isMin ) ) continue;

					// so it is an extremum, try to localize it with subpixel
					// accuracy, if it has to be moved for more than 0.5 in at
					// least one direction, try it again there but maximally 5
					// times

					boolean isLocalized = false;
					boolean isLocalizable = true;

					double dx;
				    double dy;
				    double di;

				    double dxx;
				    double dyy;
				    double dii;

				    double dxy;
				    double dxi;
				    double dyi;

				    double ox;
				    double oy;
				    double oi;

				    double od = Double.MAX_VALUE;      // offset square distance

				    double fx = 0;
				    double fy = 0;
				    double fi = 0;

					int t = 5; // maximal number of re-localizations
				    do
					{
				    	--t;

						// derive at (x, y, i) by center of difference
					    dx = ( e211 - e011 ) / 2.0f;
					    dy = ( e121 - e101 ) / 2.0f;
					    di = ( e112 - e110 ) / 2.0f;

					    // create hessian at (x, y, i) by laplace
					    final double e111_2 = 2.0f * e111;
					    dxx = e011 - e111_2 + e211;
					    dyy = e101 - e111_2 + e121;
					    dii = e110 - e111_2 + e112;

					    dxy = ( e221 - e021 - e201 + e001 ) / 4.0f;
					    dxi = ( e212 - e012 - e210 + e010 ) / 4.0f;
					    dyi = ( e122 - e102 - e120 + e100 ) / 4.0f;

					    // invert hessian
					    final double det = Matrix3x3.det( dxx, dxy, dxi, dxy, dyy, dyi, dxi, dyi, dii );
					    if ( det == 0 ) continue X;

					    final double det1 = 1.0 / det;

					    final double hixx = ( dyy * dii - dyi * dyi ) * det1;
					    final double hixy = ( dxi * dyi - dxy * dii ) * det1;
					    final double hixi = ( dxy * dyi - dxi * dyy ) * det1;
					    final double hiyy = ( dxx * dii - dxi * dxi ) * det1;
					    final double hiyi = ( dxi * dxy - dxx * dyi ) * det1;
					    final double hiii = ( dxx * dyy - dxy * dxy ) * det1;

					    // localize
					    ox = -hixx * dx - hixy * dy - hixi * di;
					    oy = -hixy * dx - hiyy * dy - hiyi * di;
					    oi = -hixi * dx - hiyi * dy - hiii * di;


					    final double odc = ox * ox + oy * oy + oi * oi;

					    if ( odc < 2.0f )
					    {
					    	if ( ( Math.abs( ox ) > 0.5 || Math.abs( oy ) > 0.5 || Math.abs( oi ) > 0.5 ) && odc < od )
						    {
						    	od = odc;

						    	xc = ( int )Math.round( xc + ox );
						    	yc = ( int )Math.round( yc + oy );
						    	ic = ( int )Math.round( ic + oi );

						    	if ( xc < 1 || yc < 1 || ic < 1 || xc > d[ 0 ].width - 2 || yc > d[ 0 ].height - 2 || ic > d.length - 2 )
						    		isLocalizable = false;
						    	else
						    	{
						    		xa = xc - 1;
						    		xb = xc + 1;
						    		rc = yc * d[ ic ].width;
						    		rac = rc - d[ ic ].width;
						    		rbc = rc + d[ ic ].width;
						    		iac = ic - 1;
						    		ibc = ic + 1;

						    		e000 = d[ iac ].data[ rac + xa ];
						    		e100 = d[ iac ].data[ rac + xc ];
						    		e200 = d[ iac ].data[ rac + xb ];

									e010 = d[ iac ].data[ rc + xa ];
									e110 = d[ iac ].data[ rc + xc ];
									e210 = d[ iac ].data[ rc + xb ];

									e020 = d[ iac ].data[ rbc + xa ];
									e120 = d[ iac ].data[ rbc + xc ];
									e220 = d[ iac ].data[ rbc + xb ];


									e001 = d[ ic ].data[ rac + xa ];
									e101 = d[ ic ].data[ rac + xc ];
									e201 = d[ ic ].data[ rac + xb ];

									e011 = d[ ic ].data[ rc + xa ];
									e111 = d[ ic ].data[ rc + xc ];
									e211 = d[ ic ].data[ rc + xb ];

									e021 = d[ ic ].data[ rbc + xa ];
									e121 = d[ ic ].data[ rbc + xc ];
									e221 = d[ ic ].data[ rbc + xb ];


									e002 = d[ ibc ].data[ rac + xa ];
									e102 = d[ ibc ].data[ rac + xc ];
									e202 = d[ ibc ].data[ rac + xb ];

									e012 = d[ ibc ].data[ rc + xa ];
									e112 = d[ ibc ].data[ rc + xc ];
									e212 = d[ ibc ].data[ rc + xb ];

									e022 = d[ ibc ].data[ rbc + xa ];
									e122 = d[ ibc ].data[ rbc + xc ];
									e222 = d[ ibc ].data[ rbc + xb ];
						    	}
						    }
						    else
						    {
						    	fx = xc + ox;
						    	fy = yc + oy;
						    	fi = ic + oi;

						    	if ( fx < 0 || fy < 0 || fi < 0 || fx > d[ 0 ].width - 1 || fy > d[ 0 ].height - 1 || fi > d.length - 1 )
						    		isLocalizable = false;
						    	else
						    		isLocalized = true;
						    }
					    }
					    else isLocalizable = false;
					}
					while ( !isLocalized && isLocalizable && t >= 0 );
				    // reject detections that could not be localized properly

					if ( !isLocalized )
					{
//						System.err.println( "Localization failed (x: " + xc + ", y: " + yc + ", i: " + ic + ") => (ox: " + ox + ", oy: " + oy + ", oi: " + oi + ")" );
//						if ( ic < 1 || ic > d.length - 2 )
//							System.err.println( "  Detection outside octave." );
						continue;
					}

					// reject detections with very low contrast

					if ( Math.abs( e111 + 0.5f * ( dx * ox + dy * oy + di * oi ) ) < MIN_CONTRAST ) continue;

					// reject edge responses

					final double det = dxx * dyy - dxy * dxy;
				    final double trace = dxx + dyy;
				    if ( trace * trace / det > MAX_CURVATURE_RATIO ) continue;

				    candidates.addElement( new double[]{ fx, fy, fi } );
					//candidates.addElement( new double[]{ x, y, i } );
				}
			}
		}
	}
}
