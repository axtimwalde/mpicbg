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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import mpicbg.models.AbstractModel;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import mpicbg.util.Util;

/**
 * Scale Invariant Feature Transform as described by David Lowe \cite{Loew04}.
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
 * NOTE:
 * The SIFT-method is protected by U.S. Patent 6,711,293: "Method and
 * apparatus for identifying scale invariant features in an image and use of
 * same for locating an object in an image" by the University of British
 * Columbia.  That is, for commercial applications the permission of the author
 * is required.
 *
 * @author Stephan Saalfeld &lt;saalfeld@mpi-cbg.de&gt;
 * @version 0.1b
 */
public class FloatArray2DSIFT extends FloatArray2DFeatureTransform< FloatArray2DSIFT.Param >
{
	static public class Param implements Serializable
	{
		/**
		 * Feature descriptor size.
		 *    How many samples per row and column
		 */
		public int fdSize = 4;

		/**
		 * Feature descriptor orientation bins
		 *    How many bins per local histogram
		 */
		public int fdBins = 8;

		/**
		 * Size limits for scale octaves in px:
		 * {@code minOctaveSize < octave < maxOctaveSize}
		 */
		public int maxOctaveSize = 1024;
		public int minOctaveSize = 64;

		/**
		 * Steps per Scale Octave
		 */
		public int steps = 3;

		/**
		 * Initial sigma of each Scale Octave
		 */
		public float initialSigma = 1.6f;

		public boolean equals( final Param p )
		{
			return
				( fdSize == p.fdSize ) &&
				( fdBins == p.fdBins ) &&
				( maxOctaveSize == p.maxOctaveSize ) &&
				( minOctaveSize == p.minOctaveSize ) &&
				( steps == p.steps ) &&
				( initialSigma == p.initialSigma );
		}

		@Override
		public boolean equals( final Object p )
		{
			if ( getClass().isInstance( p ) )
				return equals( ( Param )p );
			else
				return false;
		}

		@Override
		public Param clone()
		{
			final Param s = new Param();
			s.fdBins = fdBins;
			s.fdSize = fdSize;
			s.initialSigma = initialSigma;
			s.maxOctaveSize = maxOctaveSize;
			s.minOctaveSize = minOctaveSize;
			s.steps = steps;

			return s;
		}

		public void set( final Param p )
		{
			fdBins = p.fdBins;
			fdSize = p.fdSize;
			initialSigma = p.initialSigma;
			maxOctaveSize = p.maxOctaveSize;
			minOctaveSize = p.minOctaveSize;
			steps = p.steps;
		}
	}

	final private int fdWidth;
	final private float fdBinWidth;

	private float[] sigma;
	private float[] sigma_diff;
	private float[][] kernel_diff;

	/** per-candidate scratch buffers (a FloatArray2DSIFT instance is not thread-safe anyway) */
	final private OrientationHistogram orientationHistogram = new OrientationHistogram();
	final private float[] gaussian;

	/** descriptor scratch: sampled derivatives, magnitude, orientation, histogram  */
	final private float[] descDx;
	final private float[] descDy;
	final private float[] gradMag;
	final private float[] gradOri;
	final private float[] hist;

	// Top and bottom histogram bins for each sample, and the weights for interpolating
	final private int[] descBinB;
	final private int[] descBinT;
	final private float[] descW0;
	final private float[] descW1;

	final static private int ORIENTATION_BINS = OrientationHistogram.BINS;
	final static private double ORIENTATION_BIN_SIZE = OrientationHistogram.BIN_SIZE;


	/**
	 * octaved scale space
	 */
	private FloatArray2DScaleOctave[] octaves;
	public FloatArray2DScaleOctave[] getOctaves()
	{
		return octaves;
	}
	public FloatArray2DScaleOctave getOctave( final int i )
	{
		return octaves[ i ];
	}

	/**
	 * Difference of Gaussian detector
	 */
	private final FloatArray2DScaleOctaveDoGDetector dog;

	/**
	 * Constructor
	 */
	public FloatArray2DSIFT( final Param p )
	{
		super( p );
		octaves = null;
		dog = new FloatArray2DScaleOctaveDoGDetector();

		fdWidth = 4 * p.fdSize;
		fdBinWidth = 2.0f * ( float )Math.PI / ( float )p.fdBins;

		// Allocate scratch buffers for the feature descriptor
		descDx = new float[fdWidth * fdWidth];
		descDy = new float[fdWidth * fdWidth];
		gradMag = new float[fdWidth * fdWidth];
		gradOri = new float[fdWidth * fdWidth];

		descBinB = new int[fdWidth * fdWidth];
		descBinT = new int[fdWidth * fdWidth];
		descW0 = new float[fdWidth * fdWidth];
		descW1 = new float[fdWidth * fdWidth];

		hist = new float[p.fdSize * p.fdSize * p.fdBins];
		gaussian = new float[fdWidth * fdWidth];

		// Create a (separable) gaussian mask for the feature descriptor square
		final float[] wx = new float[2 * p.fdSize];
		final float spread = p.fdSize * p.fdSize * 8;
		for (int x = p.fdSize * 2 - 1; x >= 0; --x) {
			final float fx = (float)x + 0.5f;
			wx[x] = (float)Math.exp(-(fx * fx) / spread);
		}

		for (int y = p.fdSize * 2 - 1; y >= 0; --y) {
			final float fy = (float)y + 0.5f;
			final float wy = (float)Math.exp(-(fy * fy) / spread);
			final int iy = 2 * p.fdSize - 1 - y;
			final int jy = 2 * p.fdSize + y;

			for (int x = p.fdSize * 2 - 1; x >= 0; --x) {
				final float val = wx[x] * wy;
				final int ix = 2 * p.fdSize - 1 - x;
				final int jx = 2 * p.fdSize + x;
				gaussian[ iy * fdWidth + ix ] = val;
				gaussian[ jy * fdWidth + ix ] = val;
				gaussian[ iy * fdWidth + jx ] = val;
				gaussian[ jy * fdWidth + jx ] = val;
			}
		}

		setInitialSigma( p.initialSigma );
	}

	/**
	 * initialize the scale space as a scale pyramid having octave stubs only
	 *
	 * @param src image having a generating gaussian kernel of
	 * 	{@link Param#initialSigma} img must be a 2d-array of float
	 *  values in range [0.0f, ..., 1.0f]
	 */
	@Override
	final public void init( FloatArray2D src )
	{
		// estimate the number of octaves needed using a simple while loop instead of ld
		int o = 0;
		float w = ( float )src.width;
		float h = ( float )src.height;
		final int max_kernel_size = kernel_diff[ p.steps + 2 ].length;
		while ( w > Math.max( max_kernel_size, p.minOctaveSize - 1 ) && h > Math.max( max_kernel_size, p.minOctaveSize - 1 ) )
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

	/**
	 * Wrap a histogram bin into [ 0, bins ) like repeated {@code += bins} / {@code -= bins} would; valid
	 * for bins in [ -3 * bins, 4 * bins ), and branch-free so that it vectorizes.
	 */
	private static int wrapBin(int b, final int bins) {
		b += bins & (b >> 31);
		b += bins & (b >> 31);
		b += bins & (b >> 31);
		b -= bins & ((bins - 1 - b) >> 31);
		b -= bins & ((bins - 1 - b) >> 31);
		b -= bins & ((bins - 1 - b) >> 31);
		return b;
	}

	/**
	 * sample the scaled and rotated gradients in a region around the
	 * features location, the regions size is defined by
	 * ( FEATURE_DESCRIPTOR_SIZE * 4 )^2 ( 4x4 subregions )
	 *
	 * @param c candidate {@code 0=>x, 1=>y, 2=>scale index}
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
		final FloatArray2DScaleOctave octave = octaves[ o ];
		final FloatArray2DScaleOctave.Gradients gradients = octave.getGradients((int)Math.round(c[2]));
		final double cos_o = Math.cos( orientation );
		final double sin_o = Math.sin( orientation );

		// TODO this is for test
		//---------------------------------------------------------------------
		//FloatArray2D image = octave.getL( Math.round( c[ 2 ] ) );
		//pattern = new FloatArray2D( FEATURE_DESCRIPTOR_WIDTH, FEATURE_DESCRIPTOR_WIDTH );

		//! sample the region around the keypoint location: gather the derivatives first, then weigh and
		//! rotate them in flat passes over the sample arrays (the magnitude pass vectorizes)
		for ( int y = fdWidth - 1; y >= 0; --y )
		{
			final double ys =
				( y - 2.0 * p.fdSize + 0.5 ) * octave_sigma; //!< scale y around 0,0
			for ( int x = fdWidth - 1; x >= 0; --x )
			{
				final double xs =
					( x - 2.0 * p.fdSize + 0.5 ) * octave_sigma; //!< scale x around 0,0
				final double yr = cos_o * ys + sin_o * xs; //!< rotate y around 0,0
				final double xr = cos_o * xs - sin_o * ys; //!< rotate x around 0,0

				// flip_range at borders
				// TODO for now, the gradients orientations do not flip outside
				// the image even though they should do it. But would this
				// improve the result?

				// translate ys to sample y position in the gradient image
				final int yg = Util.pingPong(
						( int )( Math.round( yr + c[ 1 ] ) ),
						gradients.height);

				// translate xs to sample x position in the gradient image
				final int xg = Util.pingPong(
						( int )( Math.round( xr + c[ 0 ] ) ),
						gradients.width);

				// get the samples
				final int region_p = fdWidth * y + x;
				descDx[region_p] = gradients.derX(xg, yg);
				descDy[region_p] = gradients.derY(xg, yg);
			}
		}

		// weigh the gradients
		for (int q = 0; q < gradMag.length; ++q)
			gradMag[q] = FloatArray2DScaleOctave.Gradients.mag(descDx[q], descDy[q]) * gaussian[q];

		// rotate the gradients orientation it with respect to the features orientation
		for (int q = 0; q < gradOri.length; ++q)
			gradOri[q] = (float)(Filter.fastAtan2(descDy[q], descDx[q]) - orientation);

		Arrays.fill(hist, 0);

		// build the orientation histograms of 4x4 subregions: bins and weights of all samples first (vectorizes),
		// then accumulate them in the original order
		for (int q = 0; q < gradOri.length; ++q) {
			final float bin_location = (gradOri[q] + (float)Math.PI) / fdBinWidth;
			final int bin_b = (int)(bin_location);
			final float d = bin_location - bin_b;
			final float t = gradMag[q];
			descBinB[q] = wrapBin(bin_b, p.fdBins);
			descBinT[q] = wrapBin(bin_b + 1, p.fdBins);
			descW0[q] = t * (1 - d);
			descW1[q] = t * d;
		}
		for ( int y = p.fdSize - 1; y >= 0; --y )
		{
			final int yp = p.fdSize * 16 * y;
			for ( int x = p.fdSize - 1; x >= 0; --x )
			{
				final int xp = 4 * x;
				final int h = (p.fdSize * y + x) * p.fdBins;
				for ( int ysr = 3; ysr >= 0; --ysr )
				{
					final int ysrp = 4 * p.fdSize * ysr;
					for ( int xsr = 3; xsr >= 0; --xsr )
					{
						final int q = yp + xp + ysrp + xsr;
						hist[h + descBinB[q]] += descW0[q];
						hist[h + descBinT[q]] += descW1[q];
					}
				}
			}
		}

		final float[] desc = new float[ p.fdSize * p.fdSize * p.fdBins ];

		// normalize, cut above 0.2 and renormalize
		float max_bin_val = 0;
		int i = 0;
		for ( int y = p.fdSize - 1; y >= 0; --y )
		{
			for ( int x = p.fdSize - 1; x >= 0; --x )
			{
				for ( int b = p.fdBins - 1; b >= 0; --b )
				{
					desc[i] = hist[(p.fdSize * y + x) * p.fdBins + b];
					if ( desc[ i ] > max_bin_val ) max_bin_val = desc[ i ];
					++i;
				}
			}
		}
		max_bin_val /= 0.2f;
		for ( i = 0; i < desc.length; ++i )
		{
			desc[ i ] = ( float )Math.min( 1.0, desc[ i ] / max_bin_val );
		}

		return desc;
	}

	/**
	 * assign orientation to the given candidate, if more than one orientation
	 * found, duplicate the feature for each orientation
	 * <p>
	 * estimate the feature descriptor for each of those candidates
	 *
	 * @param c candidate {@code 0=>x, 1=>y, 2=>scale index}
	 * @param o octave index
	 * @param features finally contains all processed candidates
	 */
	final protected void processCandidate(
			final double[] c,
			final int o,
			final List< Feature > features )
	{
		final int scale = 1 << o;

		final FloatArray2DScaleOctave octave = octaves[ o ];

		final double octave_sigma = octave.SIGMA[ 0 ] * Math.pow( 2.0, c[ 2 ] / octave.STEPS );

		final float[] histogram_bins = orientationHistogram.compute(octave, c, octave_sigma);

		// find the dominant orientation and interpolate it with respect to its two neighbors
		int max_i = 0;
		for ( int i = 0; i < ORIENTATION_BINS; ++i )
		{
			if ( histogram_bins[ i ] > histogram_bins[ max_i ] ) max_i = i;
		}

		/*
		 * interpolate orientation estimate the offset from center of the
		 * parabolic extremum of the taylor series through env[1], derivatives
		 * via central difference and laplace
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

		/*
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
				/*
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
									createDescriptor( c, o, octave_sigma, orientation ) ) );
				}
			}
		}
	}


	/**
	 * detect features in the specified scale octave
	 *
	 * @param o octave index
	 *
	 * @return detected features
	 */
	private Vector< Feature > runOctave( final int o )
	{
		final Vector< Feature > features = new Vector<>();
		final FloatArray2DScaleOctave octave = octaves[ o ];
		octave.build();
		dog.run( octave );
		final Vector< double[] > candidates = dog.getCandidates();
		for ( final double[] c : candidates )
		{
			this.processCandidate( c, o, features );
		}

		return features;
	}

	/**
	 * detect features in all scale octaves
	 *
	 * @return detected features
	 */
	public Vector< Feature > run()
	{
		final Vector< Feature > features = new Vector<>();
		for ( int o = 0; o < octaves.length; ++o )
		{
			if ( octaves[ o ].state == FloatArray2DScaleOctave.State.EMPTY ) continue;
			final Vector< Feature > more = runOctave( o );
			features.addAll( more );
		}
		return features;
	}

	/**
	 * detect features in all scale octaves
	 *
	 * @return detected features
	 */
	public Vector< Feature > run( final int max_size )
	{
		final Vector< Feature > features = new Vector<>();
		for ( int o = 0; o < octaves.length; ++o )
		{
			if ( octaves[ o ].width <= max_size && octaves[ o ].height <= max_size )
			{
				final Vector< Feature > more = runOctave( o );
				features.addAll( more );
			}
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
	public static Vector< PointMatch > createMatches(
			final List< Feature > fs1,
			final List< Feature > fs2,
			final float rod )
	{
		final Vector< PointMatch > matches = new Vector<>();

		for ( final Feature f1 : fs1 )
		{
			Feature best = null;
			double best_d = Double.MAX_VALUE;
			double second_best_d = Double.MAX_VALUE;

			for ( final Feature f2 : fs2 )
			{
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
				matches.addElement(
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
					matches.removeElementAt( j );
				}
				else ++j;
			}
			if ( amb )
				matches.removeElementAt( i );
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
	public static Vector< PointMatch > createMatches(
			final List< Feature > fs1,
			final List< Feature > fs2,
			final double max_sd,
			final AbstractModel< ? > model,
			final double max_id,
			final double rod )
	{
		final Vector< PointMatch > matches = new Vector<>();
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
				matches.addElement(
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
					matches.removeElementAt( j );
				}
				else ++j;
			}
			if ( amb )
			{
				//System.out.println( "removing ambiguous match at " + i );
				matches.removeElementAt( i );
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
	 */
	public static double[] featureSizeHistogram(
			final Vector< Feature > features,
			final double min,
			final double max,
			final int bins )
	{
		System.out.print( "estimating feature size histogram ..." );
		final int num_features = features.size();
		final double[] h = new double[bins];
		final int[] hb = new int[bins];

		for ( final Feature f : features )
		{
			final int bin = Math.max(0, Math.min(bins - 1, (int)(Math.log(f.scale) / Math.log(2.0) * 28.0f)));
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
			sigma[ i ] = p.initialSigma * ( float )Math.pow( 2.0f, ( float )i / ( float )p.steps );
			sigma_diff[ i ] = ( float )Math.sqrt( sigma[ i ] * sigma[ i ] - p.initialSigma * p.initialSigma );

			kernel_diff[ i ] = Filter.createGaussianKernel( sigma_diff[ i ], true );
		}
	}

	final public int getMaxOctaveSize(){ return p.maxOctaveSize; }
}
