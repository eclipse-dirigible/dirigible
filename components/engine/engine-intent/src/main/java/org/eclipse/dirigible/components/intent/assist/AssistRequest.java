/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.assist;

import java.util.List;

import org.eclipse.dirigible.components.intent.ai.ChatTurn;

/**
 * A single Workbench assistant turn about one Java file - or about a TypeScript file being ported
 * to one.
 *
 * @param workspace the workspace name
 * @param project the project name
 * @param path the file's project-relative path, e.g. {@code custom/InvoiceNumber.java}; a
 *        {@code .ts} or {@code .mjs} file is ported to Java
 * @param source the editor buffer's current content - it may be dirty, and it is what the assistant
 *        reasons about; when omitted the file is read from the workspace
 * @param message the developer's new message
 * @param history the prior plain-text transcript, oldest first
 * @param target the Java file a port writes, project-relative; when omitted, the script's own path
 *        with a {@code .java} extension. Ignored for a Java file.
 */
record AssistRequest(String workspace, String project, String path, String source, String message, List<ChatTurn> history, String target) {
}
