/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.integrations;

/**
 * Marker / placeholder class for the integrations namespace — the TS surface exposed Camel helpers
 * because TS callers can't import Java types directly. Java callers implementing a Camel
 * {@code Processor} already have the {@code Exchange} parameter, so the native API
 * ({@code exchange.getMessage().setHeader(...)}, {@code setBody(...)}, {@code setProperty(...)}) is
 * the right tool — there is no intermediate facade to add.
 * <p>
 * The package exists so import lists and documentation index stay symmetric with the TS surface;
 * the class itself has no methods.
 */
public final class Integrations {

    private Integrations() {}
}
