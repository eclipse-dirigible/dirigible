/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.parsers.document.renderer;

/**
 * Turns an {@code <image src="...">} value into something the PDF renderer can actually read.
 *
 * <p>
 * The library is dependency-free and knows no storage: a bound {@code src} is whatever the template
 * author wrote or the data carried — a document path in the platform's content store, a file
 * reference on a record. Only the host knows how to read those bytes, and only the host may decide
 * whether the caller is allowed to, so resolution is handed out here. An implementation returns a
 * source the renderer emits verbatim (in practice a {@code data:} URI carrying the bytes inline —
 * the renderer's output is a self-contained stylesheet, so nothing may be fetched later) or
 * {@code null} to render <b>nothing at all</b>: a document whose logo is missing prints without a
 * logo rather than with a broken-image box.
 */
public interface ImageResolver {

    /** Emits every source unchanged — the default, for a renderer used without a host. */
    ImageResolver PASS_THROUGH = source -> source;

    /**
     * Resolves one image source.
     *
     * @param source the bound {@code src} value, never blank
     * @return the source to emit, or {@code null} to omit the image entirely
     */
    String resolve(String source);
}
