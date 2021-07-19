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
package org.eclipse.dirigible.core.git.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import javax.inject.Inject;

import org.eclipse.dirigible.core.git.GitConnectorException;
import org.eclipse.dirigible.core.git.IGitConnector;
import org.eclipse.dirigible.core.git.command.CloneCommand;
import org.eclipse.dirigible.core.test.AbstractGuiceTest;
import org.eclipse.dirigible.core.workspace.api.IFile;
import org.eclipse.dirigible.core.workspace.api.IFolder;
import org.eclipse.dirigible.core.workspace.api.IProject;
import org.eclipse.dirigible.core.workspace.api.IWorkspace;
import org.eclipse.dirigible.core.workspace.api.IWorkspacesCoreService;
import org.eclipse.dirigible.core.workspace.service.WorkspacesCoreService;
import org.junit.Before;
import org.junit.Test;

/**
 * The Class CloneComandTest.
 */
public class CloneComandTest extends AbstractGuiceTest {

	/** The clone command. */
	@Inject
	private CloneCommand cloneCommand;

	/** The workspaces core service. */
	@Inject
	private IWorkspacesCoreService workspacesCoreService;

	/**
	 * Sets the up.
	 *
	 * @throws Exception the exception
	 */
	@Before
	public void setUp() throws Exception {
		this.cloneCommand = getInjector().getInstance(CloneCommand.class);
		this.workspacesCoreService = getInjector().getInstance(WorkspacesCoreService.class);
	}

	/**
	 * Creates the workspace test.
	 *
	 * @throws GitConnectorException the git connector exception
	 */
	@Test
	public void createWorkspaceTest() throws GitConnectorException {
		String gitEnabled = System.getenv(GitConnectorTest.DIRIGIBLE_TEST_GIT_ENABLED);
		if (gitEnabled != null) {
			cloneCommand.execute("https://github.com/dirigiblelabs/sample_git_test.git", IGitConnector.GIT_MASTER, null, null, "workspace1", true, null);
			IWorkspace workspace1 = workspacesCoreService.getWorkspace("workspace1");
			assertNotNull(workspace1);
			assertTrue(workspace1.exists());
			IProject project1 = workspace1.getProject("project1");
			assertNotNull(project1);
			assertTrue(project1.exists());
			IFolder folder1 = project1.getFolder("folder1");
			assertNotNull(folder1);
			assertTrue(folder1.exists());
			IFile file1 = folder1.getFile("service1.js");
			assertNotNull(file1);
			assertTrue(file1.exists());
			workspace1.delete();
		}
	}
	
	/**
	 * Creates the workspace test.
	 *
	 * @throws GitConnectorException the git connector exception
	 */
	@Test
	public void createWorkspaceNoGitTest() throws GitConnectorException {
		String gitEnabled = System.getenv(GitConnectorTest.DIRIGIBLE_TEST_GIT_ENABLED);
		if (gitEnabled != null) {
			cloneCommand.execute("https://github.com/dirigiblelabs/sample_git_test", IGitConnector.GIT_MASTER, null, null, "workspace1", true, null);
			IWorkspace workspace1 = workspacesCoreService.getWorkspace("workspace1");
			assertNotNull(workspace1);
			assertTrue(workspace1.exists());
			IProject project1 = workspace1.getProject("project1");
			assertNotNull(project1);
			assertTrue(project1.exists());
			IFolder folder1 = project1.getFolder("folder1");
			assertNotNull(folder1);
			assertTrue(folder1.exists());
			IFile file1 = folder1.getFile("service1.js");
			assertNotNull(file1);
			assertTrue(file1.exists());
			workspace1.delete();
		}
	}
	
	/**
	 * Creates the workspace test.
	 *
	 * @throws GitConnectorException the git connector exception
	 */
	@Test
	public void createWorkspaceNoProjectTest() throws GitConnectorException {
		String gitEnabled = System.getenv(GitConnectorTest.DIRIGIBLE_TEST_GIT_ENABLED);
		if (gitEnabled != null) {
			cloneCommand.execute("https://github.com/dirigiblelabs/sample_git_no_project_test.git", IGitConnector.GIT_MASTER, null, null, "workspace1", true, null);
			IWorkspace workspace1 = workspacesCoreService.getWorkspace("workspace1");
			assertNotNull(workspace1);
			assertTrue(workspace1.exists());
			List<IProject> projects = workspace1.getProjects();
			for (IProject project : projects) {
				if (project.getName().startsWith("sample_git_no_project_test")) {
					workspace1.delete();
					return;
				}
			}
			workspace1.delete();
			fail("No project has been created implicitly");
		}
	}

}
