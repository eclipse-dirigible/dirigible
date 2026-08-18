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
 * A single Workbench assistant turn about one Java file.
 *
 * @param workspace the workspace name
 * @param project the project name
 * @param path the target file's project-relative path, e.g. {@code custom/InvoiceNumber.java}
 * @param source the editor buffer's current content - it may be dirty, and it is what the assistant
 *        reasons about; when omitted the file is read from the workspace
 * @param message the developer's new message
 * @param history the prior plain-text transcript, oldest first
 */
record AssistRequest(String workspace, String project, String path, String source, String message, List<ChatTurn> history) {
}
