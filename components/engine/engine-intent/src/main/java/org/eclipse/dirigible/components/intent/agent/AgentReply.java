/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.agent;

/**
 * The assistant's response for one turn.
 *
 * @param reply the assistant's text reply (explanation or answer)
 * @param proposedYaml the complete proposed intent YAML when the assistant proposed an edit, else
 *        {@code null}
 */
record AgentReply(String reply, String proposedYaml) {
}
