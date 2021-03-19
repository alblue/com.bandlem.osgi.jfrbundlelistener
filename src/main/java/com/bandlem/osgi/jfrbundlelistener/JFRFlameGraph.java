// Copyright (c) 2020 Alex Blewitt, Bandlem Limited
//
// Released under the MIT License SPDX:MIT
// See LICENSE.txt for more information

package com.bandlem.osgi.jfrbundlelistener;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.SortedSet;
import java.util.TreeSet;

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

/**
 * Used to process a JFR dump into a format suitable for processing with Brendan
 * Gregg's FlameGraph software.
 *
 * Usage: java JFRFlameGraph [infile] [outfile]
 *
 * Defaults to stdin and stdout if not specified.
 *
 * The post-processing step can be executed by cloning:
 *
 * https://github.com/brendangregg/FlameGraph
 *
 * followed by executing
 * <pre>
 * ./flamegraph.pl --countname ms --hash &lt; [outfile].txt &gt; [outfile].svg
 * </pre>
 * @author Alex Blewitt
 */
public class JFRFlameGraph {
	/**
	 * Given an input JFR file, writes a consolidated FlameGraph stack trace file.
	 *
	 * @param args [infile] [outfile]
	 * @throws IOException if an I/O error occurs during processing
	 */
	public static void main(String[] args) throws IOException {
		String in = args.length > 0 ? args[0] : "/dev/stdin";
		String out = args.length > 1 ? args[1] : "/dev/stdout";
		writeFlameEvents(readRecording(Paths.get(in)), new File(out));
	}

	/**
	 * Process a flight recording, looking for {@link JFRBundleEvent} events,
	 * extracting the {@code Bundle-SymbolicName} and returning a set of
	 * {@link FlameEvent}.
	 *
	 * The returned FlameEvent objects are sorted by start time, so the earliest
	 * event from the set will be the one with the lowest start time.
	 *
	 * @param path the path of the recording
	 * @return the set of the {@link FlameEvent} objects.
	 * @throws IOException if an I/O error occurs during reading
	 */
	private static SortedSet<FlameEvent> readRecording(Path path) throws IOException {
		TreeSet<FlameEvent> events = new TreeSet<>();
		try (RecordingFile recordingFile = new RecordingFile(path)) {
			while (recordingFile.hasMoreEvents()) {
				RecordedEvent event = recordingFile.readEvent();
				String type = event.getEventType().getName();
				if (type.startsWith(JFRBundleEvent.class.getName())) {
					String name = event.getString("Bundle-SymbolicName");
					if (name != null) {
						events.add(new FlameEvent(event, name));
					}
				}
			}
		}
		return events;
	}

	/**
	 * Given a stack of {@link FlameEvent} objects, write out a single line to the
	 * output with a concatenated list of messages and concluding with the line.
	 *
	 * The output of this will look like:
	 *
	 * main;msg1;msg2;msg3 1234
	 *
	 * This output can be consumed by FlameGraph to process into a flame graph
	 *
	 * @param stack the stack of FlameEvent objects
	 * @param out   the output to write to
	 * @throws IOException if an I/O error occurs during writing
	 */
	private static void write(Deque<FlameEvent> stack, Writer out) throws IOException {
		FlameEvent top = stack.peek();
		Duration duration = top.getDuration();
		StringBuilder builder = new StringBuilder(top.getThreadName());
		for (FlameEvent flameEvent : stack) {
			builder.append(';');
			builder.append(flameEvent.getMessage());
		}
		builder.append(' ');
		builder.append(duration.toMillis());
		builder.append('\n');
		out.write(builder.toString());
	}

	/**
	 * Write all stack elements that occurred before a particular time, so that we
	 * have a moving cut-off filter of events.
	 *
	 * @param stack  the stack of FlameEvent objects
	 * @param before the cut-off time
	 * @param out    the writer to write to
	 * @throws IOException if an I/O error occurs during writing
	 */
	private static void writeBefore(Deque<FlameEvent> stack, Instant before, Writer out) throws IOException {
		if (stack.isEmpty()) {
			return;
		}
		if (stack.peek().getEndTime().compareTo(before) < 0) {
			write(stack, out);
			stack.pop();
			writeBefore(stack, before, out);
		}
	}

	/**
	 * Given a sorted set of FlameEvent objects, write them out to the output file
	 * on a line by line basis.
	 *
	 * @param events     all flame events
	 * @param outputFile the output file to write to
	 * @throws IOException if an I/O error occurs during writing
	 */
	private static void writeFlameEvents(SortedSet<FlameEvent> events, File outputFile) throws IOException {
		try (Writer out = new FileWriter(outputFile)) {
			Deque<FlameEvent> stack = new ArrayDeque<>();
			for (FlameEvent event :  events) {
				writeBefore(stack, event.getStartTime(), out);
				if (!stack.isEmpty()) {
					stack.peek().adjust(event.getDuration());
				}
				stack.push(event);
			}
			writeBefore(stack, Instant.MAX, out);
			out.flush();
		}
	}
}
