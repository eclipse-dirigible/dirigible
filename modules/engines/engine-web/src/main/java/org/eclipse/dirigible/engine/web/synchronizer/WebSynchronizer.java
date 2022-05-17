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
package org.eclipse.dirigible.engine.web.synchronizer;

import static java.text.MessageFormat.format;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.core.scheduler.api.AbstractSynchronizer;
import org.eclipse.dirigible.core.scheduler.api.ISynchronizerArtefactType.ArtefactState;
import org.eclipse.dirigible.core.scheduler.api.SchedulerException;
import org.eclipse.dirigible.core.scheduler.api.SynchronizationException;
import org.eclipse.dirigible.engine.web.api.IWebCoreService;
import org.eclipse.dirigible.engine.web.api.WebCoreException;
import org.eclipse.dirigible.engine.web.artefacts.WebSynchronizationArtefactType;
import org.eclipse.dirigible.engine.web.models.WebModel;
import org.eclipse.dirigible.engine.web.processor.WebExposureManager;
import org.eclipse.dirigible.engine.web.service.WebCoreService;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class WebSynchronizer.
 */
public class WebSynchronizer extends AbstractSynchronizer {

	private static final Logger logger = LoggerFactory.getLogger(WebSynchronizer.class);

	private static final Map<String, WebModel> WEB_PREDELIVERED = Collections.synchronizedMap(new HashMap<String, WebModel>());

	private static final List<String> WEB_SYNCHRONIZED = Collections.synchronizedList(new ArrayList<String>());

	private WebCoreService webCoreService = new WebCoreService();

	private final String SYNCHRONIZER_NAME = this.getClass().getCanonicalName();

	private static final WebSynchronizationArtefactType WEB_ARTEFACT = new WebSynchronizationArtefactType();

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.core.scheduler.api.ISynchronizer#synchronize()
	 */
	@Override
	public void synchronize() {
		synchronized (WebSynchronizer.class) {
			if (beforeSynchronizing()) {
				logger.trace("Synchronizing Web...");
				try {
					if (isSynchronizationEnabled()) {
						startSynchronization(SYNCHRONIZER_NAME);
						clearCache();
						synchronizePredelivered();
						synchronizeRegistry();
						updateWebExposures();
						int immutableCount = WEB_PREDELIVERED.size();
						int mutableCount = WEB_SYNCHRONIZED.size();
						cleanup();
						clearCache();
						successfulSynchronization(SYNCHRONIZER_NAME, format("Immutable: {0}, Mutable: {1}", immutableCount, mutableCount));
					} else {
						logger.debug("Synchronization has been disabled");
					}
				} catch (Exception e) {
					logger.error("Synchronizing process for Web failed.", e);
					try {
						failedSynchronization(SYNCHRONIZER_NAME, e.getMessage());
					} catch (SchedulerException e1) {
						logger.error("Synchronizing process for Web files failed in registering the state log.", e);
					}
				}
				logger.trace("Done synchronizing Webs.");
				afterSynchronizing();
			}
		}
	}

	/**
	 * Force synchronization.
	 */
	public static final void forceSynchronization() {
		WebSynchronizer synchronizer = new WebSynchronizer();
		synchronizer.setForcedSynchronization(true);
		try {
			synchronizer.synchronize();
		} finally {
			synchronizer.setForcedSynchronization(false);
		}
	}

	/**
	 * Register predelivered web.
	 *
	 * @param webPath
	 *            the web path
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void registerPredeliveredWeb(String webPath) throws IOException {
		InputStream in = WebSynchronizer.class.getResourceAsStream("/META-INF/dirigible" + webPath);
		try {
			String json = IOUtils.toString(in, StandardCharsets.UTF_8);
			WebModel webModel = webCoreService.parseWeb(webPath, json);
			WEB_PREDELIVERED.put(webPath, webModel);
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.core.scheduler.api.AbstractSynchronizer#synchronizeRegistry()
	 */
	@Override
	protected void synchronizeRegistry() throws SynchronizationException {
		logger.trace("Synchronizing Webs from Registry...");

		super.synchronizeRegistry();

		logger.trace("Done synchronizing Webs from Registry.");
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.core.scheduler.api.AbstractSynchronizer#synchronizeResource(org.eclipse.dirigible.
	 * repository.api.IResource)
	 */
	@Override
	protected void synchronizeResource(IResource resource) throws SynchronizationException {
		String resourceName = resource.getName();
		if (resourceName.equals(IWebCoreService.FILE_PROJECT_JSON)) {
			String path = getRegistryPath(resource);
			WebModel webModel = webCoreService.parseWeb(path, resource.getContent());
			synchronizeWeb(webModel);
		}

	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.core.scheduler.api.AbstractSynchronizer#cleanup()
	 */
	@Override
	protected void cleanup() throws SynchronizationException {
		logger.trace("Cleaning up Webs...");
		super.cleanup();

		try {
			List<WebModel> webModels = webCoreService.getWebs();
			for (WebModel webModel : webModels) {
				if (!WEB_SYNCHRONIZED.contains(webModel.getGuid())) {
					webCoreService.removeWeb(webModel.getGuid());
					logger.warn("Cleaned up Web [{}]", webModel.getGuid());
				}
			}
		} catch (WebCoreException e) {
			throw new SynchronizationException(e);
		}

		logger.trace("Done cleaning up Webs.");
	}

	private void updateWebExposures() throws SchedulerException {
		logger.trace("Start Web Registering...");

		for (String webName : WEB_SYNCHRONIZED) {
			if (!WebExposureManager.existExposableProject(webName)) {
				WebModel webModel = null;
				try {
					webModel = webCoreService.getWebByName(webName);
					if (webModel.getExposes() != null) {
						WebExposureManager.registerExposableProject(webName, webModel.getExposes());
						applyArtefactState(webModel, WEB_ARTEFACT, ArtefactState.SUCCESSFUL_CREATE);
					} else {
						logger.trace(webName + " skipped due to lack of exposures");
					}
				} catch (WebCoreException e) {
					logger.error(e.getMessage(), e);
					applyArtefactState(webModel, WEB_ARTEFACT, ArtefactState.FAILED_CREATE, e.getMessage());
				}
			}
		}

		Set<String> registerdProjects = WebExposureManager.listRegisteredProjects();
		for (String registeredProject : registerdProjects) {
			WebModel webModel = null;
			try {
				if (!WEB_SYNCHRONIZED.contains(registeredProject)) {
					webModel = new WebModel();
					webModel.setLocation(IRepository.SEPARATOR + registeredProject + IRepository.SEPARATOR + "project.json");
					webModel.setGuid(registeredProject);
					webCoreService.removeWeb(webModel.getLocation());
					WebExposureManager.unregisterProject(registeredProject);
					applyArtefactState(webModel, WEB_ARTEFACT, ArtefactState.SUCCESSFUL_DELETE);
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				applyArtefactState(webModel, WEB_ARTEFACT, ArtefactState.FAILED_DELETE, e.getMessage());
			}
		}

		logger.trace("Registered Projects: " + registerdProjects.size());
		logger.trace("Done registering Projects.");
	}

	private void clearCache() {
		WEB_SYNCHRONIZED.clear();
	}

	private void synchronizePredelivered() throws SynchronizationException {
		logger.trace("Synchronizing predelivered Webs...");
		// Webs
		for (WebModel webModel : WEB_PREDELIVERED.values()) {
			synchronizeWeb(webModel);
		}
		logger.trace("Done synchronizing predelivered Jobs.");
	}

	private void synchronizeWeb(WebModel webModel) throws SynchronizationException {
		try {
			if (!webCoreService.existsWeb(webModel.getLocation())) {
				webCoreService.createWeb(webModel.getLocation(), webModel.getGuid(), webModel.getExposed(), webModel.getHash());
				logger.info("Synchronized a new Web [{}]", webModel.getLocation());
			} else {
				WebModel existing = webCoreService.getWeb(webModel.getLocation());
				if (!webModel.equals(existing)) {
					webCoreService.updateWeb(webModel.getLocation(), webModel.getGuid(), webModel.getExposed(), webModel.getHash());
					logger.info("Synchronized a modified Web [{}]", webModel.getLocation());
				}
			}
			WEB_SYNCHRONIZED.add(webModel.getGuid());
		} catch (WebCoreException e) {
			throw new SynchronizationException(e);
		}
	}

}
