/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.messaging;

import org.eclipse.dirigible.components.listeners.service.MessageProducer;

/**
 * Puts a producer of the test's own behind {@link MessagingFacade}, for a test whose messages are
 * sent from another thread: a static mock of the facade answers only on the thread that made it. In
 * the facade's package because its constructor is not public.
 */
public final class TestMessaging {

    private TestMessaging() {}

    /**
     * Makes {@link MessagingFacade} send through the given producer, on every thread.
     *
     * @param producer the producer, or {@code null} to leave the facade as it is before the platform
     *        starts
     */
    public static void use(MessageProducer producer) {
        new MessagingFacade(null, producer);
    }
}
