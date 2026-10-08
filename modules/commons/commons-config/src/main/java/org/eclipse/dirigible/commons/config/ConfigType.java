/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.commons.config;

/**
 * The kind of value a {@link DirigibleConfig} entry holds - what an editor or a view needs to
 * render it. Every value is still stored and read as a string.
 */
public enum ConfigType {

    /** Free text. */
    STRING,
    /** {@code true} or {@code false}. */
    BOOLEAN,
    /** A whole number. */
    INT,
    /** A whole number of the time unit the key names ({@code _SECONDS}, {@code _MINUTES}, ...). */
    DURATION,
    /** Comma-separated values. */
    LIST,
    /** A URL or URI. */
    URL,
    /** One of a fixed set of values. */
    ENUM,
    /** A password, client secret, token or API key - never shown in clear. */
    SECRET
}
