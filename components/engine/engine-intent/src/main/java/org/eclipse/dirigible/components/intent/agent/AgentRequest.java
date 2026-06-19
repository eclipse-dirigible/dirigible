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

import java.util.List;

/**
 * A single assistant turn requested by the Intent Editor.
 *
 * @param yaml the current intent YAML in the editor buffer (the ground truth to diff against)
 * @param message the developer's new message
 * @param history the prior plain-text transcript, oldest first (no tool calls are replayed)
 */
record AgentRequest(String yaml, String message, List<AgentTurn> history) {
}
