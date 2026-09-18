package mpicbg.ij;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import ij.io.Opener;
import ij.process.ImageProcessor;
import mpicbg.imagefeatures.Feature;
import mpicbg.imagefeatures.FloatArray2DMOPS;
import mpicbg.imagefeatures.FloatArray2DSIFT;
import mpicbg.imagefeatures.TestData;

/**
 * SIFT and MOPS features of the test image, extracted through the production {@link SIFT} and
 * {@link MOPS} paths with default parameters, must stay within {@link #TOLERANCE} of the ground truth
 * that was recorded from the original implementation. Feature count and order must match exactly.
 */
public class FeatureGroundTruthTest {
	static final String IMAGE = "boats-512.png";

	static final double TOLERANCE = 1e-4;

	static class GtFeature {
		double x, y, scale, orientation;
		float[] descriptor;
	}

	static ImageProcessor image() {
		return new Opener().openImage(TestData.file(IMAGE).getPath()).getProcessor();
	}

	static List<Feature> sift() {
		final List<Feature> features = new ArrayList<>();
		new SIFT(new FloatArray2DSIFT(new FloatArray2DSIFT.Param())).extractFeatures(image(), features);
		return features;
	}

	static List<Feature> mops() {
		final List<Feature> features = new ArrayList<>();
		new MOPS(new FloatArray2DMOPS(new FloatArray2DMOPS.Param())).extractFeatures(image(), features);
		return features;
	}

	static void check(final String jsonName, final List<Feature> features) throws IOException {
		final GtFeature[] gt;
		try (Reader r = Files.newBufferedReader(TestData.file(jsonName).toPath())) {
			gt = new Gson().fromJson(r, GtFeature[].class);
		}
		assertEquals(gt.length, features.size(), "feature count");
		for (int i = 0; i < gt.length; ++i) {
			final GtFeature g = gt[i];
			final Feature f = features.get(i);
			final String at = "feature " + i;
			assertEquals(g.x, f.location[0], TOLERANCE, at + " x");
			assertEquals(g.y, f.location[1], TOLERANCE, at + " y");
			assertEquals(g.scale, f.scale, TOLERANCE, at + " scale");
			assertEquals(g.orientation, f.orientation, TOLERANCE, at + " orientation");
			assertArrayEquals(g.descriptor, f.descriptor, (float)TOLERANCE, at + " descriptor");
		}
	}

	@Test
	public void siftFeaturesMatchGroundTruth() throws IOException {
		check("ground_truth_sift.json", sift());
	}

	@Test
	public void mopsFeaturesMatchGroundTruth() throws IOException {
		check("ground_truth_mops.json", mops());
	}

	static void write(final String jsonName, final List<Feature> features) throws IOException {
		final GtFeature[] gt = new GtFeature[features.size()];
		for (int i = 0; i < gt.length; ++i) {
			final Feature f = features.get(i);
			gt[i] = new GtFeature();
			gt[i].x = f.location[0];
			gt[i].y = f.location[1];
			gt[i].scale = f.scale;
			gt[i].orientation = f.orientation;
			gt[i].descriptor = f.descriptor;
		}
		try (Writer w = Files.newBufferedWriter(TestData.sourceFile(jsonName).toPath())) {
			new Gson().toJson(gt, w);
		}
		System.out.println("wrote " + gt.length + " features to " + TestData.sourceFile(jsonName));
	}

	/** Regenerate the ground truth from the current code. Only for intentional changes of the results. */
	public static void main(final String[] args) throws IOException {
		write("ground_truth_sift.json", sift());
		write("ground_truth_mops.json", mops());
		System.exit(0); // ImageJ leaves a non-daemon thread pool behind that would keep the JVM alive
	}
}
