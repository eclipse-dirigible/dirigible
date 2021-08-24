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
package org.eclipse.dirigible.core.workspace.json;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.fs.FileSystemRepository;
import org.eclipse.dirigible.repository.local.LocalWorkspaceMapper;

/**
 * The Workspace Descriptor transport object.
 */
public class WorkspaceDescriptor {

	private String name;

	private String path;

	private String type = "workspace";

	private List<ProjectDescriptor> projects = new ArrayList<ProjectDescriptor>();

	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name.
	 *
	 * @param name
	 *            the new name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the path.
	 *
	 * @return the path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Sets the path.
	 *
	 * @param path
	 *            the new path
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * Gets the projects.
	 *
	 * @return the projects
	 */
	public List<ProjectDescriptor> getProjects() {
		return projects;
	}

	/**
	 * Sets the.
	 *
	 * @param projects
	 *            the projects
	 */
	public void set(List<ProjectDescriptor> projects) {
		this.projects = projects;
	}
	
	/**
	 * Get the git folder
	 * 
	 * @param repository the repository
	 * @param repositoryPath the path
	 */
	public static File getCanonicalFilePerProjectPath(IRepository repository, String repositoryPath) {
		try {
			if (repository instanceof FileSystemRepository) {
				String path = LocalWorkspaceMapper.getMappedName((FileSystemRepository) repository, repositoryPath);
				return new File(path).getCanonicalFile();
			}
		} catch (Throwable e) {
			return null;
		}
		return null;
	}

}
