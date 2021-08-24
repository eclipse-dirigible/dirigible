/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.core.extensions.synchronizer;

import static java.text.MessageFormat.format;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.core.extensions.api.ExtensionsException;
import org.eclipse.dirigible.core.extensions.api.IExtensionsCoreService;
import org.eclipse.dirigible.core.extensions.definition.ExtensionDefinition;
import org.eclipse.dirigible.core.extensions.definition.ExtensionPointDefinition;
import org.eclipse.dirigible.core.extensions.service.ExtensionsCoreService;
import org.eclipse.dirigible.core.scheduler.api.AbstractSynchronizer;
import org.eclipse.dirigible.core.scheduler.api.SchedulerException;
import org.eclipse.dirigible.core.scheduler.api.SynchronizationException;
import org.eclipse.dirigible.repository.api.IResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class ExtensionsSynchronizer.
 */
public class ExtensionsSynchronizer extends AbstractSynchronizer {

	private static final Logger logger = LoggerFactory.getLogger(ExtensionsSynchronizer.class);

	private static final Map<String, ExtensionPointDefinition> EXTENSION_POINTS_PREDELIVERED = Collections
			.synchronizedMap(new HashMap<String, ExtensionPointDefinition>());

	private static final Map<String, ExtensionDefinition> EXTENSIONS_PREDELIVERED = Collections
			.synchronizedMap(new HashMap<String, ExtensionDefinition>());

	private static final List<String> EXTENSION_POINTS_SYNCHRONIZED = Collections.synchronizedList(new ArrayList<String>());

	private static final List<String> EXTENSIONS_SYNCHRONIZED = Collections.synchronizedList(new ArrayList<String>());

	private ExtensionsCoreService extensionsCoreService = new ExtensionsCoreService();
	
	private final String SYNCHRONIZER_NAME = this.getClass().getCanonicalName();
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.core.scheduler.api.ISynchronizer#synchronize()
	 */
	@Override
	public void synchronize() {
		synchronized (ExtensionsSynchronizer.class) {
			if (beforeSynchronizing()) {
				logger.trace("Synchronizing Extension Points and Extensions...");
				try {
					startSynchronization(SYNCHRONIZER_NAME);
					clearCache();
					synchronizePredelivered();
					synchronizeRegistry();
					int immutableExtensionPointsCount = EXTENSION_POINTS_PREDELIVERED.size();
					int immutableExtensionsCount = EXTENSIONS_PREDELIVERED.size();
					int mutableExtensionPointsCount = EXTENSION_POINTS_SYNCHRONIZED.size();
					int mutableExtensionsCount = EXTENSIONS_SYNCHRONIZED.size();
					cleanup();
					clearCache();
					successfulSynchronization(SYNCHRONIZER_NAME, format("Immutable Extension Points: {0}, Immutable Extensions: {1}, Mutable Extension Points: {2}, Mutable Extensions: {3}", 
							immutableExtensionPointsCount, immutableExtensionsCount, mutableExtensionPointsCount, mutableExtensionsCount));
				} catch (Exception e) {
					logger.error("Synchronizing process for Extension Points and Extensions failed.", e);
					try {
						failedSynchronization(SYNCHRONIZER_NAME, e.getMessage());
					} catch (SchedulerException e1) {
						logger.error("Synchronizing process for Extension Points and Extensions files failed in registering the state log.", e);
					}
				}
				logger.trace("Done synchronizing Extension Points and Extensions.");
				afterSynchronizing();
			}
		}
	}

	/**
	 * Force synchronization.
	 */
	public static final void forceSynchronization() {
		ExtensionsSynchronizer synchronizer = new ExtensionsSynchronizer();
		synchronizer.setForcedSynchronization(true);
		try {
			synchronizer.synchronize();
		} finally {
			synchronizer.setForcedSynchronization(false);
		}
	}

	/**
	 * Register pre-delivered extension point.
	 *
	 * @param extensionPointPath
	 *            the extension point path
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void registerPredeliveredExtensionPoint(String extensionPointPath) throws IOException {
		InputStream in = ExtensionsSynchronizer.class.getResourceAsStream("/META-INF/dirigible" + extensionPointPath);
		try {
			String json = IOUtils.toString(in, StandardCharsets.UTF_8);
			ExtensionPointDefinition extensionPointDefinition = extensionsCoreService.parseExtensionPoint(json);
			extensionPointDefinition.setLocation(extensionPointPath);
			EXTENSION_POINTS_PREDELIVERED.put(extensionPointPath, extensionPointDefinition);
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	/**
	 * Register pre-delivered extension.
	 *
	 * @param extensionPath
	 *            the extension path
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void registerPredeliveredExtension(String extensionPath) throws IOException {
		InputStream in = ExtensionsSynchronizer.class.getResourceAsStream("/META-INF/dirigible" + extensionPath);
		try {
			String json = IOUtils.toString(in, StandardCharsets.UTF_8);
			ExtensionDefinition extensionDefinition = extensionsCoreService.parseExtension(json);
			extensionDefinition.setLocation(extensionPath);
			EXTENSIONS_PREDELIVERED.put(extensionPath, extensionDefinition);
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	private void clearCache() {
		EXTENSION_POINTS_SYNCHRONIZED.clear();
		EXTENSIONS_SYNCHRONIZED.clear();
	}

	private void synchronizePredelivered() throws SynchronizationException {
		logger.trace("Synchronizing predelivered Extension Points and Extensions...");
		// Extension Points
		for (ExtensionPointDefinition extensionPointDefinition : EXTENSION_POINTS_PREDELIVERED.values()) {
			synchronizeExtensionPoint(extensionPointDefinition);
		}
		// Extensions
		for (ExtensionDefinition extensionDefinition : EXTENSIONS_PREDELIVERED.values()) {
			synchronizeExtension(extensionDefinition);
		}
		logger.trace("Done synchronizing predelivered Extension Points and Extensions.");
	}

	private void synchronizeExtensionPoint(ExtensionPointDefinition extensionPointDefinition) throws SynchronizationException {
		try {
			if (!extensionsCoreService.existsExtensionPoint(extensionPointDefinition.getLocation())) {
				extensionsCoreService.createExtensionPoint(extensionPointDefinition.getLocation(), extensionPointDefinition.getName(),
						extensionPointDefinition.getDescription());
				logger.info("Synchronized a new Extension Point [{}] from location: {}", extensionPointDefinition.getName(),
						extensionPointDefinition.getLocation());
			} else {
				ExtensionPointDefinition existing = extensionsCoreService.getExtensionPoint(extensionPointDefinition.getLocation());
				if (!extensionPointDefinition.equals(existing)) {
					extensionsCoreService.updateExtensionPoint(extensionPointDefinition.getLocation(), extensionPointDefinition.getName(),
							extensionPointDefinition.getDescription());
					logger.info("Synchronized a modified Extension Point [{}] from location: {}", extensionPointDefinition.getName(),
							extensionPointDefinition.getLocation());
				}
			}
			EXTENSION_POINTS_SYNCHRONIZED.add(extensionPointDefinition.getLocation());
		} catch (ExtensionsException e) {
			throw new SynchronizationException(e);
		}
	}

	private void synchronizeExtension(ExtensionDefinition extensionDefinition) throws SynchronizationException {
		try {
			if (!extensionsCoreService.existsExtension(extensionDefinition.getLocation())) {
				extensionsCoreService.createExtension(extensionDefinition.getLocation(), extensionDefinition.getModule(),
						extensionDefinition.getExtensionPoint(), extensionDefinition.getDescription());
				logger.info("Synchronized a new Extension [{}] for Extension Point [{}] from location: {}", extensionDefinition.getModule(),
						extensionDefinition.getExtensionPoint(), extensionDefinition.getLocation());
			} else {
				ExtensionDefinition existing = extensionsCoreService.getExtension(extensionDefinition.getLocation());
				if (!extensionDefinition.equals(existing)) {
					extensionsCoreService.updateExtension(extensionDefinition.getLocation(), extensionDefinition.getModule(),
							extensionDefinition.getExtensionPoint(), extensionDefinition.getDescription());
					logger.info("Synchronized a modified Extension [{}] for Extension Point [{}] from location: {}", extensionDefinition.getModule(),
							extensionDefinition.getExtensionPoint(), extensionDefinition.getLocation());
				}
			}
			EXTENSIONS_SYNCHRONIZED.add(extensionDefinition.getLocation());
		} catch (ExtensionsException e) {
			throw new SynchronizationException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.core.scheduler.api.AbstractSynchronizer#synchronizeRegistry()
	 */
	@Override
	protected void synchronizeRegistry() throws SynchronizationException {
		logger.trace("Synchronizing Extension Points and Extensions from Registry...");

		super.synchronizeRegistry();

		logger.trace("Done synchronizing Extension Points and Extensions from Registry.");
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.core.scheduler.api.AbstractSynchronizer#synchronizeResource(org.eclipse.dirigible.
	 * repository.api.IResource)
	 */
	@Override
	protected void synchronizeResource(IResource resource) throws SynchronizationException {
		String resourceName = resource.getName();
		if (resourceName.endsWith(IExtensionsCoreService.FILE_EXTENSION_EXTENSIONPOINT)) {
			ExtensionPointDefinition extensionPointDefinition = extensionsCoreService.parseExtensionPoint(resource.getContent());

			extensionPointDefinition.setLocation(getRegistryPath(resource));
			synchronizeExtensionPoint(extensionPointDefinition);
		}

		if (resourceName.endsWith(IExtensionsCoreService.FILE_EXTENSION_EXTENSION)) {
			ExtensionDefinition extensionDefinition = extensionsCoreService.parseExtension(resource.getContent());
			extensionDefinition.setLocation(getRegistryPath(resource));
			synchronizeExtension(extensionDefinition);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.core.scheduler.api.AbstractSynchronizer#cleanup()
	 */
	@Override
	protected void cleanup() throws SynchronizationException {
		logger.trace("Cleaning up Extension Points and Extensions...");
		super.cleanup();

		try {
			List<ExtensionPointDefinition> extensionPointDefinitions = extensionsCoreService.getExtensionPoints();
			for (ExtensionPointDefinition extensionPointDefinition : extensionPointDefinitions) {
				if (!EXTENSION_POINTS_SYNCHRONIZED.contains(extensionPointDefinition.getLocation())) {
					extensionsCoreService.removeExtensionPoint(extensionPointDefinition.getLocation());
					logger.warn("Cleaned up Extension Point [{}] from location: {}", extensionPointDefinition.getName(),
							extensionPointDefinition.getLocation());
				}
			}

			List<ExtensionDefinition> extensionDefinitions = extensionsCoreService.getExtensions();
			for (ExtensionDefinition extensionDefinition : extensionDefinitions) {
				if (!EXTENSIONS_SYNCHRONIZED.contains(extensionDefinition.getLocation())) {
					extensionsCoreService.removeExtension(extensionDefinition.getLocation());
					logger.warn("Cleaned up Extension for Module [{}] from location: {}", extensionDefinition.getModule(),
							extensionDefinition.getLocation());
				}
			}
		} catch (ExtensionsException e) {
			throw new SynchronizationException(e);
		}

		logger.trace("Done cleaning up Extension Points and Extensions.");
	}
}
