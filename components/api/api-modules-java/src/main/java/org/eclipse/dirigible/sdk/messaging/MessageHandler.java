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
 * Optional typed contract for a {@link Listener @Listener} class. Implementing this interface is
 * not required — the runtime still accepts any class that exposes a public
 * {@code onMessage(String)} (and optionally {@code onError(String)}) method via reflection — but
 * implementations gain compile-time signature checking and the listener container dispatches
 * messages via a direct method call instead of a reflective {@code invoke}.
 *
 * <p>
 * The {@code @Listener} annotation remains the marker that binds the class to a JMS destination;
 * this interface only describes the callback shape.
 *
 * <p>
 * Example:
 *
 * <pre>
 * {@literal @}Listener(name = "orders", kind = ListenerKind.QUEUE)
 * public class OrderListener implements MessageHandler {
 *     {@literal @}Override public void onMessage(String message) { ... }
 *     {@literal @}Override public void onError(String error)    { ... } // optional
 * }
 * </pre>
 */
public interface MessageHandler {

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
