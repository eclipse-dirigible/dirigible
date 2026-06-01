/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a client Java class as an ActiveMQ message listener managed by the Dirigible runtime.
 *
 * <p>
 * The annotated class must expose a public {@code onMessage(String message)} method and optionally
 * a {@code onError(String error)} method. Dirigible instantiates the class once, connects it to the
 * specified queue or topic, and routes incoming messages to {@code onMessage}. Hot-reload replaces
 * the listener transparently.
 *
 * <p>
 * Example:
 *
 * <pre>
 * {@literal @}Listener(name = "my-queue", kind = ListenerKind.QUEUE)
 * public class OrderListener {
 *     public void onMessage(String message) { ... }
 *     public void onError(String error) { ... }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Listener {

    /** Logical name of the queue or topic destination. */
    String name();

    /** Whether to listen on a queue (default) or a topic. */
    ListenerKind kind() default ListenerKind.QUEUE;

}
