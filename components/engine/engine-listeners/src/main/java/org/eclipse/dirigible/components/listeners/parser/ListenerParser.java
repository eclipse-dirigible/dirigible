/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.listeners.parser;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.dirigible.parsers.typescript.TypeScriptLexer;
import org.eclipse.dirigible.parsers.typescript.TypeScriptParser;
import org.eclipse.dirigible.parsers.typescript.TypeScriptParserBaseVisitor;

/**
 * Parser that extracts metadata from TypeScript classes decorated with:
 *
 * <pre>
 * @Listener({
 *     name: "MyListener",
 *     kind: "event"
 * })
 * </pre>
 */
public class ListenerParser {

    public static final Map<String, ListenerMetadata> LISTENERS = Collections.synchronizedMap(new HashMap<>());
    public static final Map<String, String> MD5 = Collections.synchronizedMap(new HashMap<>());

    /**
     * Parses the given TypeScript source and extracts @Listener metadata.
     *
     * @param location the file path or name
     * @param source the TypeScript source code
     * @return the extracted ListenerMetadata
     */
    public ListenerMetadata parse(String location, String source) {
        String md5 = DigestUtils.md5Hex(source.getBytes());
        String filename = FilenameUtils.getBaseName(location);
        String existing = MD5.get(filename);

        if (existing != null && existing.equals(md5)) {
            return LISTENERS.get(filename);
        }

        CharStream input = CharStreams.fromString(source);
        TypeScriptLexer lexer = new TypeScriptLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        TypeScriptParser parser = new TypeScriptParser(tokens);

        ParseTree tree = parser.program();

        MetadataExtractorVisitor visitor = new MetadataExtractorVisitor();
        ListenerMetadata metadata = visitor.visit(tree);

        LISTENERS.put(filename, metadata);
        MD5.put(filename, md5);

        return metadata;
    }

    /**
     * Extracts @Listener(name, kind) decorator metadata from the TypeScript AST.
     */
    private static class MetadataExtractorVisitor extends TypeScriptParserBaseVisitor<ListenerMetadata> {

        private final ListenerMetadata metadata = new ListenerMetadata();

        @Override
        public ListenerMetadata visitProgram(TypeScriptParser.ProgramContext ctx) {
            super.visitProgram(ctx);
            return metadata;
        }

        @Override
        public ListenerMetadata visitClassDeclaration(TypeScriptParser.ClassDeclarationContext ctx) {
            if (ctx.identifier() != null && ctx.identifier()
                                               .Identifier() != null) {
                metadata.setClassName(ctx.identifier()
                                         .Identifier()
                                         .getText());
            }

            if (ctx.decoratorList() != null && ctx.decoratorList()
                                                  .decorator() != null) {
                ctx.decoratorList()
                   .decorator()
                   .forEach(this::parseClassDecorator);
            }

            return super.visitClassDeclaration(ctx);
        }

        private void parseClassDecorator(TypeScriptParser.DecoratorContext ctx) {
            String decoratorName = getDecoratorBaseName(ctx);
            if (!"Listener".equals(decoratorName)) {
                return;
            }

            if (ctx.decoratorCallExpression() != null && ctx.decoratorCallExpression()
                                                            .arguments() != null
                    && ctx.decoratorCallExpression()
                          .arguments()
                          .argumentList() != null
                    && !ctx.decoratorCallExpression()
                           .arguments()
                           .argumentList()
                           .argument()
                           .isEmpty()) {

                String argText = ctx.decoratorCallExpression()
                                    .arguments()
                                    .argumentList()
                                    .argument(0)
                                    .getText();

                // Extract decorator parameters
                metadata.setName(extractValue(argText, "name"));
                metadata.setKind(extractValue(argText, "kind"));
            }
        }

        private String getDecoratorBaseName(TypeScriptParser.DecoratorContext ctx) {
            if (ctx.decoratorCallExpression() != null && ctx.decoratorCallExpression()
                                                            .decoratorMemberExpression() != null) {
                String fullName = ctx.decoratorCallExpression()
                                     .decoratorMemberExpression()
                                     .getText();
                int lastDot = fullName.lastIndexOf('.');
                return lastDot != -1 ? fullName.substring(lastDot + 1) : fullName;
            } else if (ctx.decoratorMemberExpression() != null) {
                String fullName = ctx.decoratorMemberExpression()
                                     .getText();
                int lastDot = fullName.lastIndexOf('.');
                return lastDot != -1 ? fullName.substring(lastDot + 1) : fullName;
            }
            return null;
        }

        private String extractValue(String source, String key) {
            String pattern = key + ":";
            if (source.contains(pattern)) {
                String valueSegment = source.substring(source.indexOf(pattern) + pattern.length())
                                            .trim();
                int end = valueSegment.indexOf(",");
                if (end == -1)
                    end = valueSegment.indexOf("}");
                if (end == -1)
                    end = valueSegment.length();
                String value = valueSegment.substring(0, end)
                                           .trim()
                                           .replace("{", "")
                                           .replace("}", "")
                                           .replace("'", "")
                                           .replace("\"", "");
                return value;
            }
            return null;
        }
    }
}
