package mpicbg.imagefeatures;

import java.io.File;
import java.net.URISyntaxException;

/**
 * Test resources under {@code src/test/resources}: the test image and the ground-truth files.
 */
public class TestData {
	/** The resource as a file on the test classpath (ImageJ's {@code Opener} needs a path). */
	public static File file(final String name) {
		try {
			return new File(TestData.class.getResource("/" + name).toURI());
		} catch (final URISyntaxException e) {
			throw new IllegalArgumentException(name, e);
		}
	}

	/**
	 * The resource's source file, for regenerating ground truth. Resolved relative to the working
	 * directory, which is the module directory under Maven or the repository root under an IDE.
	 */
	public static File sourceFile(final String name) {
		final File module = new File("src/test/resources");
		return new File(module.isDirectory() ? module : new File("mpicbg/src/test/resources"), name);
	}
}
