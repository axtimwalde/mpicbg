package mpicbg.ij.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;

import ij.plugin.filter.GaussianBlur;
import ij.process.FloatProcessor;

/**
 * {@link Filter#createDownsampled} re-implements ImageJ's {@link GaussianBlur} arithmetic in a
 * vectorizable form; its output must be bit-identical to blurring with ImageJ and sampling.
 */
public class CreateDownsampledTest {
	/** The original implementation: duplicate, ImageJ blur, nearest-neighbor LUT sampling. */
	static FloatProcessor reference(final FloatProcessor source, final double scale, final float sourceSigma, final float targetSigma) {
		final int ow = source.getWidth(), oh = source.getHeight();
		final int w = (int)Math.round(ow * scale), h = (int)Math.round(oh * scale);
		final FloatProcessor temp = (FloatProcessor)source.duplicate();
		final double s = targetSigma / scale;
		final double v = s * s - sourceSigma * sourceSigma;
		if (v > 0) {
			final float sigma = (float)Math.sqrt(v);
			new GaussianBlur().blurFloat(temp, sigma, sigma, 0.01);
		}
		if (scale == 1.0f) return temp;
		final float[] tempPixels = (float[])temp.getPixels();
		final FloatProcessor target = new FloatProcessor(w, h);
		final float[] targetPixels = (float[])target.getPixels();
		for (int y = 0; y < h; ++y) {
			final int q = Math.min(oh - 1, Math.max(0, (int)Math.round(y / scale))) * ow;
			for (int x = 0; x < w; ++x)
				targetPixels[y * w + x] = tempPixels[q + Math.min(ow - 1, Math.max(0, (int)Math.round(x / scale)))];
		}
		return target;
	}

	static FloatProcessor randomImage(final int w, final int h, final long seed) {
		final Random rnd = new Random(seed);
		final float[] pixels = new float[w * h];
		for (int i = 0; i < pixels.length; ++i)
			pixels[i] = rnd.nextInt(3) == 0 ? 0 : (rnd.nextFloat() - 0.25f) * 255;
		return new FloatProcessor(w, h, pixels);
	}

	static void check(final int w, final int h, final double scale, final float sourceSigma, final float targetSigma) {
		final FloatProcessor source = randomImage(w, h, 42);
		final FloatProcessor expected = reference(source, scale, sourceSigma, targetSigma);
		final FloatProcessor actual = Filter.createDownsampled(source, scale, sourceSigma, targetSigma);
		final String at = w + "x" + h + " scale " + scale + " sigma " + sourceSigma + "->" + targetSigma;
		assertEquals(expected.getWidth(), actual.getWidth(), at);
		assertEquals(expected.getHeight(), actual.getHeight(), at);
		assertArrayEquals((float[])expected.getPixels(), (float[])actual.getPixels(), 0.0f, at);
	}

	@Test
	public void matchesImageJBitExactly() {
		check(2000, 1748, 1023.0 / 2000, 0.5f, 0.5f); // SIFT on an EM tile
		check(301, 203, 0.5, 0.5f, 0.5f);
		check(301, 203, 0.31, 0.5f, 0.5f);
		check(301, 203, 0.77, 0.5f, 0.5f);
		check(301, 203, 0.5, 0.5f, 1.6f); // larger kernel
		check(301, 203, 1.0, 0.5f, 1.0f); // blur only
		check(301, 203, 0.5, 2.0f, 0.5f); // no blur, sampling only
		check(5, 7, 0.5, 0.5f, 2.0f); // lines shorter than the kernel
		check(60, 40, 0.1, 0.5f, 3.0f); // sigma above ImageJ's downscaling threshold: delegated to ImageJ
	}
}
