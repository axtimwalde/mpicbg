package mpicbg.ij;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import ij.process.ImageProcessor;
import mpicbg.imagefeatures.Feature;
import mpicbg.imagefeatures.FloatArray2DSIFT;
import mpicbg.imagefeatures.TestData;
import mpicbg.models.PointMatch;

/**
 * {@link FeatureTransform#matchFeatures} between the SIFT features of the test image and of its
 * rotated copy must return the recorded matches: same count and order, coordinates within
 * {@link #TOLERANCE}.
 */
public class MatchFeaturesGroundTruthTest {
	static final String JSON = "ground_truth_matches.json";

	static final float ROD = 0.92f;

	static final double TOLERANCE = 1e-4;

	static class GtMatch {
		double x1, y1, x2, y2;
	}

	static List<PointMatch> compute() {
		final ImageProcessor ip = FeatureGroundTruthTest.image();
		final FloatArray2DSIFT.Param p = new FloatArray2DSIFT.Param();
		final List<Feature> fs1 = new ArrayList<>(), fs2 = new ArrayList<>();
		new SIFT(new FloatArray2DSIFT(p)).extractFeatures(ip, fs1);
		new SIFT(new FloatArray2DSIFT(p)).extractFeatures(ip.rotateRight(), fs2);
		return FeatureTransform.matchFeatures(fs1, fs2, ROD);
	}

	@Test
	public void matchesEqualGroundTruth() throws IOException {
		final GtMatch[] gt;
		try (Reader r = Files.newBufferedReader(TestData.file(JSON).toPath())) {
			gt = new Gson().fromJson(r, GtMatch[].class);
		}
		final List<PointMatch> matches = compute();
		assertEquals(gt.length, matches.size(), "match count");
		for (int i = 0; i < gt.length; ++i) {
			final double[] p1 = matches.get(i).getP1().getL(), p2 = matches.get(i).getP2().getL();
			assertEquals(gt[i].x1, p1[0], TOLERANCE, "match " + i + " x1");
			assertEquals(gt[i].y1, p1[1], TOLERANCE, "match " + i + " y1");
			assertEquals(gt[i].x2, p2[0], TOLERANCE, "match " + i + " x2");
			assertEquals(gt[i].y2, p2[1], TOLERANCE, "match " + i + " y2");
		}
	}

	/** Regenerate the ground truth from the current code. Only for intentional changes of the results. */
	public static void main(final String[] args) throws IOException {
		final List<PointMatch> matches = compute();
		final GtMatch[] gt = new GtMatch[matches.size()];
		for (int i = 0; i < gt.length; ++i) {
			final double[] p1 = matches.get(i).getP1().getL(), p2 = matches.get(i).getP2().getL();
			gt[i] = new GtMatch();
			gt[i].x1 = p1[0];
			gt[i].y1 = p1[1];
			gt[i].x2 = p2[0];
			gt[i].y2 = p2[1];
		}
		try (Writer w = Files.newBufferedWriter(TestData.sourceFile(JSON).toPath())) {
			new Gson().toJson(gt, w);
		}
		System.out.println("wrote " + gt.length + " matches to " + TestData.sourceFile(JSON));
		System.exit(0); // ImageJ leaves a non-daemon thread pool behind that would keep the JVM alive
	}
}
