/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.di.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
 * Parses TypeScript component files and extracts metadata (class name + @Inject properties with
 * their declared types) for dependency injection at runtime.
 */
public class ComponentParser {

    private static final Map<String, ComponentMetadata> COMPONENTS = Collections.synchronizedMap(new HashMap<>());
    private static final Map<String, String> MD5 = Collections.synchronizedMap(new HashMap<>());

    /**
     * Parses the given TypeScript source and returns extracted component metadata.
     *
     * @param location the file path
     * @param source the TypeScript source code
     * @return ComponentMetadata containing class name and injected fields
     */
    public ComponentMetadata parse(String location, String source) {
        String md5 = DigestUtils.md5Hex(source.getBytes());
        String filename = FilenameUtils.getBaseName(location);
        String existing = MD5.get(filename);
        if (existing != null && existing.equals(md5)) {
            return COMPONENTS.get(filename);
        }

        CharStream input = CharStreams.fromString(source);
        TypeScriptLexer lexer = new TypeScriptLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        TypeScriptParser parser = new TypeScriptParser(tokens);
        ParseTree tree = parser.program();

        MetadataExtractorVisitor visitor = new MetadataExtractorVisitor();
        ComponentMetadata metadata = visitor.visit(tree);

        COMPONENTS.put(filename, metadata);
        MD5.put(filename, md5);
        return metadata;
    }

    /**
     * Visitor implementation that walks the TypeScript AST and extracts
     *
     * @Inject-decorated properties and their declared types.
     */
    private static class MetadataExtractorVisitor extends TypeScriptParserBaseVisitor<ComponentMetadata> {

        private final ComponentMetadata meta = new ComponentMetadata();

        @Override
        public ComponentMetadata visitProgram(TypeScriptParser.ProgramContext ctx) {
            super.visitProgram(ctx);
            return meta;
        }

        @Override
        public ComponentMetadata visitClassDeclaration(TypeScriptParser.ClassDeclarationContext ctx) {

            if (ctx.identifier() != null) {
                meta.setClassName(ctx.identifier()
                                     .getText());
            }

            for (int i = 0; i < ctx.decoratorList()
                                   .decorator()
                                   .size(); i++) {
                if (ctx.decoratorList()
                       .decorator(i) instanceof TypeScriptParser.DecoratorContext) {
                    parseClassDecorator((TypeScriptParser.DecoratorContext) ctx.decoratorList()
                                                                               .decorator(i));
                }
            }

            for (TypeScriptParser.ClassElementContext elem : ctx.classTail()
                                                                .classElement()) {
                TypeScriptParser.PropertyDeclarationExpressionContext propDecl = null;
                List<TypeScriptParser.DecoratorContext> decorators = new ArrayList<>();

                // Scan children of each element for decorators + property declaration
                for (int i = 0; i < elem.getChildCount(); i++) {
                    ParseTree child = elem.getChild(i);
                    if (child instanceof TypeScriptParser.DecoratorListContext decList) {
                        decorators.addAll(decList.decorator());
                    } else if (child instanceof TypeScriptParser.DecoratorContext dec) {
                        decorators.add(dec);
                    } else if (child instanceof TypeScriptParser.PropertyDeclarationExpressionContext prop) {
                        propDecl = prop;
                    }
                }

                if (propDecl != null) {
                    String propertyName = propDecl.propertyName()
                                                  .getText();
                    String typeName = "unknown";

                    if (propDecl.typeAnnotation() != null && propDecl.typeAnnotation()
                                                                     .type_() != null) {
                        typeName = propDecl.typeAnnotation()
                                           .type_()
                                           .getText();
                    }

                    // Check if property has @Inject decorator
                    for (TypeScriptParser.DecoratorContext decCtx : decorators) {
                        String decoratorName = getDecoratorBaseName(decCtx);
                        if ("Inject".equals(decoratorName)) {
                            meta.addPropertyType(propertyName, typeName);
                            break;
                        }
                    }
                }
            }

            return super.visitClassDeclaration(ctx);
        }

        private void parseClassDecorator(TypeScriptParser.DecoratorContext ctx) {
            String decoratorName = getDecoratorBaseName(ctx);
            if (!"Component".equals(decoratorName))
                return;

            String name = null;
            String moduleName = null;

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
                if (argText.contains("name:")) {
                    int idx = argText.indexOf("name:") + 5;
                    name = argText.substring(idx)
                                  .trim()
                                  .replace("{", "")
                                  .replace("}", "")
                                  .replace("'", "")
                                  .replace("\"", "")
                                  .split(",")[0].trim();
                }
                meta.setComponentName(name);

                if (argText.contains("module:")) {
                    int idx = argText.indexOf("module:") + 7;
                    moduleName = argText.substring(idx)
                                        .trim()
                                        .replace("{", "")
                                        .replace("}", "")
                                        .replace("'", "")
                                        .replace("\"", "")
                                        .split(",")[0].trim();
                }
            }
            meta.setModuleName(moduleName);
        }

        /**
         * Helper to extract the simple decorator name (e.g. "Inject" from @core.Inject)
         */
        private String getDecoratorBaseName(TypeScriptParser.DecoratorContext ctx) {
            TypeScriptParser.DecoratorMemberExpressionContext memberCtx = null;
            if (ctx.decoratorCallExpression() != null) {
                memberCtx = ctx.decoratorCallExpression()
                               .decoratorMemberExpression();
            } else if (ctx.decoratorMemberExpression() != null) {
                memberCtx = ctx.decoratorMemberExpression();
            }
            if (memberCtx != null) {
                String fullName = memberCtx.getText();
                int lastDot = fullName.lastIndexOf('.');
                if (lastDot != -1) {
                    return fullName.substring(lastDot + 1);
                }
                return fullName;
            }
            return null;
        }
    }

}
