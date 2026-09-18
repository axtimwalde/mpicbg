package mpicbg.imagefeatures;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class OrientationHistogramTest {
	private static final int SIZE = 64;
	private static final float SIGMA = 1.6f;

	/** Linear ramp with slopes exactly representable in float, so gradients are noise free. */
	private static FloatArray2D ramp() {
		final FloatArray2D img = new FloatArray2D(SIZE, SIZE);
		for (int y = 0; y < SIZE; ++y)
			for (int x = 0; x < SIZE; ++x)
				img.data[y * SIZE + x] = 1.5f * x + 2.75f * y;
		return img;
	}

	/** Histogram at scale index 0, i.e. on the unblurred image. */
	private static float[] histogram(final FloatArray2D img, final int x, final int y) {
		final FloatArray2DScaleOctave octave = new FloatArray2DScaleOctave(img, 1, SIGMA);
		octave.build();
		return new OrientationHistogram().compute(octave, new double[] {x, y, 0}, SIGMA).clone();
	}

	/**
	 * Since the gradient is uniform, the histogram of the gradient orientations should look the
	 * same at both vertical or horizontal edges. Since the gradient does not have the same vertical
	 * and horizontal component, horizontal and vertical edges are not the same.
	 */
	@Test
	void leftBorderMatchesOtherBorders() {
		final float delta = 1e-4f;
		final int mid = SIZE / 2;
		final int end = SIZE - 2;
		final FloatArray2D img = ramp();

		assertArrayEquals(histogram(img, mid, 1), histogram(img, mid, end), delta, "top vs bottom");
		assertArrayEquals(histogram(img, 1, mid), histogram(img, end, mid), delta, "left vs right");
	}
}
