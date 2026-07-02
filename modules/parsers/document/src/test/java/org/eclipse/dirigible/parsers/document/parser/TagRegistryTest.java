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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.dirigible.parsers.document.CustomNode;
import org.eclipse.dirigible.parsers.document.Node;
import org.junit.Test;

/**
 * Extending the DSL with new tags is a registry entry, never a parser change.
 */
public class TagRegistryTest {

    @Test
    public void unregisteredTagFailsWithPosition() {
        try {
            new DocumentParser().parse("<document><qrcode value=\"{{invoice.uuid}}\"/></document>");
        } catch (ParseException e) {
            assertTrue(e.getMessage()
                        .contains("Unknown tag <qrcode>"));
            assertEquals(11, e.getColumn());
            return;
        }
        fail("Expected ParseException");
    }

    @Test
    public void registeredTagParsesToCustomNode() {
        TagRegistry registry = TagRegistry.builtIn();
        registry.register("qrcode");
        Node root = new DocumentParser(registry).parse("<document><qrcode value=\"{{invoice.uuid}}\">payload</qrcode></document>");
        Node qrcode = root.children()
                          .get(0);
        assertTrue(qrcode instanceof CustomNode);
        assertEquals("qrcode", qrcode.tag());
        assertEquals("{{invoice.uuid}}", qrcode.attributes()
                                               .get("value"));
        assertEquals("payload", qrcode.text());
    }

    @Test
    public void customFactoryMayProduceRicherNodes() {
        class BarcodeNode extends CustomNode {
            BarcodeNode(String tag, org.eclipse.dirigible.parsers.document.SourcePosition position,
                    org.eclipse.dirigible.parsers.document.Attributes attributes, java.util.List<Node> children, String text) {
                super(tag, position, attributes, children, text);
            }
        }
        TagRegistry registry = TagRegistry.builtIn();
        registry.register("barcode", BarcodeNode::new);
        Node root = new DocumentParser(registry).parse("<document><barcode/></document>");
        assertTrue(root.children()
                       .get(0) instanceof BarcodeNode);
    }

    @Test
    public void builtInsStillWorkOnACustomRegistry() {
        TagRegistry registry = TagRegistry.builtIn();
        registry.register("qrcode");
        Node root = new DocumentParser(registry).parse("<document><text>Hi</text><qrcode/></document>");
        assertEquals("text", root.children()
                                 .get(0)
                                 .tag());
        assertEquals("qrcode", root.children()
                                   .get(1)
                                   .tag());
    }

    @Test
    public void reRegisteringOverridesTheFactory() {
        TagRegistry registry = TagRegistry.builtIn();
        registry.register("text",
                (tag, position, attributes, children, text) -> new CustomNode(tag, position, attributes, children, text.toUpperCase()));
        Node root = new DocumentParser(registry).parse("<document><text>hi</text></document>");
        assertEquals("HI", root.children()
                               .get(0)
                               .text());
    }
}
