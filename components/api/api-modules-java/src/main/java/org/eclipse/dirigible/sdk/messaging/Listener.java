/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.messaging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Subscribes a public {@code void m(String message)} method of a
 * {@link org.eclipse.dirigible.sdk.component.Component &#64;Component} bean to an ActiveMQ queue or
 * topic — the method-level style, like Spring's {@code @JmsListener}. One bean can host several
 * listener methods and use injected collaborators.
 *
 * <p>
 * The alternative, strong-interface style is a {@code @Component} bean implementing
 * {@link MessageHandler} (which supplies its own {@code destination()} / {@code kind()}). A class
 * uses one style or the other, never both.
 *
 * <p>
 * Example:
 *
 * <pre>
 * {@literal @}Component
 * public class Orders {
 *     {@literal @}Listener(name = "orders-new", kind = ListenerKind.TOPIC)
 *     public void onNewOrder(String message) { ... }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Listener {

    /** Logical name of the queue or topic destination. */
    String name();

    /** Whether to listen on a queue (default) or a topic. */
    ListenerKind kind() default ListenerKind.QUEUE;

}
