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

import java.util.Arrays;

/**
 * Histogram of the gradient orientations in a Gaussian window around a candidate, from which
 * {@link FloatArray2DSIFT} and {@link FloatArray2DMOPS} pick the dominant orientations. An
 * instance holds the scratch buffers of one thread of candidates and is not thread-safe.
 */
final class OrientationHistogram {
	final static int BINS = 36;
	final static double BIN_SIZE = 2.0 * Math.PI / BINS;

	final private float[] histogramBins = new float[BINS];

	// The window with a one pixel border (called "patch") and four shifted copies
	private float[] pat = new float[0];
	private float[] patL = new float[0];
	private float[] patR = new float[0];
	private float[] patU = new float[0];
	private float[] patD = new float[0];

	// Per pixel derivatives, weight, magnitude, orientation, bin index in the window (no border)
	private float[] winDx = new float[0];
	private float[] winDy = new float[0];
	private float[] winWei = new float[0];
	private float[] winMag = new float[0];
	private float[] winOri = new float[0];
	private int[] roiBin = new int[0];

	/**
	 * Build the orientation histogram of the region around a candidate: the gradient magnitudes of
	 * the level closest to the candidate's scale, weighted by a circular Gaussian window with sigma
	 * 1.5 times that of the candidate, accumulated into {@link #BINS} bins of the orientation.
	 *
	 * @param octave the octave of the candidate
	 * @param candidate candidate {@code 0=>x, 1=>y, 2=>scale index}
	 * @param octaveSigma the candidate's sigma in the octave
	 *
	 * @return the histogram, a buffer of this instance that the next call overwrites
	 */
	float[] compute(
			final FloatArray2DScaleOctave octave,
			final double[] candidate,
			final double octaveSigma
	) {
		final float[] histogramBins = this.histogramBins;
		Arrays.fill(histogramBins, 0);

		// Create a circular gaussian window with sigma 1.5 times that of the feature.
		// The window is separable: exp(-(dx² + dy²) / s) = exp(-dx² / s) * exp(-dy² / s), so
		// 2 * size exp calls replace size^2. Rounding differs in the last bit
		// from the direct 2d evaluation of Filter.createGaussianKernelOffset.
		final double maskSigma = octaveSigma * 1.5;
		final int windowSize = Math.max(3, (int)(2 * Math.round(3 * maskSigma) + 1));
		final int halfSize = windowSize / 2;
		final int patchSize = windowSize + 2; // the window plus a one pixel border for the derivatives
		final int patchElements = patchSize * patchSize;

		// Ensure the scratch buffers are large enough for the largest window seen so far
		if (pat.length < patchElements) {
			pat = new float[patchElements];
			patL = new float[patchElements];
			patR = new float[patchElements];
			patU = new float[patchElements];
			patD = new float[patchElements];
			winDx = new float[patchElements];
			winDy = new float[patchElements];
			winWei = new float[patchElements];
			winMag = new float[patchElements];
			winOri = new float[patchElements];
			roiBin = new int[patchElements];
		}

		// Initialize the separable gaussian kernel
		final double spread = 2 * maskSigma * maskSigma;
		final double offsetX = candidate[0] - Math.floor(candidate[0]);
		final double offsetY = candidate[1] - Math.floor(candidate[1]);

		final float[] wx = new float[windowSize];
		for (int x = 0; x < windowSize; ++x) {
			final double dx = x - halfSize - offsetX;
			wx[x] = (float) Math.exp(-dx * dx / spread);
		}

		for (int y = 0; y < windowSize; ++y) {
			final double dy = y - halfSize - offsetY;
			final float wy = (float)Math.exp(-dy * dy / spread);
			final int rowY = (y + 1) * patchSize + 1;

			for (int x = 0; x < windowSize; ++x) {
				winWei[rowY + x] = wy * wx[x];
			}
		}

		/*
		 * Get the gradients in the window around the keypoint, weighted by the window. Window pixel
		 * (xi, yi) lives at (yi + 1) * patchSize + xi + 1 of the padded scratch arrays; the padding positions
		 * hold junk that is never read. In the interior, the window is copied with its border into pat and
		 * the derivatives, weighted magnitudes and orientations are evaluated in flat passes over shifted
		 * copies (the loops then have identical indices, which C2 vectorizes; the orientation stays scalar).
		 */
		final FloatArray2DScaleOctave.Gradients src = octave.getGradients((int)Math.round(candidate[2]));
		final int cx = (int)candidate[0];
		final int cy = (int)candidate[1];

		final int winStartX = cx - halfSize;
		final int winStartY = cy - halfSize;
		final int winEndX = winStartX + windowSize;
		final int winEndY = winStartY + windowSize;

		if (winStartX >= 1 && winEndX <= src.width - 1 && winStartY >= 1 && winEndY <= src.height - 1) {
			// Copy the window with a one pixel border into the scratch array
			for (int y = 0; y < patchSize; ++y) {
				final int rowYStart = (winStartY - 1 + y) * src.width + winStartX - 1;
				System.arraycopy(src.data, rowYStart, pat, y * patchSize, patchSize);
			}

			// Copy the shifted versions of the patch for the derivatives (row-major order, so everything is contiguous)
			System.arraycopy(pat, 0, patL, 1, patchElements - 1); // patL[k] = pat[k - 1]
			System.arraycopy(pat, 1, patR, 0, patchElements - 1); // patR[k] = pat[k + 1]
			System.arraycopy(pat, 0, patU, patchSize, patchElements - patchSize); // patU[k] = pat[k - patchSize]
			System.arraycopy(pat, patchSize, patD, 0, patchElements - patchSize); // patD[k] = pat[k + patchSize]

			// Compute the derivatives
			for (int k = 0; k < patchElements; ++k) {
				winDx[k] = (patR[k] - patL[k]) / 2;
				winDy[k] = (patD[k] - patU[k]) / 2;
			}

			// Compute gradient magnitudes, weighted by the gaussian window
			for (int k = 0; k < patchElements; ++k) {
				winMag[k] = FloatArray2DScaleOctave.Gradients.mag(winDx[k], winDy[k]) * winWei[k];
			}

			// Compute gradient orientations
			for (int y = 0; y < windowSize; ++y) {
				final int rowY = (y + 1) * patchSize + 1;
				for (int x = 0; x < windowSize; ++x)
					winOri[rowY + x] = Filter.fastAtan2(winDy[rowY + x], winDx[rowY + x]);
			}
		} else {
			// The window reaches over the image border: clamp coordinates pixel by pixel
			for (int y = 0; y < windowSize; ++y) {
				final int ya = Math.max(0, Math.min(src.height - 1, winStartY + y));
				final int rowY = (y + 1) * patchSize + 1;
				for (int x = 0; x < windowSize; ++x) {
					final int xa = Math.max(0, Math.min(src.width - 1, winStartX + x));
					final float Dx = src.derX(xa, ya);
					final float Dy = src.derY(xa, ya);
					winMag[rowY + x] = FloatArray2DScaleOctave.Gradients.mag(Dx, Dy) * winWei[rowY + x];
					winOri[rowY + x] = Filter.fastAtan2(Dy, Dx);
				}
			}
		}

		// Build an orientation histogram of the region: bins for the whole patch (vectorizes), accumulation in scan order
		// The histogram weighs orientations by the gaussian-weighted gradient magnitudes
		final int[] bin = roiBin;
		for (int m = 0; m < patchElements; ++m) {
			final double binIndex = (winOri[m] + Math.PI) / BIN_SIZE;
			bin[m] = Math.max(0, Math.min(BINS - 1, (int)binIndex));
		}
		for (int y = 0; y < windowSize; ++y) {
			final int rowY = (y + 1) * patchSize + 1;
			for (int x = 0; x < windowSize; ++x)
				histogramBins[bin[rowY + x]] += winMag[rowY + x];
		}

		return histogramBins;
	}
}
