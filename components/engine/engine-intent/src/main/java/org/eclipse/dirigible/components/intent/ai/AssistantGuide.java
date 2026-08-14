/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.ai;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Loads an assistant's system prompt from the classpath.
 *
 * <p>
 * Every guide is an externalized markdown resource rather than an inline string, so it can be
 * reviewed and edited as documentation and stays in lockstep with the rules it teaches.
 */
public final class AssistantGuide {

    private AssistantGuide() {}

    /**
     * Load a guide packaged in this module's jar. A missing or unreadable resource is a build/packaging
     * defect, so it fails fast at class initialization rather than degrading the assistant to a
     * promptless state at request time.
     *
     * @param resource the absolute classpath resource path, e.g. {@code /intent-assistant-guide.md}
     * @return the guide contents
     */
    public static String load(String resource) {
        try (InputStream in = AssistantGuide.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Required classpath resource " + resource + " is missing");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load the assistant guide " + resource, ex);
        }
    }
}
