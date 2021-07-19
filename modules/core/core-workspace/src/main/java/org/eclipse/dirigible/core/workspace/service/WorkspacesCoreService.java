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
package org.eclipse.dirigible.core.workspace.service;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.dirigible.api.v3.security.UserFacade;
import org.eclipse.dirigible.core.workspace.api.IWorkspace;
import org.eclipse.dirigible.core.workspace.api.IWorkspacesCoreService;
import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Workspaces Core Service.
 */
@Singleton
public class WorkspacesCoreService implements IWorkspacesCoreService {
	
	private static final Logger logger = LoggerFactory.getLogger(WorkspacesCoreService.class);

	/** The Constant DEFAULT_WORKSPACE_NAME. */
	private static final String DEFAULT_WORKSPACE_NAME = "workspace";

	@Inject
	private IRepository repository;

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.core.workspace.api.IWorkspacesCoreService#createWorkspace(java.lang.String)
	 */
	@Override
	public IWorkspace createWorkspace(String name) {
		ICollection collection = getWorkspace(name);
		collection.create();
		logger.info("Workspace created [{}]", collection.getPath());
		return new Workspace(collection);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.core.workspace.api.IWorkspacesCoreService#getWorkspace(java.lang.String)
	 */
	@Override
	public IWorkspace getWorkspace(String name) {
		StringBuilder workspacePath = generateWorkspacePath(name, null, null);
		ICollection collection = repository.getCollection(workspacePath.toString());
		return new Workspace(collection);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.core.workspace.api.IWorkspacesCoreService#getWorkspaces()
	 */
	@Override
	public List<IWorkspace> getWorkspaces() {
		StringBuilder workspacePath = generateWorkspacePath(null, null, null);
		ICollection root = repository.getCollection(workspacePath.toString());
		List<IWorkspace> workspaces = new ArrayList<IWorkspace>();
		if (!root.exists()) {
			root.create();
		}
		List<ICollection> collections = root.getCollections();
		for (ICollection collection : collections) {
			workspaces.add(new Workspace(collection));
		}
		if (workspaces.isEmpty()) {
			ICollection collection = root.createCollection(DEFAULT_WORKSPACE_NAME);
			workspaces.add(new Workspace(collection));
		}
		return workspaces;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.core.workspace.api.IWorkspacesCoreService#deleteWorkspace(java.lang.String)
	 */
	@Override
	public void deleteWorkspace(String name) {
		ICollection collection = getWorkspace(name);
		collection.delete();
		logger.info("Workspace deleted [{}]", collection.getPath());
	}

	/**
	 * Generate workspace path.
	 *
	 * @param workspace
	 *            the workspace
	 * @param project
	 *            the project
	 * @param path
	 *            the path
	 * @return the string builder
	 */
	private StringBuilder generateWorkspacePath(String workspace, String project, String path) {
		StringBuilder relativePath = new StringBuilder(IRepositoryStructure.PATH_USERS).append(IRepositoryStructure.SEPARATOR)
				.append(UserFacade.getName());
		if (workspace != null) {
			relativePath.append(IRepositoryStructure.SEPARATOR).append(workspace);
		}
		if (project != null) {
			relativePath.append(IRepositoryStructure.SEPARATOR).append(project);
		}
		if (path != null) {
			relativePath.append(IRepositoryStructure.SEPARATOR).append(path);
		}
		return relativePath;
	}

}
