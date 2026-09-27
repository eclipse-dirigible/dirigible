/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.db;

/**
 * One soft finding about a write - a condition the model declared with {@code severity: warn} (a
 * second customer with the same name, a document line at price zero). Unlike a
 * {@link ValidationException} it does not refuse the write: it asks the person making it to confirm
 * first (issue #7466).
 *
 * @param code the stable identity of the warning - what a caller echoes back to confirm it
 * @param message the authored, user-facing text
 */
public record Warning(String code, String message) {
}
