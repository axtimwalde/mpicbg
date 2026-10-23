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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Verifies {@link Filter#fastAtan2} matches {@link Math#atan2} within the 1e-6 rad error
 * bound documented on the method, across all four quadrants and the axes.
 */
class FilterTest {

	private static final double TOLERANCE = 1e-6;

	@Test
	void zeroArgumentsReturnZero() {
		assertEquals(0f, Filter.fastAtan2(0, 0));
	}

	@Test
	void matchesMathAtan2AcrossQuadrantsAndAxes() {
		// Evaluate a grid of points in the range [-10, 10] for both x and y, skipping the origin (0, 0).
		for (int xi = -10; xi <= 10; ++xi) {
			for (int yi = -10; yi <= 10; ++yi) {
				if (xi == 0 && yi == 0)
					continue;

				final float x = xi * 0.37f;
				final float y = yi * 0.61f;
				final double expected = Math.atan2(y, x);
				final double actual = Filter.fastAtan2(y, x);

				assertEquals(expected, actual, TOLERANCE, "x=" + x + " y=" + y);
			}
		}
	}
}
