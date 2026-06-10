/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a client Java class as a REST controller. Methods annotated with {@link Get}, {@link Post},
 * {@link Put}, {@link Patch}, {@link Delete} are exposed as HTTP endpoints.
 *
 * <p>
 * The base URL is derived from the class's fully-qualified name (the same path used by
 * {@code JavaEndpoint}): {@code /services/java/<project>/<package-path>/<ClassName>}. Each method
 * annotation contributes the trailing suffix; the HTTP method comes from the annotation type.
 *
 * <p>
 * A controller class must be public, have a public no-arg constructor, and must not also implement
 * {@code JavaHandler} — the two dispatch styles are mutually exclusive.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Controller {
}
