/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.platform;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.dirigible.commons.api.helpers.BytesHelper;
import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.components.ide.workspace.domain.File;
import org.eclipse.dirigible.components.ide.workspace.domain.Workspace;
import org.eclipse.dirigible.components.ide.workspace.service.WorkspaceService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The Workspace Facade.
 */
@Component
public class WorkspaceFacade implements InitializingBean {

    /** The instance. */
    private static WorkspaceFacade INSTANCE;

    /** The workspace service. */
    private WorkspaceService workspaceService;

    /**
     * Instantiates a new workspace facade.
     *
     * @param workspaceService the workspace service
     */
    @Autowired
    public WorkspaceFacade(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    /**
     * After properties set.
     *
     * @throws Exception the exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        INSTANCE = this;
    }

    /**
     * Gets the instance.
     *
     * @return the workspace facade
     */
    public static WorkspaceFacade get() {
        return INSTANCE;
    }

    /**
     * Gets the workspace service.
     *
     * @return the workspace service
     */
    public WorkspaceService getWorkspaceService() {
        return workspaceService;
    }

    /**
     * Creates a workspace.
     *
     * @param name the name
     * @return the workspace
     */
    public static Workspace createWorkspace(String name) {
        return WorkspaceFacade.get()
                              .getWorkspaceService()
                              .createWorkspace(name);
    }

    /**
     * Gets the workspace.
     *
     * @param name the workspace name
     * @return the workspace
     */
    public static Workspace getWorkspace(String name) {
        return WorkspaceFacade.get()
                              .getWorkspaceService()
                              .getWorkspace(name);
    }

    /**
     * Gets the workspaces names.
     *
     * @return the workspaces
     */
    public static String getWorkspacesNames() {
        List<String> names = new ArrayList<String>();
        for (Workspace workspace : WorkspaceFacade.get()
                                                  .getWorkspaceService()
                                                  .getWorkspaces()) {
            names.add(workspace.getName());
        }
        return GsonHelper.toJson(names);
    }

    /**
     * Delete workspace.
     *
     * @param name the name
     */
    public static void deleteWorkspace(String name) {
        WorkspaceFacade.get()
                       .getWorkspaceService()
                       .deleteWorkspace(name);
    }

    /**
     * Get the file content.
     *
     * @param file the file
     * @return the content
     */
    public static final byte[] getContent(File file) {
        return file.getContent();
    }

    /**
     * Set the file content.
     *
     * @param file the file
     * @param input the input
     */
    public static final void setContent(File file, String input) {
        byte[] bytes = BytesHelper.jsonToBytes(input);
        file.setContent(bytes);
    }

    /**
     * Set the file content.
     *
     * @param file the file
     * @param input the input
     */
    public static final void setContent(File file, byte[] input) {
        file.setContent(input);
    }

}
