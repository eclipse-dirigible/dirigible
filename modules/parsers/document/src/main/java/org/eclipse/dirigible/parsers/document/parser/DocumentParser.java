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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.dirigible.parsers.document.Attributes;
import org.eclipse.dirigible.parsers.document.DocumentNode;
import org.eclipse.dirigible.parsers.document.Node;
import org.eclipse.dirigible.parsers.document.SourcePosition;

/**
 * The hand-written recursive-descent parser of the document-template DSL. It produces the typed
 * {@link Node} AST and deliberately does NOT evaluate Mustache placeholders — <code>{{...}}</code>
 * sequences in text and attribute values survive verbatim for a later data-binding layer.
 *
 * <p>
 * The grammar is XML-like with business-user leniencies: only the five named entities
 * ({@code &lt; &gt; &amp; &quot; &apos;}) are decoded and a raw {@code &} that does not form one is
 * literal text; comments and an optional XML prolog are skipped; attribute values may use single or
 * double quotes. Unknown tags are a parse error — extend the DSL by registering tags on a
 * {@link TagRegistry}. Parsing is deterministic and instances are stateless per parse, so a parser
 * may be reused and shared.
 */
public final class DocumentParser {

    private final TagRegistry registry;

    /**
     * Creates a parser over the built-in tag registry.
     */
    public DocumentParser() {
        this(TagRegistry.builtIn());
    }

    /**
     * Creates a parser over a custom tag registry.
     *
     * @param registry the registry resolving tag names to node factories
     */
    public DocumentParser(TagRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    /**
     * Parses a template into its AST.
     *
     * @param source the template source
     * @return the root node
     * @throws ParseException on any syntax error, with the exact line and column
     */
    public Node parse(String source) {
        Objects.requireNonNull(source, "source");
        Scanner scanner = new Scanner(source);
        if (scanner.lookingAt("\uFEFF")) {
            scanner.skip(1);
        }
        skipMisc(scanner);
        skipProlog(scanner);
        skipMisc(scanner);
        if (scanner.eof() || scanner.peek() != '<') {
            throw scanner.error("Expected '<' to start the root element");
        }
        Node root = parseElement(scanner);
        skipMisc(scanner);
        if (!scanner.eof()) {
            throw scanner.error("Unexpected content after the root element");
        }
        return root;
    }

    /**
     * Parses a template and requires the root element to be {@code <document>}.
     *
     * @param source the template source
     * @return the root document node
     * @throws ParseException on any syntax error or when the root is not {@code <document>}
     */
    public DocumentNode parseDocument(String source) {
        Node root = parse(source);
        if (root instanceof DocumentNode document) {
            return document;
        }
        throw new ParseException("Root element must be <document> but was <" + root.tag() + ">", root.position()
                                                                                                     .line(),
                root.position()
                    .column());
    }

    private Node parseElement(Scanner scanner) {
        SourcePosition start = scanner.position();
        scanner.skip(1); // the '<'
        String tag = parseName(scanner, "tag name");
        NodeFactory factory = registry.factory(tag);
        if (factory == null) {
            throw scanner.errorAt("Unknown tag <" + tag + ">. Register it via TagRegistry.register(...)", start);
        }
        List<Attributes.Attribute> attributes = new ArrayList<>();
        while (true) {
            skipWhitespace(scanner);
            if (scanner.eof()) {
                throw scanner.errorAt("Unclosed element <" + tag + ">", start);
            }
            if (scanner.lookingAt("/>")) {
                scanner.skip(2);
                return factory.create(tag, start, Attributes.of(attributes), List.of(), "");
            }
            if (scanner.peek() == '>') {
                scanner.skip(1);
                return parseContent(scanner, tag, start, factory, attributes);
            }
            parseAttribute(scanner, attributes);
        }
    }

    private void parseAttribute(Scanner scanner, List<Attributes.Attribute> attributes) {
        SourcePosition position = scanner.position();
        String name = parseName(scanner, "attribute name");
        for (Attributes.Attribute existing : attributes) {
            if (existing.name()
                        .equals(name)) {
                throw scanner.errorAt("Duplicate attribute '" + name + "'", position);
            }
        }
        skipWhitespace(scanner);
        if (scanner.eof() || scanner.peek() != '=') {
            throw scanner.error("Expected '=' after attribute name '" + name + "'");
        }
        scanner.skip(1);
        skipWhitespace(scanner);
        if (scanner.eof() || (scanner.peek() != '"' && scanner.peek() != '\'')) {
            throw scanner.error("Attribute value must be quoted");
        }
        SourcePosition quotePosition = scanner.position();
        char quote = scanner.next();
        StringBuilder value = new StringBuilder();
        while (true) {
            if (scanner.eof()) {
                throw scanner.errorAt("Unterminated attribute value", quotePosition);
            }
            char c = scanner.peek();
            if (c == quote) {
                scanner.skip(1);
                break;
            }
            if (c == '&') {
                value.append(readEntityOrAmpersand(scanner));
            } else {
                value.append(scanner.next());
            }
        }
        attributes.add(new Attributes.Attribute(name, value.toString(), position));
    }

    private Node parseContent(Scanner scanner, String tag, SourcePosition start, NodeFactory factory,
            List<Attributes.Attribute> attributes) {
        List<Node> children = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        while (true) {
            if (scanner.eof()) {
                throw scanner.errorAt("Unclosed element <" + tag + ">", start);
            }
            if (scanner.lookingAt("<!--")) {
                skipComment(scanner);
            } else if (scanner.lookingAt("</")) {
                SourcePosition closePosition = scanner.position();
                scanner.skip(2);
                String closeTag = parseName(scanner, "closing tag name");
                skipWhitespace(scanner);
                if (scanner.eof() || scanner.peek() != '>') {
                    throw scanner.error("Expected '>' to end the closing tag </" + closeTag + ">");
                }
                scanner.skip(1);
                if (!closeTag.equals(tag)) {
                    throw scanner.errorAt("Mismatched closing tag: expected </" + tag + ">, found </" + closeTag + ">", closePosition);
                }
                return factory.create(tag, start, Attributes.of(attributes), children, normalize(text));
            } else if (scanner.peek() == '<') {
                children.add(parseElement(scanner));
            } else if (scanner.peek() == '&') {
                text.append(readEntityOrAmpersand(scanner));
            } else {
                text.append(scanner.next());
            }
        }
    }

    private String parseName(Scanner scanner, String what) {
        if (scanner.eof() || !isNameStart(scanner.peek())) {
            throw scanner.error("Invalid " + what);
        }
        StringBuilder name = new StringBuilder();
        name.append(scanner.next());
        while (!scanner.eof() && isNameChar(scanner.peek())) {
            name.append(scanner.next());
        }
        return name.toString();
    }

    private static boolean isNameStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private static boolean isNameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == '.';
    }

    /**
     * Decodes one of the five named entities, or returns a literal ampersand when the input does not
     * form one — the business-user leniency that keeps "Fruit & Veg" a valid text.
     */
    private char readEntityOrAmpersand(Scanner scanner) {
        if (scanner.lookingAt("&lt;")) {
            scanner.skip(4);
            return '<';
        }
        if (scanner.lookingAt("&gt;")) {
            scanner.skip(4);
            return '>';
        }
        if (scanner.lookingAt("&amp;")) {
            scanner.skip(5);
            return '&';
        }
        if (scanner.lookingAt("&quot;")) {
            scanner.skip(6);
            return '"';
        }
        if (scanner.lookingAt("&apos;")) {
            scanner.skip(6);
            return '\'';
        }
        scanner.skip(1);
        return '&';
    }

    private void skipProlog(Scanner scanner) {
        if (!scanner.lookingAt("<?xml")) {
            return;
        }
        SourcePosition start = scanner.position();
        while (!scanner.lookingAt("?>")) {
            if (scanner.eof()) {
                throw scanner.errorAt("Unterminated XML prolog", start);
            }
            scanner.skip(1);
        }
        scanner.skip(2);
    }

    private void skipMisc(Scanner scanner) {
        while (true) {
            skipWhitespace(scanner);
            if (scanner.lookingAt("<!--")) {
                skipComment(scanner);
            } else {
                return;
            }
        }
    }

    private void skipComment(Scanner scanner) {
        SourcePosition start = scanner.position();
        scanner.skip(4); // the '<!--'
        while (!scanner.lookingAt("-->")) {
            if (scanner.eof()) {
                throw scanner.errorAt("Unterminated comment", start);
            }
            scanner.skip(1);
        }
        scanner.skip(3);
    }

    private static void skipWhitespace(Scanner scanner) {
        while (!scanner.eof() && Character.isWhitespace(scanner.peek())) {
            scanner.skip(1);
        }
    }

    /** Collapses whitespace runs to single spaces and trims — deterministic text content. */
    private static String normalize(StringBuilder text) {
        StringBuilder normalized = new StringBuilder(text.length());
        boolean pendingSpace = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                pendingSpace = normalized.length() > 0;
            } else {
                if (pendingSpace) {
                    normalized.append(' ');
                    pendingSpace = false;
                }
                normalized.append(c);
            }
        }
        return normalized.toString();
    }
}
