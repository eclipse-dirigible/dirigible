/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.template.service;

/**
 * Supported built-in parameters for generation.
 */
public interface GenerationParameters {

    /** The name of the selected workspace. */
    public static final String PARAMETER_WORKSPACE_NAME = "workspaceName";

    /** The name of the selected project. */
    public static final String PARAMETER_PROJECT_NAME = "projectName";

    /** The name of the entered file name. */
    public static final String PARAMETER_FILE_NAME = "fileName";

    /** The name of the entered file name's extension. */
    public static final String PARAMETER_FILE_NAME_EXT = "fileNameExt";

    /** The name of the entered file name without extension. */
    public static final String PARAMETER_FILE_NAME_BASE = "fileNameBase";

    /** The name of the entered file's path. */
    public static final String PARAMETER_FILE_PATH = "filePath";

    /** The name of the selected package's path. */
    public static final String PARAMETER_PACKAGE_PATH = "packagePath";

    /** The name of the selected engine. */
    public static final String PARAMETER_ENGINE = "engine";

    /** The name of the selected handler. */
    public static final String PARAMETER_HANDLER = "handler";

}
