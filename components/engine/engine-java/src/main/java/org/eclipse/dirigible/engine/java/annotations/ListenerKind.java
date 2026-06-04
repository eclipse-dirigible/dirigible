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

/**
 * Destination type for a {@link Listener}-annotated class.
 */
public enum ListenerKind {

    /** Point-to-point queue — each message is consumed by exactly one listener. */
    QUEUE,

    /** Publish-subscribe topic — each message is delivered to all active subscribers. */
    TOPIC
}
