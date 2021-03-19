// Copyright (c) 2020 Alex Blewitt, Bandlem Limited
//
// Released under the MIT License SPDX:MIT
// See LICENSE.txt for more information

package com.bandlem.osgi.jfrbundlelistener;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;

/**
 * A synchronous bundle listener that logs {@link JFRBundleEvent} when bundles
 * move into the {@link BundleEvent#STARTED} state.
 *
 * The duration of the event is between when the bundle moves into the
 * {@link BundleEvent#STARTING} state until it finishes in the
 * {@link BundleEvent#STARTED} state. Bundles that never complete started state
 * will not be logged. Bundles that are already in a starting state or already
 * started are not logged by this listener.
 *
 * To ensure that it records bundles, mark it as an early startup using an OSGi
 * start level and an auto-start directive where applicable.
 *
 * This is marked as a {@link SynchronousBundleListener} so that the start and
 * end of the bundle startup can be recorded accurately. If they were
 * asynchronous, then the start and end times could be skewed leading to
 * inaccurate results.
 *
 * @author Alex Blewitt
 */
public class JFRBundleListener implements SynchronousBundleListener {

	static final int MAX_BUNDLES = Integer.getInteger(JFRBundleListener.class.getName() + ".max", 1000);
	final JFRBundleEvent[] events = new JFRBundleEvent[MAX_BUNDLES];

	/**
	 * Called when a bundle changes state.
	 *
	 * When a bundle moves into the {@link BundleEvent#STARTING} state, the JFR
	 * event begins. When the bundle moves into the {@link BundleEvent#STARTED}
	 * state, the JFR event ends and is committed.
	 *
	 * Events are recorded into an array of bundles, with a fixed size of 1000 or
	 * the value of {@code com.bandlem.osgi.jfrbundlelistener.max} specified as
	 * a property. This is an array for fast fixed-width lookup. A future
	 * implementation might use a different backing store, in which case this
	 * restriction could be lifted.
	 */
	@Override
	public void bundleChanged(BundleEvent event) {
		Bundle bundle = event.getBundle();
		int id = (int) bundle.getBundleId();
		int type = event.getType();

		JFRBundleEvent jfr = events[id];
		if (jfr == null) {
			jfr = new JFRBundleEvent(event);
			events[id] = jfr;
		}
		if (type == BundleEvent.STARTING) {
			jfr.begin();
		} else if (type == BundleEvent.STARTED) {
			jfr.end();
			jfr.commit();
		}
	}
}
