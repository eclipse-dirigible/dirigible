/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field on a client class (typically a {@code @Controller}) as needing dependency
 * injection. The field's declared type is resolved through the engine's
 * {@code org.eclipse.dirigible.engine.java.spi.DependencyResolver} chain at class-load time —
 * presently fulfilled by repositories from {@code data-store-java}, but the SPI is open for further
 * consumers.
 *
 * <p>
 * Unlike Spring's {@code @Autowired}, this injection happens via the engine's own SPI — client
 * classes are not Spring-scanned, so {@code @Autowired} would silently no-op.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Inject {
}
