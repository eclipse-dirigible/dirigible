/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.extensions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Registers a client Java class as a contribution to a named Dirigible extension point.
 *
 * <p>
 * The runtime stores an {@code Extension} metadata record that maps the extension point name to
 * this class. Callers that query the extension point (via
 * {@code ExtensionService.findByExtensionPoint}) will receive this record among the results. The
 * class itself is instantiated on demand by the consumer — no specific interface is required.
 *
 * <p>
 * Example:
 *
 * <pre>
 * {@literal @}Extension(name = "my-menu-item", to = "ide-menu")
 * public class MyMenuItem {
 *     public List&lt;Map&lt;String, Object&gt;&gt; getItems() { ... }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Extension {

    /** Logical name of this extension contribution. */
    String name();

    /** Name of the extension point this class contributes to. */
    String to();

}
