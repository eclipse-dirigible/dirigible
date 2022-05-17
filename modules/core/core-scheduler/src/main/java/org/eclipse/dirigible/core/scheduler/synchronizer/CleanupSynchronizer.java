/*
 * Copyright (c) 2022 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2022 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.core.scheduler.synchronizer;

import static java.text.MessageFormat.format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

import org.eclipse.dirigible.commons.api.service.ICleanupService;
import org.eclipse.dirigible.core.scheduler.api.AbstractSynchronizer;
import org.eclipse.dirigible.core.scheduler.api.IOrderedSynchronizerContribution;
import org.eclipse.dirigible.core.scheduler.api.SchedulerException;
import org.eclipse.dirigible.core.scheduler.api.SynchronizationException;
import org.eclipse.dirigible.repository.api.IResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The OrderedSynchronizer Synchronizer.
 */
public class CleanupSynchronizer extends AbstractSynchronizer {

	private static final Logger logger = LoggerFactory.getLogger(CleanupSynchronizer.class);
	
	private final String SYNCHRONIZER_NAME = this.getClass().getCanonicalName();
	
	private static final ServiceLoader<ICleanupService> CLEANUP_SERVICES = ServiceLoader.load(ICleanupService.class);
		
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.core.scheduler.api.ISynchronizer#synchronize()
	 */
	@Override
	public void synchronize() {
		synchronized (CleanupSynchronizer.class) {
			if (beforeSynchronizing()) {
				logger.trace("Synchronizing Cleanup Synchronizers...");
				try {
					if (isSynchronizationEnabled()) {
						startSynchronization(SYNCHRONIZER_NAME);
						
						for (ICleanupService next : CLEANUP_SERVICES) {
							try {
								next.cleanup();
							} catch (Exception e) {
								logger.error(format("Error during cleaning up the: [{0}]. Skipped due to an error: {1}", next.getClass().getCanonicalName(), e.getMessage()), e);
							}
						}
												
						successfulSynchronization(SYNCHRONIZER_NAME, "Details in the previous log messages");
					} else {
						logger.debug("Synchronization has been disabled");
					}
				} catch (Exception e) {
					logger.error("Synchronizing process for Cleanup Synchronizers failed.", e);
					try {
						failedSynchronization(SYNCHRONIZER_NAME, e.getMessage());
					} catch (SchedulerException e1) {
						logger.error("Synchronizing process for Cleanup Synchronizers failed in registering the state log.", e);
					}
				}
				logger.trace("Done synchronizing Cleanup Synchronizers.");
				afterSynchronizing();
			}
		}
	}

	/**
	 * Force synchronization.
	 */
	public static final void forceSynchronization() {
		CleanupSynchronizer synchronizer = new CleanupSynchronizer();
		synchronizer.setForcedSynchronization(true);
		try {
			synchronizer.synchronize();
		} finally {
			synchronizer.setForcedSynchronization(false);
		}
	}

	@Override
	protected void synchronizeResource(IResource resource) throws SynchronizationException {
		// TODO Auto-generated method stub
		
	}

}
