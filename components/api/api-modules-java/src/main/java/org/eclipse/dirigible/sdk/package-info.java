/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
/**
 * Public Dirigible SDK for client Java code dropped under {@code /registry/public/<project>/...}.
 * Sub-packages group related capabilities — file I/O, HTTP request and response, database access,
 * security, scheduling, messaging — and within each sub-package classes follow Java conventions:
 * static facades ({@code Files}, {@code Base64}, {@code Configurations}) where the operations are
 * stateless, regular classes ({@code Logger}, {@code Connection}) where the calls share per-
 * instance state.
 * <p>
 * Most classes are thin wrappers over the {@code org.eclipse.dirigible.components.api.*} facades —
 * the same Java code paths the GraalJS bridge exposes to TS / JS user code. That keeps the two SDK
 * surfaces strictly equivalent and means semantics, error contracts, and observable side effects do
 * not drift between languages.
 * <p>
 * Annotation surface (controllers, entities, scheduled jobs, listeners, websockets, extensions,
 * dependency injection markers) lives under the corresponding sub-package so an import like
 * {@code org.eclipse.dirigible.sdk.http.Controller} groups naturally with {@code Get},
 * {@code Post}, {@code Body}, {@code PathParam} etc. These annotations are consumed at runtime by
 * the engine- java {@code JavaClassConsumer} chain — there is no compile-time code generation step.
 */
package org.eclipse.dirigible.sdk;
