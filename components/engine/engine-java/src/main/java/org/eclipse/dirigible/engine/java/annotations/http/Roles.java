/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.annotations.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restricts access to a controller class or method to authenticated users in any of the listed
 * roles. Roles are checked at dispatch time via
 * {@code org.eclipse.dirigible.components.api.security.UserFacade#isInRole(String)} — any-of
 * semantics. A method-level annotation overrides the class-level one for that method only.
 *
 * <p>
 * An empty {@link #value()} means "no restriction" (useful at the method level to open up a single
 * endpoint on an otherwise restricted controller).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Roles {

    /** Role names — any one of which grants access. */
    String[] value() default {};

}
