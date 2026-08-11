/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.template.service.model;

/**
 * One file produced by model generation, before it is written into the workspace.
 *
 * @param location the template it was rendered from, null for a file the pipeline composes itself
 * @param path the project-relative path to write it to
 * @param content the rendered content
 */
public record GeneratedFile(String location, String path, String content) {
}
