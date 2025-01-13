/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.git.config;

import org.eclipse.dirigible.components.ide.git.command.StatusCommand;
import org.eclipse.dirigible.components.ide.git.domain.GitProjectStatusProvider;
import org.eclipse.dirigible.components.ide.workspace.domain.ProjectStatusProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The Class GitConfig.
 */
@Configuration
public class GitConfig {

    /**
     * Creates the project status provider.
     *
     * @param statusCommand the status command
     * @return the project status provider
     */
    @Bean
    public ProjectStatusProvider createProjectStatusProvider(StatusCommand statusCommand) {
        return new GitProjectStatusProvider(statusCommand);
    }

}
