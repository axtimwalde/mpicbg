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
package mpicbg.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for {@link RingBuffer}. Expected values are the output of
 * the legacy {@code test/ringbuffer.bsh} demo script, captured as the
 * specification of current behavior.
 */
class RingBufferTest {

	private static <T> List<T> unordered(final RingBuffer<T> rb) {
		final List<T> result = new ArrayList<>();
		final Iterator<T> i = rb.iterator();
		while (i.hasNext()) {
			result.add(i.next());
		}
		return result;
	}

	private static <T> List<T> ordered(final RingBuffer<T> rb) {
		final List<T> result = new ArrayList<>();
		final ListIterator<T> i = rb.listIterator();
		while (i.hasNext()) {
			result.add(i.next());
		}
		return result;
	}

	@Test
	void emptyBuffer() {
		final RingBuffer<Integer> rb = new RingBuffer<>(5);
		assertEquals(0, rb.size());
		assertEquals(0, rb.nextIndex());
		assertEquals(java.util.Collections.<Integer>emptyList(), unordered(rb));
		assertEquals(java.util.Collections.<Integer>emptyList(), ordered(rb));
	}

	@Test
	void addingFillsBufferInOrder() {
		final RingBuffer<Integer> rb = new RingBuffer<>(5);
		for (int i = 0; i < 5; ++i) {
			rb.add(i);
		}
		assertEquals(5, rb.size());
		assertEquals(5, rb.nextIndex());
		assertEquals(Arrays.asList(0, 1, 2, 3, 4), unordered(rb));
		assertEquals(Arrays.asList(0, 1, 2, 3, 4), ordered(rb));
	}

	@Test
	void addingPastCapacityWrapsRawSlotsButPreservesLogicalOrder() {
		final RingBuffer<Integer> rb = new RingBuffer<>(5);
		for (int i = 0; i < 6; ++i) {
			rb.add(i);
		}
		assertEquals(5, rb.size());
		assertEquals(6, rb.nextIndex());
		// raw iterator walks underlying slots, so the freshly overwritten slot 0 leads
		assertEquals(Arrays.asList(5, 1, 2, 3, 4), unordered(rb));
		// listIterator walks in insertion order
		assertEquals(Arrays.asList(1, 2, 3, 4, 5), ordered(rb));
	}

	@Test
	void addingTenElementsLeavesLastFive() {
		final RingBuffer<Integer> rb = new RingBuffer<>(5);
		for (int i = 0; i < 10; ++i) {
			rb.add(i);
		}
		assertEquals(5, rb.size());
		assertEquals(10, rb.nextIndex());
		assertEquals(Arrays.asList(5, 6, 7, 8, 9), unordered(rb));
		assertEquals(Arrays.asList(5, 6, 7, 8, 9), ordered(rb));
	}

	@Test
	void listIteratorTraversesForwardThenBackward() {
		final RingBuffer<Integer> rb = new RingBuffer<>(5);
		for (int i = 0; i < 10; ++i) {
			rb.add(i);
		}

		final ListIterator<Integer> it = rb.listIterator();
		final List<Integer> forward = new ArrayList<>();
		while (it.hasNext()) {
			forward.add(it.next());
		}
		assertEquals(Arrays.asList(5, 6, 7, 8, 9), forward);

		final List<Integer> backward = new ArrayList<>();
		while (it.hasPrevious()) {
			backward.add(it.previous());
		}
		assertEquals(Arrays.asList(9, 8, 7, 6, 5), backward);
	}

	@Test
	void listIteratorAtLogicalIndexStartsThere() {
		final RingBuffer<Integer> rb = new RingBuffer<>(5);
		for (int i = 0; i < 10; ++i) {
			rb.add(i);
		}

		final ListIterator<Integer> it = rb.listIterator(8);
		final List<Integer> forward = new ArrayList<>();
		while (it.hasNext()) {
			forward.add(it.next());
		}
		assertEquals(Arrays.asList(8, 9), forward);
	}

	@Test
	void addAtIndexShiftsAndWraps() {
		final RingBuffer<Integer> rb = new RingBuffer<>(5);
		for (int i = 0; i < 10; ++i) {
			rb.add(i);
		}
		rb.add(8, 20);
		assertEquals(5, rb.size());
		assertEquals(11, rb.nextIndex());
		assertEquals(Arrays.asList(9, 6, 7, 20, 8), unordered(rb));
		assertEquals(Arrays.asList(6, 7, 20, 8, 9), ordered(rb));
	}

	@Test
	void clearResetsBuffer() {
		final RingBuffer<Integer> rb = new RingBuffer<>(5);
		for (int i = 0; i < 10; ++i) {
			rb.add(i);
		}
		rb.clear();
		assertEquals(0, rb.size());
		assertEquals(0, rb.nextIndex());
		assertEquals(java.util.Collections.<Integer>emptyList(), unordered(rb));
		assertEquals(java.util.Collections.<Integer>emptyList(), ordered(rb));
	}

	@Test
	void addAtIndexThenAppendAndRemove() {
		final RingBuffer<Integer> rb = new RingBuffer<>(5);
		rb.add(100);
		assertEquals(Collections.singletonList(100), ordered(rb));
		assertEquals(1, rb.nextIndex());

		rb.add(0, 20);
		assertEquals(Arrays.asList(20, 100), ordered(rb));
		assertEquals(Arrays.asList(20, 100), unordered(rb));
		assertEquals(2, rb.nextIndex());

		rb.add(3);
		assertEquals(Arrays.asList(20, 100, 3), ordered(rb));
		assertEquals(3, rb.nextIndex());

		final Integer removed = rb.remove(1);
		assertEquals(Integer.valueOf(100), removed);
		assertEquals(Arrays.asList(20, 3), ordered(rb));
		assertEquals(Arrays.asList(20, 3), unordered(rb));
		assertEquals(2, rb.size());
		assertEquals(2, rb.nextIndex());
	}
}
