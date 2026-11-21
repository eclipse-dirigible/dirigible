/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.context;

import java.util.Optional;

/**
 * The language bound to the current thread, preferred over a request's {@code Accept-Language}
 * wherever the platform resolves a language: the multilingual overlay, a print template, a catalog
 * message.
 *
 * <p>
 * It exists for work that runs without a request yet knows whose language it serves: a render bound
 * by generated code ({@code User.setLanguage}), and the handler of an event, which is bound - with
 * its {@link OriginatingUser} - to the language of the write that raised the event (#8776). It is
 * attribution for message and data resolution only and grants nothing.
 */
public final class ThreadLanguage {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private ThreadLanguage() {}

    /**
     * The language bound to the current thread.
     *
     * @return the language, or empty when none is bound
     */
    public static Optional<String> get() {
        return Optional.ofNullable(CURRENT.get());
    }

    /**
     * Binds a language to the current thread; whoever binds it clears it, so a pooled thread never
     * keeps it. Prefer {@link OriginatingUser#bind(String, String)}, whose scope restores what was
     * bound before.
     *
     * @param language the language; {@code null} or blank clears the binding
     */
    public static void set(String language) {
        if (language == null || language.isBlank()) {
            CURRENT.remove();
        } else {
            CURRENT.set(language);
        }
    }

    /** Clears the language bound to the current thread. */
    public static void clear() {
        CURRENT.remove();
    }
}
