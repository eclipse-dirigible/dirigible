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

/**
 * One compiler error on a proposed source, positioned so a client can render it at the line the
 * Problems view would.
 *
 * @param line the 1-based line, or {@code -1} when the error carries no position
 * @param column the 1-based column, or {@code -1} when the error carries no position
 * @param message the compiler's message
 */
record AssistDiagnostic(long line, long column, String message) {
}
