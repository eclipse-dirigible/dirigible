/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.capture;

/**
 * A vision-model provider. A client {@code @Component} implementing this interface is selected by
 * setting {@code DIRIGIBLE_CAPTURE_PROVIDER} to its {@link #name()}.
 */
public interface CaptureProvider {

    /**
     * The name the provider is selected by.
     *
     * @return the name
     */
    String name();

    /**
     * Read the document.
     *
     * @param request the document, the instructions and the JSON schema the answer must follow
     * @return the answer
     * @throws CaptureException when the document cannot be read
     */
    CaptureAnswer extract(CaptureRequest request);
}
