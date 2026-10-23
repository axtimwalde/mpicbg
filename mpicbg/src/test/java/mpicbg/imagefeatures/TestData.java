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
