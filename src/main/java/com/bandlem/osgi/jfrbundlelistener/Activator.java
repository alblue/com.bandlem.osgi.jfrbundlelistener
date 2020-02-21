// Copyright (c) 2020 Alex Blewitt, Bandlem Limited
//
// Released under the MIT License SPDX:MIT
// See LICENSE.txt for more information

package com.bandlem.osgi.jfrbundlelistener;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Hooks in to the OSGi framework listening for bundle startup events in order
 * to generate JFR events. Registers {@link JFRBundleListener} to subscribe to
 * OSGi bundle listener events synchronously (so that the correct timing can be
 * determined).
 * 
 * @author Alex Blewitt
 */
public class Activator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		context.addBundleListener(new JFRBundleListener());
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}
}