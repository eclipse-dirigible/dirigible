/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.document;

import org.eclipse.dirigible.components.api.pdf.PDFFacade;
import org.eclipse.dirigible.parsers.document.Node;
import org.eclipse.dirigible.parsers.document.binding.DataBinder;
import org.eclipse.dirigible.parsers.document.parser.DocumentParser;
import org.eclipse.dirigible.parsers.document.renderer.XslFoRenderer;

import java.util.Map;

/**
 * The {@code .print} rendering pipeline: parse the template, bind the data, render to a
 * self-contained XSL-FO stylesheet and (optionally) transform it to a PDF.
 */
final class PrintRenderer {

    /**
     * The renderer's output is a self-contained stylesheet, so the transformation source carries no
     * data of its own.
     */
    private static final String EMPTY_DATA_SOURCE = "<data/>";

    private PrintRenderer() {}

    /**
     * Parses the template, binds the data and renders the XSL-FO stylesheet.
     *
     * @param templateSource the {@code .print} template source
     * @param data the document data context
     * @return the XSL-FO stylesheet with the data merged in
     */
    static String renderFo(String templateSource, Map<String, Object> data) {
        Node bound = new DataBinder().bind(new DocumentParser().parse(templateSource), data);
        return new XslFoRenderer().renderBound(bound);
    }

    /**
     * Runs the full pipeline down to PDF bytes.
     *
     * @param templateSource the {@code .print} template source
     * @param data the document data context
     * @return the PDF bytes
     */
    static byte[] renderPdf(String templateSource, Map<String, Object> data) {
        return PDFFacade.generate(renderFo(templateSource, data), EMPTY_DATA_SOURCE);
    }
}
