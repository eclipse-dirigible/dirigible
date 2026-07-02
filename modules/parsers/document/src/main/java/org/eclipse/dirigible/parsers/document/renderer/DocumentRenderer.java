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

import org.eclipse.dirigible.parsers.document.layout.LayoutNode;

/**
 * The extension seam for concrete rendering backends. A renderer receives the normalized layout
 * tree (data already merged, {@code if}/{@code for} expanded by the caller's data-binding layer)
 * and produces its output form — e.g. an XSL-FO document for the platform's PDF pipeline.
 *
 * @param <T> the output type, e.g. {@code String} for an XSL-FO renderer
 */
public interface DocumentRenderer<T> {

    /**
     * Renders the layout tree.
     *
     * @param root the layout tree root
     * @return the rendered output
     */
    T render(LayoutNode root);
}
