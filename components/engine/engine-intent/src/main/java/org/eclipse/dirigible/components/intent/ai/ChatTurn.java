/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.ai;

/**
 * One prior message in an assistant transcript, as the browser replays it. Shared by every
 * assistant surface so a client speaks one shape regardless of which endpoint it talks to.
 *
 * @param role {@code user} or {@code assistant}
 * @param content the plain-text message content
 */
public record ChatTurn(String role, String content) {
}
