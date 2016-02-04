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
package mpicbg.imagefeatures;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import mpicbg.models.AbstractModel;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import mpicbg.util.Util;

/**
 * This implementation actually uses the DoG-detector as described by
 * \cite{Lowe04} and extracts local intensity patches from coarser scales
 * similar to what was described by \cite{BrownAl05} as Multi-Scale Oriented
 * Patches.  The intensities of the patches are enhanced such that each patch
 * uses the full [0.0--1.0] range.
 *
 *
 * BibTeX:
 * <pre>
 * &#64;inproceedings{BrownAl05,
 *   author    = {Matthew Brown and Richard Szeliski and Simon Winder},
 *   title     = {Multi-Image Matching Using Multi-Scale Oriented Patches},
 *   booktitle = {CVPR '05: Proceedings of the 2005 IEEE Computer Society Conference on Computer Vision and Pattern Recognition (CVPR'05) - Volume 1},
 *   year      = {2005},
 *   isbn      = {0-7695-2372-2},
 *   pages     = {510--517},
 *   publisher = {IEEE Computer Society},
 *   address   = {Washington, DC, USA},
 *   doi       = {http://dx.doi.org/10.1109/CVPR.2005.235},
 *   url       = {http://www.cs.ubc.ca/~mbrown/papers/cvpr05.pdf},
 * }
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
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 */
public class FloatArray2DMOPS extends FloatArray2DFeatureTransform< FloatArray2DMOPS.Param >
{
	final static public class Param
	{
		/**
		 * Width of the feature descriptor square in samples.
		 */
		public int fdSize = 16;
		public int maxOctaveSize = 1024;
		public int minOctaveSize = 64;

		public int steps = 3;
		public float initialSigma = 1.6f;
	}

	private float[] sigma;
	private float[] sigma_diff;
	private float[][] kernel_diff;

	final int O_SCALE = 4;
	final int O_SCALE_LD2 = 2;

	/**
	 * Returns the size in bytes of a Feature object.
	 */
	public long getFeatureObjectSize()
	{
		return FloatArray2DMOPS.getFeatureObjectSize( p.fdSize );
	}

	static public long getFeatureObjectSize( final int fdsize )
	{
		return fdsize * fdsize * 4 + 32 + 32;
	}


	/**
	 * octaved scale space
	 */
	private FloatArray2DScaleOctave[] octaves;
	public FloatArray2DScaleOctave[] getOctaves(){ return octaves; }
	public FloatArray2DScaleOctave getOctave( final int i ){ return octaves[ i ]; }

	/**
	 * Difference of Gaussian detector
	 */
	private final FloatArray2DScaleOctaveDoGDetector dog;

	/**
	 * Constructor
	 *
	 * @param feature_descriptor_size
	 * @param feature_descriptor_size
	 */
	public FloatArray2DMOPS(
			final Param p )
	{
		super( p );
		octaves = null;
		dog = new FloatArray2DScaleOctaveDoGDetector();

		sigma = new float[ p.steps + 3 ];
		sigma[ 0 ] = p.initialSigma;
		sigma_diff = new float[ p.steps + 3 ];
		sigma_diff[ 0 ] = 0.0f;
		kernel_diff = new float[ p.steps + 3 ][];

		for ( int i = 1; i < p.steps + 3; ++i )
		{
			sigma[ i ] = ( float )( p.initialSigma * Math.pow( 2.0, ( double )i / ( double )p.steps ) );
			sigma_diff[ i ] = ( float )Math.sqrt( sigma[ i ] * sigma[ i ] - p.initialSigma * p.initialSigma );

			kernel_diff[ i ] = Filter.createGaussianKernel( sigma_diff[ i ], true );
		}
	}

	/**
	 * initialize the scale space as a scale pyramid having octave stubs only
	 *
	 * @param src image having a generating gaussian kernel of initial_sigma
	 * 	 img must be a 2d-array of float values in range [0.0f, ..., 1.0f]
	 */
	@Override
	public void init( FloatArray2D src )
	{
		// estimate the number of octaves needed using a simple while loop instead of ld
		int o = 0;
		double w = src.width;
		double h = src.height;
		final int minOctaveSize = p.minOctaveSize / 4;
		final int max_kernel_size = kernel_diff[ p.steps + 2 ].length;
		while ( w > Math.max( max_kernel_size, minOctaveSize - 1 ) && h > Math.max( max_kernel_size, minOctaveSize - 1 ) )
		{
			w /= 2.0f;
			h /= 2.0f;
			++o;
		}
		octaves = new FloatArray2DScaleOctave[ o ];



		FloatArray2D next;

		for ( int i = 0; i < octaves.length; ++i )
		{
			octaves[ i ] = new FloatArray2DScaleOctave(
					src,
					sigma,
					sigma_diff,
					kernel_diff );
			octaves[ i ].buildStub();
			next = new FloatArray2D(
					src.width / 2 + src.width % 2,
					src.height / 2 + src.height % 2 );
			FloatArray2DScaleOctave.downsample( octaves[ i ].getL( 1 ), next );
			if ( src.width > p.maxOctaveSize || src.height > p.maxOctaveSize )
				octaves[ i ].clear();
			src = next;
		}
	}

	// TODO this is for test
	//---------------------------------------------------------------------
	public FloatArray2D pattern;


	/**
	 * sample the scaled and rotated gradients in a region around the
	 * features location, the regions size is defined by
	 * FEATURE_DESCRIPTOR_WIDTH^2
	 *
	 * @param c candidate 0=>x, 1=>y, 2=>scale index
	 * @param o octave index
	 * @param octave_sigma sigma of the corresponding gaussian kernel with
	 *   respect to the scale octave
	 * @param orientation orientation [-&pi; ... &pi;]
	 */
	private float[] createDescriptor(
			final double[] c,
			final int o,
			final double octave_sigma,
			final double orientation )
	{
		// select the octave with SCALE*octave_sigma = octaves[ o+SCALE_LD2 ]
		final FloatArray2DScaleOctave octave = octaves[ o + O_SCALE_LD2 ];
		final FloatArray2D l = octave.getL( ( int )Math.round( c[ 2 ] ) );

		final float[] desc = new float[ p.fdSize * p.fdSize ];

		final double cos_o = Math.cos( orientation );
		final double sin_o = Math.sin( orientation );

		// TODO this is for test
		//---------------------------------------------------------------------
		//pattern = new FloatArray2D( p.fdSize, p.fdSize );

		int i = 0;
		float max = Float.MIN_VALUE;
		float min = Float.MAX_VALUE;

		//! sample the region arround the keypoint location
		for ( int y = p.fdSize - 1; y >= 0; --y )
		{
			final double ys =
				( ( double )y - ( double )p.fdSize / 2.0 + 0.5 ) * octave_sigma; //!< scale y around 0,0
			for ( int x = p.fdSize - 1; x >= 0; --x )
			{
				final double xs =
					( ( double )x - ( double )p.fdSize / 2.0 + 0.5 ) * octave_sigma; //!< scale x around 0,0
				final double yr = cos_o * ys + sin_o * xs; //!< rotate y around 0,0
				final double xr = cos_o * xs - sin_o * ys; //!< rotate x around 0,0

				// translate ys to sample y position in the gradient image
				final int yg = Util.pingPong(
						( int )( Math.round( yr + c[ 1 ] / O_SCALE ) ),
						l.height );

				// translate xs to sample x position in the gradient image
				final int xg = Util.pingPong(
						( int )( Math.round( xr + c[ 0 ] / O_SCALE ) ),
						l.width );

				desc[ i ] = l.get( xg, yg );

				// TODO this is for test
				//---------------------------------------------------------------------
				//pattern.set( desc[ i ], x, y );

				if ( desc[ i ] > max ) max = desc[ i ];
				else if ( desc[ i ] < min ) min = desc[ i ];
				++i;
			}
		}

		// normalize
		final float n = max - min;
		for ( i = 0; i < desc.length; ++i )
			desc[ i ] = ( desc[ i ] - min ) / n;

		return desc;
	}

	/**
	 * assign orientation to the given candidate, if more than one orientations
	 * found, duplicate the feature for each orientation
	 *
	 * estimate the feature descriptor for each of those candidates
	 *
	 * @param c candidate 0=>x, 1=>y, 2=>scale index
	 * @param o octave index
	 * @param features finally contains all processed candidates
	 */
	void processCandidate(
			final double[] c,
			final int o,
			final List< Feature > features )
	{
		final int ORIENTATION_BINS = 36;
		final int ORIENTATION_BINS1 = ORIENTATION_BINS - 1;
		final double ORIENTATION_BIN_SIZE = 2.0 * Math.PI / ORIENTATION_BINS;
		final float[] histogram_bins = new float[ ORIENTATION_BINS ];

		final int scale = 1 << o;

		final FloatArray2DScaleOctave octave = octaves[ o ];

		final double octave_sigma = octave.SIGMA[ 0 ] * Math.pow( 2.0, c[ 2 ] / ( double )octave.STEPS );

		// create a circular gaussian window with sigma 1.5 times that of the feature
		final FloatArray2D gaussianMask =
			Filter.createGaussianKernelOffset(
					octave_sigma * 1.5,
					c[ 0 ] - Math.floor( c[ 0 ] ),
					c[ 1 ] - Math.floor( c[ 1 ] ),
					false );
		//FloatArrayToImagePlus( gaussianMask, "gaussianMask", 0, 0 ).show();

		// get the gradients in a region arround the keypoints location
		final FloatArray2D[] src = octave.getL1( ( int )Math.round( c[ 2 ] ) );
		final FloatArray2D[] gradientROI = new FloatArray2D[ 2 ];
		gradientROI[ 0 ] = new FloatArray2D( gaussianMask.width, gaussianMask.width );
		gradientROI[ 1 ] = new FloatArray2D( gaussianMask.width, gaussianMask.width );

		final int half_size = gaussianMask.width / 2;
		int n = gaussianMask.width * gaussianMask.width - 1;
		for ( int yi = gaussianMask.width - 1; yi >= 0; --yi )
		{
			final int ra_y = src[ 0 ].width * Math.max( 0, Math.min( src[ 0 ].height - 1, ( int )c[ 1 ] + yi - half_size ) );
			final int ra_x = ra_y + Math.min( ( int )c[ 0 ], src[ 0 ].width - 1 );

			for ( int xi = gaussianMask.width - 1; xi >= 0; --xi )
			{
				final int pt = Math.max( ra_y, Math.min( ra_y + src[ 0 ].width - 2, ra_x + xi - half_size ) );
				gradientROI[ 0 ].data[ n ] = src[ 0 ].data[ pt ];
				gradientROI[ 1 ].data[ n ] = src[ 1 ].data[ pt ];
				--n;
			}
		}

		// and mask this region with the precalculated gaussion window
		for ( int i = 0; i < gradientROI[ 0 ].data.length; ++i )
		{
			gradientROI[ 0 ].data[ i ] *= gaussianMask.data[ i ];
		}

		// TODO this is for test
		//---------------------------------------------------------------------
		//ImageArrayConverter.FloatArrayToImagePlus( gradientROI[ 0 ], "gaussianMaskedGradientROI", 0, 0 ).show();
		//ImageArrayConverter.FloatArrayToImagePlus( gradientROI[ 1 ], "gaussianMaskedGradientROI", 0, 0 ).show();

		// build an orientation histogram of the region
		for ( int i = 0; i < gradientROI[ 0 ].data.length; ++i )
		{
			final int bin = Math.max( 0, Math.min( ORIENTATION_BINS1, ( int )( ( gradientROI[ 1 ].data[ i ] + Math.PI ) / ORIENTATION_BIN_SIZE ) ) );
			histogram_bins[ bin ] += gradientROI[ 0 ].data[ i ];
		}

		// find the dominant orientation and interpolate it with respect to its two neighbours
		int max_i = 0;
		for ( int i = 0; i < ORIENTATION_BINS; ++i )
		{
			if ( histogram_bins[ i ] > histogram_bins[ max_i ] ) max_i = i;
		}

		/*
		 * Interpolate orientation.
		 * Estimate the offset from center of the
		 * parabolic extremum of the taylor series through env[1], derivatives
		 * via central difference and laplace.
		 */
		double e0 = histogram_bins[ ( max_i + ORIENTATION_BINS - 1 ) % ORIENTATION_BINS ];
		double e1 = histogram_bins[ max_i ];
		double e2 = histogram_bins[ ( max_i + 1 ) % ORIENTATION_BINS ];
		double offset = ( e0 - e2 ) / 2.0 / ( e0 - 2.0 * e1 + e2 );
		double orientation = ( max_i + offset ) * ORIENTATION_BIN_SIZE - Math.PI;

		// assign descriptor and add the Feature instance to the collection
		features.add(
				new Feature(
						octave_sigma * scale,
						orientation,
						new double[]{ c[ 0 ] * scale, c[ 1 ] * scale },
						//new double[]{ ( c[ 0 ] + 0.5f ) * scale - 0.5f, ( c[ 1 ] + 0.5f ) * scale - 0.5f },
						createDescriptor( c, o, octave_sigma, orientation ) ) );

		// TODO this is for test
		//---------------------------------------------------------------------
		//ImageArrayConverter.FloatArrayToImagePlus( pattern, "test", 0f, 1.0f ).show();

		/**
		 * check if there is another significant orientation ( > 80% max )
		 * if there is one, duplicate the feature and
		 */
		for ( int i = 0; i < ORIENTATION_BINS; ++i )
		{
			if (
					i != max_i &&
					( max_i + 1 ) % ORIENTATION_BINS != i &&
					( max_i - 1 + ORIENTATION_BINS ) % ORIENTATION_BINS != i &&
					histogram_bins[ i ] > 0.8 * histogram_bins[ max_i ] )
			{
				/**
				 * interpolate orientation estimate the offset from center of
				 * the parabolic extremum of the taylor series through env[1],
				 * derivatives via central difference and laplace
				 */
				e0 = histogram_bins[ ( i + ORIENTATION_BINS - 1 ) % ORIENTATION_BINS ];
				e1 = histogram_bins[ i ];
				e2 = histogram_bins[ ( i + 1 ) % ORIENTATION_BINS ];

				if ( e0 < e1 && e2 < e1 )
				{
					offset = ( e0 - e2 ) / 2.0f / ( e0 - 2.0f * e1 + e2 );
					orientation = ( i + 0.5 + offset ) * ORIENTATION_BIN_SIZE - Math.PI;

					features.add(
							new Feature(
									octave_sigma * scale,
									orientation,
									new double[]{ c[ 0 ] * scale, c[ 1 ] * scale },
									//new double[]{ ( c[ 0 ] + 0.5f ) * scale - 0.5f, ( c[ 1 ] + 0.5f ) * scale - 0.5f },
									createDescriptor( c, o, octave_sigma, orientation ) ) );

					// TODO this is for test
					//---------------------------------------------------------------------
					//ImageArrayConverter.FloatArrayToImagePlus( pattern, "test", 0f, 1.0f ).show();
				}
			}
		}
		return;
	}


	/**
	 * detect features in the specified scale octave
	 *
	 * @param o octave index
	 *
	 * @return detected features
	 */
	public List< Feature > runOctave( final int o )
	{
		final List< Feature > features = new ArrayList< Feature >();
		final FloatArray2DScaleOctave octave = octaves[ o ];
		octave.build();
		dog.run( octave );
		final List< double[] > candidates = dog.getCandidates();
		for ( final double[] c : candidates )
		{
			this.processCandidate( c, o, features );
		}
		//System.out.println( features.size() + " candidates processed in octave " + o );

		return features;
	}

	/**
	 * Detect features in all scale octaves.
	 *
	 * Note that there are O_SCALE_LD2 more octaves needed for descriptor extraction.
	 *
	 * @return detected features
	 */
	public List< Feature > run()
	{
		final List< Feature > features = new ArrayList< Feature >();
		for ( int o = 0; o < octaves.length; ++o )
		{
			if ( octaves[ o ].state == FloatArray2DScaleOctave.State.EMPTY ) continue;
			octaves[ o ].build();
		}
		for ( int o = 0; o < octaves.length - O_SCALE_LD2; ++o )
		{
			if ( octaves[ o ].state == FloatArray2DScaleOctave.State.EMPTY ) continue;
			final List< Feature > more = runOctave( o );
			features.addAll( more );
		}
		return features;
	}

	/**
	 * Detect features in all scale octaves.
	 *
	 * Note that there are O_SCALE_LD2 more octaves needed for descriptor extraction.
	 *
	 * @return detected features
	 */
	public List< Feature > run( final int max_size )
	{
		final List< Feature > features = new ArrayList< Feature >();
		for ( int o = 0; o < octaves.length; ++o )
			if ( octaves[ o ].width <= max_size && octaves[ o ].height <= max_size )
				octaves[ o ].build();
		for ( int o = 0; o < octaves.length - O_SCALE_LD2; ++o )
			if ( octaves[ o ].width <= max_size && octaves[ o ].height <= max_size )
			{
				final List< Feature > more = runOctave( o );
				features.addAll( more );
			}

		//System.out.println( features.size() + " candidates processed in all octaves" );
		return features;
	}

	/**
	 * Identify corresponding features
	 *
	 * @param fs1 feature collection from set 1
	 * @param fs2 feature collection from set 2
	 * @param rod Ratio of distances (closest/next closest match)
	 *
	 * @return matches
	 */
	public static List< PointMatch > createMatches(
			final List< Feature > fs1,
			final List< Feature > fs2,
			final float rod )
	{
		final List< PointMatch > matches = new ArrayList< PointMatch >();

		for ( final Feature f1 : fs1 )
		{
			Feature best = null;
			double best_d = Double.MAX_VALUE;
			double second_best_d = Double.MAX_VALUE;

			for ( final Feature f2 : fs2 )
			{
				final double d = f1.descriptorDistance( f2 );
				//System.out.println( d );
				if ( d < best_d )
				{
					second_best_d = best_d;
					best_d = d;
					best = f2;
				}
				else if ( d < second_best_d )
					second_best_d = d;
			}
			if ( best != null && second_best_d < Double.MAX_VALUE && best_d / second_best_d < rod )
			//if ( best != null )
				matches.add(
						new PointMatch(
								new Point(
										new double[] { f1.location[ 0 ], f1.location[ 1 ] } ),
								new Point(
										new double[] { best.location[ 0 ], best.location[ 1 ] } ),
								( f1.scale + best.scale ) / 2.0f ) );
//			else
//				System.out.println( "No match found." );
		}

		// now remove ambiguous matches
		for ( int i = 0; i < matches.size(); )
		{
			boolean amb = false;
			final PointMatch m = matches.get( i );
			final double[] m_p2 = m.getP2().getL();
			for ( int j = i + 1; j < matches.size(); )
			{
				final PointMatch n = matches.get( j );
				final double[] n_p2 = n.getP2().getL();
				if ( m_p2[ 0 ] == n_p2[ 0 ] && m_p2[ 1 ] == n_p2[ 1 ] )
				{
					amb = true;
					matches.remove( j );
				}
				else ++j;
			}
			if ( amb )
				matches.remove( i );
			else ++i;
		}
		return matches;
	}

	/**
	 * Identify corresponding features.
	 * Fill a HashMap that stores the Features for each positive PointMatch.
	 *
	 * @param fs1 feature collection from set 1
	 * @param fs2 feature collection from set 2
	 * @param rod Ratio of distances (closest/next closest match)
	 *
	 * @return matches
	 */
	public static List< PointMatch > createMatches(
			final List< Feature > fs1,
			final List< Feature > fs2,
			final double rod,
			final HashMap< Point, Feature > m1,
			final HashMap< Point, Feature > m2 )
	{
		final List< PointMatch > matches = new ArrayList< PointMatch >();

		for ( final Feature f1 : fs1 )
		{
			Feature best = null;
			double best_d = Double.MAX_VALUE;
			double second_best_d = Double.MAX_VALUE;

			for ( final Feature f2 : fs2 )
			{
				final double d = f1.descriptorDistance( f2 );
				//System.out.println( d );
				if ( d < best_d )
				{
					second_best_d = best_d;
					best_d = d;
					best = f2;
				}
				else if ( d < second_best_d )
					second_best_d = d;
			}
			if ( best != null && second_best_d < Double.MAX_VALUE && best_d / second_best_d < rod )
			//if ( best != null )
			{
				final Point p1 = new Point( new double[] { f1.location[ 0 ], f1.location[ 1 ] } );
				final Point p2 = new Point( new double[] { best.location[ 0 ], best.location[ 1 ] } );

				matches.add(	new PointMatch( p1,	p2,	( f1.scale + best.scale ) / 2.0f ) );

				m1.put( p1, f1 );
				m2.put( p2, best );
			}
//			else
//				System.out.println( "No match found." );
		}

		// now remove ambiguous matches
		for ( int i = 0; i < matches.size(); )
		{
			boolean amb = false;
			final PointMatch m = matches.get( i );
			final double[] m_p2 = m.getP2().getL();
			for ( int j = i + 1; j < matches.size(); )
			{
				final PointMatch n = matches.get( j );
				final double[] n_p2 = n.getP2().getL();
				if ( m_p2[ 0 ] == n_p2[ 0 ] && m_p2[ 1 ] == n_p2[ 1 ] )
				{
					m1.remove( n.getP1() );
					m2.remove( n.getP2() );
					amb = true;
					matches.remove( j );
				}
				else ++j;
			}
			if ( amb )
			{
				matches.remove( i );
				m1.remove( m.getP1() );
				m2.remove( m.getP2() );
			}
			else ++i;
		}
		return matches;
	}


	/**
	 * identify corresponding features using spatial constraints
	 *
	 * @param fs1 feature collection from set 1 sorted by decreasing size
	 * @param fs2 feature collection from set 2 sorted by decreasing size
	 * @param max_sd maximal difference in size (ratio max/min)
	 * @param model transformation model to be applied to fs2
	 * @param max_id maximal distance in image space ($\sqrt{x^2+y^2}$)
	 * @param rod Ratio of distances (closest/next closest match)
	 *
	 * @return matches
	 *
	 * TODO implement the spatial constraints
	 */
	public static List< PointMatch > createMatches(
			final List< Feature > fs1,
			final List< Feature > fs2,
			final double max_sd,
			final AbstractModel< ? > model,
			final double max_id,
			final double rod )
	{
		final List< PointMatch > matches = new ArrayList< PointMatch >();
		final double min_sd = 1.0 / max_sd;

		final int size = fs2.size();
		final int size_1 = size - 1;

		for ( final Feature f1 : fs1 )
		{
			Feature best = null;
			double best_d = Double.MAX_VALUE;
			double second_best_d = Double.MAX_VALUE;

			int first = 0;
			int last = size_1;
			int s = size / 2 + size % 2;
			if ( max_sd < Double.MAX_VALUE )
			{
				while ( s > 1 )
				{
					Feature f2 = fs2.get( last );
					if ( f2.scale / f1.scale < min_sd ) last = Math.max( 0, last - s );
					else last = Math.min( size_1, last + s );
					f2 = fs2.get( first );
					if ( f2.scale / f1.scale < max_sd ) first = Math.max( 0, first - s );
					else first = Math.min( size_1, first + s );
					s = s / 2 + s % 2;
				}
				//System.out.println( "first = " + first + ", last = " + last + ", first.scale = " + fs2.get( first ).scale + ", last.scale = " + fs2.get( last ).scale + ", this.scale = " + f1.scale );
			}

			//for ( Feature f2 : fs2 )

			for ( int i = first; i <= last; ++i )
			{
				final Feature f2 = fs2.get( i );
				final double d = f1.descriptorDistance( f2 );
				if ( d < best_d )
				{
					second_best_d = best_d;
					best_d = d;
					best = f2;
				}
				else if ( d < second_best_d )
					second_best_d = d;
			}
			if ( best != null && second_best_d < Double.MAX_VALUE && best_d / second_best_d < rod )
				// not weighted
//				matches.addElement(
//						new PointMatch(
//								new Point(
//										new double[] { f1.location[ 0 ], f1.location[ 1 ] } ),
//								new Point(
//										new double[] { best.location[ 0 ], best.location[ 1 ] } ) ) );
				// weighted with the features scale
				matches.add(
						new PointMatch(
								new Point(
										new double[] { f1.location[ 0 ], f1.location[ 1 ] } ),
								new Point(
										new double[] { best.location[ 0 ], best.location[ 1 ] } ),
								( f1.scale + best.scale ) / 2.0f ) );
		}
		// now remove ambiguous matches
		for ( int i = 0; i < matches.size(); )
		{
			boolean amb = false;
			final PointMatch m = matches.get( i );
			final double[] m_p2 = m.getP2().getL();
			for ( int j = i + 1; j < matches.size(); )
			{
				final PointMatch n = matches.get( j );
				final double[] n_p2 = n.getP2().getL();
				if ( m_p2[ 0 ] == n_p2[ 0 ] && m_p2[ 1 ] == n_p2[ 1 ] )
				{
					amb = true;
					//System.out.println( "removing ambiguous match at " + j );
					matches.remove( j );
				}
				else ++j;
			}
			if ( amb )
			{
				//System.out.println( "removing ambiguous match at " + i );
				matches.remove( i );
			}
			else ++i;
		}
		return matches;
	}

	@Override
	final public void extractFeatures( final Collection< Feature > features )
	{
		features.addAll( run( p.maxOctaveSize ) );
	}

	/**
	 * get a histogram of feature sizes
	 *
	 * @param rs
	 */
	public static double[] featureSizeHistogram(
			final List< Feature > features,
			final double min,
			final double max,
			final int bins )
	{
		System.out.print( "estimating feature size histogram ..." );
		final int num_features = features.size();
		final double h[] = new double[ bins ];
		final int hb[] = new int[ bins ];

		for ( final Feature f : features )
		{
			final int bin = ( int )Math.max( 0, Math.min( bins - 1, ( int )( Math.log( f.scale ) / Math.log( 2.0 ) * 28.0f ) ) );
			++hb[ bin ];
		}
		for ( int i = 0; i < bins; ++i )
		{
			h[ i ] = ( double )hb[ i ] / ( double )num_features;
		}
		System.out.println( " done" );
		return h;
	}

	final public float getInitialSigma(){ return p.initialSigma; }
	final public void setInitialSigma( final float initialSigma )
	{
		p.initialSigma = initialSigma;
		sigma = new float[ p.steps + 3 ];
		sigma[ 0 ] = p.initialSigma;
		sigma_diff = new float[ p.steps + 3 ];
		sigma_diff[ 0 ] = 0.0f;
		kernel_diff = new float[ p.steps + 3 ][];

		for ( int i = 1; i < p.steps + 3; ++i )
		{
			sigma[ i ] = ( float )( p.initialSigma * Math.pow( 2.0f, ( double )i / ( double )p.steps ) );
			sigma_diff[ i ] = ( float )Math.sqrt( sigma[ i ] * sigma[ i ] - p.initialSigma * p.initialSigma );

			kernel_diff[ i ] = Filter.createGaussianKernel( sigma_diff[ i ], true );
		}
	}

	final public int getMaxOctaveSize(){ return p.maxOctaveSize; }
}
