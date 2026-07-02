/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.parsers.document.parser;

import java.util.List;

import org.eclipse.dirigible.parsers.document.Attributes;
import org.eclipse.dirigible.parsers.document.Node;
import org.eclipse.dirigible.parsers.document.SourcePosition;

/**
 * Builds an AST node for a parsed element. The {@link TagRegistry} maps each tag name to one
 * factory; registering a factory is how new tags extend the DSL without touching the parser.
 */
@FunctionalInterface
public interface NodeFactory {

    /**
     * Creates the node for a fully parsed element.
     *
     * @param tag the tag name
     * @param position the position of the opening tag in the source
     * @param attributes the parsed attributes
     * @param children the parsed child nodes in document order
     * @param text the normalized text content
     * @return the AST node
     */
    Node create(String tag, SourcePosition position, Attributes attributes, List<Node> children, String text);
}
