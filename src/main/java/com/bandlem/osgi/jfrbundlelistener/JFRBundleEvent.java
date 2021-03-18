// Copyright (c) 2020 Alex Blewitt, Bandlem Limited
//
// Released under the MIT License SPDX:MIT
// See LICENSE.txt for more information

package com.bandlem.osgi.jfrbundlelistener;

import org.osgi.framework.BundleEvent;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;

/**
 * Provides a JFR capable logging event for bundle startup times.
 * @author Alex Blewitt
 */
@Category("OGSi")
@Description("Represents bundle start up times")
@Label("Bundle Event")
public class JFRBundleEvent extends Event {
	@Description("The bundle identifier used by this runtime")
	@Label("Bundle-Id")
	@Name("Bundle-Id")
	public int bundleID;

	@Description("The bundle symbolic name")
	@Label("Bundle-SymbolicName")
	@Name("Bundle-SymbolicName")
	public String bundleSymbolicName;

	@Description("The bundle version")
	@Label("Bundle-Version")
	@Name("Bundle-Version")
	public String version;

	/**
	 * Create a new JFRBundleEvent wrapping a given {@link BundleEvent}.
	 * @param event the bundle event to wrap
	 */
	public JFRBundleEvent(BundleEvent event) {
		bundleID = (int) event.getBundle().getBundleId();
		bundleSymbolicName = event.getBundle().getSymbolicName();
		version = event.getBundle().getVersion().toString();
	}
}
