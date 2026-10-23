/*-
 * #%L
 * MPICBG Core Library.
 * %%
 * Copyright (C) 2008 - 2026 Stephan Saalfeld et. al.
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
package mpicbg.ij.clahe;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Self-contained correctness checks for the Flat CLAHE implementation.
 * Each test asserts an analytical invariant any correct CLAHE must satisfy.
 */
class ClaheTest {

	private static final float EPS = 1e-6f;

	private static ByteProcessor randomBp(final int w, final int h, final long seed) {
		final ByteProcessor bp = new ByteProcessor(w, h);
		final Random rng = new Random(seed);
		final byte[] px = (byte[]) bp.getPixels();
		rng.nextBytes(px);
		return bp;
	}

	private static ByteProcessor ramp(final int w, final int h, final int lo, final int hi) {
		final ByteProcessor bp = new ByteProcessor(w, h);
		for (int y = 0; y < h; ++y)
			for (int x = 0; x < w; ++x)
				bp.set(x, y, lo + (x * (hi - lo)) / (w - 1));
		return bp;
	}

	// ---------- Util-level analytical checks --------------------------------

	@Test
	void utilTransferUniform() {
		final int[] hist = {1, 1, 1, 1, 1, 1, 1, 1};
		final int[] clipped = new int[hist.length];
		final int limit = 8;
		for (int v = 0; v < hist.length; ++v) {
			final float t = Util.transferValue(v, hist, clipped, limit);
			assertEquals(v / 7.0f, t, EPS, "uniform transfer at v=" + v);
		}
	}

	@Test
	void utilTransferBinary() {
		final int[] hist = {10, 0, 0, 0, 0, 0, 0, 10};
		final int[] clipped = new int[hist.length];
		final int limit = 10;
		for (int v = 0; v < 7; ++v) {
			final float t = Util.transferValue(v, hist, clipped, limit);
			assertEquals(0.0f, t, EPS, "binary transfer at v=" + v);
		}
		assertEquals(1.0f, Util.transferValue(7, hist, clipped, limit), EPS, "binary transfer at v=7");
	}

	/**
	 * Pointwise transferValue must match the bulk LUT produced by createTransfer
	 * for every bin, on a non-trivial histogram that exercises the lookahead
	 * clipping path.
	 */
	@Test
	void utilCreateTransferMatchesPointwise() {
		final int[] hist = {100, 0, 0, 0, 5, 5, 5, 0, 0, 0};
		final int limit = 20;
		final float[] lut = Util.createTransfer(hist, limit);
		final int[] clipped = new int[hist.length];
		for (int v = 0; v < hist.length; ++v) {
			final float t = Util.transferValue(v, hist, clipped, limit);
			assertEquals(lut[v], t, EPS, "createTransfer vs transferValue at v=" + v);
		}
	}

	/**
	 * Ramp hist[i] = i with bins odd and limit = (bins-1)/2: deficit of the
	 * lower half exactly matches excess of the upper half, so redistribution
	 * lands every bin at exactly limit.
	 */
	@Test
	void clipHistogramRampToUniform() {
		final int bins = 9;
		final int limit = (bins - 1) / 2;
		final int[] hist = new int[bins];
		for (int i = 0; i < bins; ++i) hist[i] = i;
		final int[] clipped = new int[bins];
		Util.transferValue(0, hist, clipped, limit);
		final int[] expected = new int[bins];
		Arrays.fill(expected, limit);
		assertArrayEquals(expected, clipped, "clipped histogram should be uniform at limit");
	}

	// ---------- Pipeline-level analytical check -----------------------------

	/**
	 * 16x16 image with each grey level 0..255 appearing exactly once; under
	 * global blockRadius and no clipping the transfer reduces to v/255 and
	 * the output must be byte-identical to the input.
	 */
	@Test
	void globalEqualizationOnUniformImageIsIdentity() {
		final int w = 16, h = 16;
		final ByteProcessor src = new ByteProcessor(w, h);
		for (int i = 0; i < w * h; ++i) src.set(i, i);
		final byte[] before = Arrays.copyOf((byte[]) src.getPixels(), w * h);

		final ImagePlus imp = new ImagePlus("uniform", src);
		Flat.getInstance().run(imp, Math.max(w, h) - 1, 256, 1e6f, null, false);

		final byte[] after = (byte[]) imp.getProcessor().getPixels();
		assertArrayEquals(before, after, "global eq on uniform image must be identity");
	}

	// ---------- Structural invariants ---------------------------------------

	/**
	 * Under global blockRadius every pixel sees the same window histogram, so
	 * identical inputs map to identical outputs and the transfer (a CDF) is
	 * monotone non-decreasing in the input.
	 */
	@Test
	void monotonicityWithGlobalBlock() {
		final int w = 32, h = 32;
		final ByteProcessor src = randomBp(w, h, 0xC0FFEEL);
		final byte[] inputs = Arrays.copyOf((byte[]) src.getPixels(), w * h);

		final ImagePlus imp = new ImagePlus("rand", src);
		Flat.getInstance().run(imp, Math.max(w, h) - 1, 256, 4.0f, null, false);
		final byte[] outputs = (byte[]) imp.getProcessor().getPixels();

		final int[] minOut = new int[256];
		final int[] maxOut = new int[256];
		final boolean[] seen = new boolean[256];
		Arrays.fill(minOut, 256);
		Arrays.fill(maxOut, -1);
		for (int i = 0; i < inputs.length; ++i) {
			final int in = inputs[i] & 0xff;
			final int out = outputs[i] & 0xff;
			seen[in] = true;
			if (out < minOut[in]) minOut[in] = out;
			if (out > maxOut[in]) maxOut[in] = out;
		}
		int prev = -1;
		for (int v = 0; v < 256; ++v) {
			if (!seen[v]) continue;
			assertEquals(minOut[v], maxOut[v],
					"input " + v + " must map to a single output under global block");
			if (minOut[v] < prev)
				throw new AssertionError("monotonicity violated at input " + v +
						": output " + minOut[v] + " < previous " + prev);
			prev = maxOut[v];
		}
	}

	@Test
	void determinism() {
		final ByteProcessor a = randomBp(48, 48, 0xBEEFL);
		final ByteProcessor b = (ByteProcessor) a.duplicate();
		final ImagePlus impA = new ImagePlus("a", a);
		final ImagePlus impB = new ImagePlus("b", b);
		Flat.getInstance().run(impA, 8, 256, 3.0f, null, false);
		Flat.getInstance().run(impB, 8, 256, 3.0f, null, false);
		assertArrayEquals((byte[]) impA.getProcessor().getPixels(),
				(byte[]) impB.getProcessor().getPixels(),
				"Flat must be deterministic");
	}

	/**
	 * Ramp confined to [lo, hi] under global blockRadius and slope high enough
	 * to disable clipping must be stretched to span [0, 255]: the lowest
	 * occupied bin lands at 0 (cdf == cdfMin) and the highest at 255.
	 */
	@Test
	void linearRampSpansFullRange() {
		final int w = 64, h = 64;
		final int lo = 50, hi = 200;
		final ByteProcessor src = ramp(w, h, lo, hi);
		final ImagePlus imp = new ImagePlus("ramp", src);
		Flat.getInstance().run(imp, Math.max(w, h) - 1, 256, 100.0f, null, false);

		final byte[] px = (byte[]) imp.getProcessor().getPixels();
		int min = 255, max = 0;
		for (byte b : px) {
			final int v = b & 0xff;
			if (v < min) min = v;
			if (v > max) max = v;
		}
		assertEquals(0, min, "ramp must be stretched to minimum 0");
		assertEquals(255, max, "ramp must be stretched to maximum 255");
	}
}
