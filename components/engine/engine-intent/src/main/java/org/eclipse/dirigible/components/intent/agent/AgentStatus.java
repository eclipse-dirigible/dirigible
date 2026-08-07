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
 * Whether the AI assistant is usable on this instance.
 *
 * @param configured {@code true} when an API key is configured, so a client can offer the assistant
 *        (or explain that it is unavailable) before the user's first message instead of after it
 *        fails
 */
record AgentStatus(boolean configured) {
}
