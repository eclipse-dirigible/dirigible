/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.runtime;

/**
 * A single structured {@code javac} diagnostic attributed to a client source unit. Unlike the
 * formatted failure string (which collapses every error for a file into one message), this carries
 * the per-diagnostic position so consumers - the Problems view in particular - can show one entry
 * per error at its exact line/column.
 *
 * @param error {@code true} for an error, {@code false} for a warning
 * @param line 1-based source line, or a non-positive value when javac reported no position
 * @param column 1-based source column, or a non-positive value when javac reported no position
 * @param message the localized diagnostic message
 */
public record CompileDiagnostic(boolean error, long line, long column, String message) {
}
