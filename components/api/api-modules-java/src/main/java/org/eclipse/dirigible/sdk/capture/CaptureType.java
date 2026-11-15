/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.capture;

/** The type of a value the model is asked to read off a document. */
public enum CaptureType {
    /** Free text, returned as printed. */
    STRING,
    /** A whole number. */
    INTEGER,
    /** A decimal number. */
    NUMBER,
    /** A calendar date, returned as ISO {@code yyyy-MM-dd}. */
    DATE,
    /** A yes/no value. */
    BOOLEAN
}
