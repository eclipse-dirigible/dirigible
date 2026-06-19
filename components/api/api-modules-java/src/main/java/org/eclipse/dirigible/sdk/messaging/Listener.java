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

import org.eclipse.dirigible.sdk.component.Component;

/**
 * Subscribes a client handler to an ActiveMQ queue or topic. Two styles are supported, like
 * Spring's {@code @JmsListener}:
 *
 * <p>
 * <b>Class level</b> — annotate a class that either implements {@link MessageHandler} or exposes a
 * public {@code onMessage(String)} method (and optionally {@code onError(String)}):
 *
 * <pre>
 * {@literal @}Listener(name = "my-queue", kind = ListenerKind.QUEUE)
 * public class OrderListener implements MessageHandler {
 *     public void onMessage(String message) { ... }
 * }
 * </pre>
 *
 * <p>
 * <b>Method level</b> — annotate a public {@code void m(String message)} method on a
 * {@link org.eclipse.dirigible.sdk.component.Component @Component} bean; the bean can host several
 * listeners and use injected collaborators:
 *
 * <pre>
 * {@literal @}Component
 * public class Orders {
 *     {@literal @}Listener(name = "orders-new", kind = ListenerKind.TOPIC)
 *     public void onNewOrder(String message) { ... }
 * }
 * </pre>
 *
 * <p>
 * Dirigible connects the handler to the destination and routes incoming messages to it; hot-reload
 * replaces the subscription transparently.
 */
@Component
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Listener {

    /** Logical name of the queue or topic destination. */
    String name();

    /** Whether to listen on a queue (default) or a topic. */
    ListenerKind kind() default ListenerKind.QUEUE;

}
