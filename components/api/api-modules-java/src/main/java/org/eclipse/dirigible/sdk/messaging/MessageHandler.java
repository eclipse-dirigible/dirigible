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

/**
 * Self-describing contract for a message listener — the strong-interface style. A
 * {@link org.eclipse.dirigible.sdk.component.Component @Component} bean that implements this
 * interface IS a listener: it supplies its own destination via {@link #destination()} (and
 * optionally {@link #kind()}) and its handling via {@link #onMessage(String)}, with no
 * {@code @Listener} annotation. This mirrors implementing {@code jakarta.jms.MessageListener} in
 * Spring — the implementation carries everything.
 *
 * <p>
 * The alternative, method-level style is {@code @Listener} on a {@code @Component} method. A single
 * class uses one style or the other, never both.
 *
 * <p>
 * Example:
 *
 * <pre>
 * {@literal @}Component
 * public class OrderListener implements MessageHandler {
 *     public String destination() { return "orders"; }
 *     public void onMessage(String message) { ... }
 * }
 * </pre>
 */
public interface MessageHandler {

    /**
     * The queue or topic this listener binds to.
     *
     * @return the destination name
     */
    String destination();

    /**
     * Whether {@link #destination()} is a queue (default) or a topic.
     *
     * @return the destination kind
     */
    default ListenerKind kind() {
        return ListenerKind.QUEUE;
    }

    /** Fires for every text message received on the bound destination. */
    void onMessage(String message);

    /**
     * Fires when the listener container catches an exception while delivering a message. Default is a
     * no-op so implementations only override it when they need to react to delivery failures.
     */
    default void onError(String error) {
        // intentional no-op default
    }
}
