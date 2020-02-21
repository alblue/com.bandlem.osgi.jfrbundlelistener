// Copyright (c) 2020 Alex Blewitt, Bandlem Limited
//
// Released under the MIT License SPDX:MIT
// See LICENSE.txt for more information

package com.bandlem.osgi.jfrbundlelistener;

import java.time.Duration;
import java.time.Instant;

import jdk.jfr.consumer.RecordedEvent;

/**
 * Wraps a {@link RecordedEvent} so that durations can be adjusted for nested
 * startup events.
 * 
 * Provides delegated access to the underlying event. Contains an associated
 * message which can be used for displaying on a flame graph.
 * 
 * @author Alex Blewitt
 */
public class FlameEvent implements Comparable<FlameEvent> {

	// The wrapped recorded event
	private final RecordedEvent event;
	// The message for displaying in a flame graph
	private final String message;
	// Any adjusted duration for nested event types
	private Duration adjustment = Duration.ZERO;

	/**
	 * Create a new FlameEvent with a wrapped RecordedEvent and message.
	 * 
	 * @param event   the JFR recorded event to wrap
	 * @param message the message to display in a flame graph
	 */
	public FlameEvent(RecordedEvent event, String message) {
		this.event = event;
		this.message = message;
	}

	/**
	 * Adjust the duration reported by the given amount.
	 * 
	 * If event A subsumes event B, and event B takes 1s while event A takes 1.5s,
	 * we want to report A's time as 0.5s. So when processing nested events, we
	 * adjust A-&gt;B's time by -1s, so that A's total time is correct.
	 * 
	 * @param duration the duration to adjust by
	 */
	public void adjust(Duration duration) {
		this.adjustment = adjustment.plus(duration);
	}

	@Override
	public int compareTo(FlameEvent other) {
		return getStartTime().compareTo(other.getStartTime());
	}

	/**
	 * Returns the adjusted duration for the event, or the event's whole duration if
	 * the adjustment is zero.
	 * 
	 * @return the adjusted duration
	 */
	public Duration getDuration() {
		Duration adjustedDuration = event.getDuration().minus(adjustment);
		return adjustedDuration.isNegative() ? Duration.ZERO : adjustedDuration;
	}

	/**
	 * Delegates to {@link RecordedEvent#getEndTime}
	 * 
	 * @return the end time of the event
	 */
	public Instant getEndTime() {
		return event.getEndTime();
	}

	public String getMessage() {
		return message;
	}

	/**
	 * Delegates to {@link RecordedEvent#getStartTime}
	 * 
	 * @return the start time of the event
	 */
	public Instant getStartTime() {
		return event.getStartTime();
	}

	/**
	 * Delegates to {@link RecordedEvent#getThread}
	 * 
	 * @return the thread name as reported by the stack trace
	 */
	public String getThreadName() {
		return event.getThread().getJavaName().replace(' ', '-');
	}
}