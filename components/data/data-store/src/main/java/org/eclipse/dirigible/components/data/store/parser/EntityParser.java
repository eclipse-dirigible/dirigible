/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store.parser;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.dirigible.components.data.store.model.EntityFieldMetadata;
import org.eclipse.dirigible.components.data.store.model.EntityFieldMetadata.CollectionDetails;
import org.eclipse.dirigible.components.data.store.model.EntityFieldMetadata.ColumnDetails;
import org.eclipse.dirigible.components.data.store.model.EntityMetadata;
import org.eclipse.dirigible.parsers.typescript.TypeScriptLexer;
import org.eclipse.dirigible.parsers.typescript.TypeScriptParser;
import org.eclipse.dirigible.parsers.typescript.TypeScriptParserBaseVisitor;

/**
 * Main parser class to generate EntityMetadata from TypeScript code.
 */
public class EntityParser {

    /**
     * Parses the given TypeScript source code and extracts EntityMetadata.
     *
     * @param tsSource The TypeScript source code string.
     * @return EntityMetadata object populated with extracted data.
     */
    public EntityMetadata parse(String tsSource) {
        CharStream input = CharStreams.fromString(tsSource);
        TypeScriptLexer lexer = new TypeScriptLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        TypeScriptParser parser = new TypeScriptParser(tokens);

        // Disable default console error logging if you use custom listeners
        // parser.removeErrorListeners();

        ParseTree tree = parser.program();

        MetadataExtractorVisitor visitor = new MetadataExtractorVisitor();
        return visitor.visit(tree);
    }

    /**
     * Custom visitor to traverse the ParseTree and extract entity metadata. The result of the visit is
     * the EntityMetadata object.
     */
    private static class MetadataExtractorVisitor extends TypeScriptParserBaseVisitor<EntityMetadata> {

        private EntityMetadata currentEntityMetadata = new EntityMetadata();

        @Override
        public EntityMetadata visitProgram(TypeScriptParser.ProgramContext ctx) {
            super.visitProgram(ctx);
            return currentEntityMetadata;
        }

        private String getDecoratorBaseName(TypeScriptParser.DecoratorContext ctx) {
            TypeScriptParser.DecoratorMemberExpressionContext memberCtx = null;

            if (ctx.decoratorCallExpression() != null) {
                // If it's a call expression like @Entity('Car'), the name is inside the call
                memberCtx = ctx.decoratorCallExpression()
                               .decoratorMemberExpression();
            } else if (ctx.decoratorMemberExpression() != null) {
                // If it's a simple decorator like @Id
                memberCtx = ctx.decoratorMemberExpression();
            }

            if (memberCtx != null) {
                String fullName = memberCtx.getText();
                // Handle namespaced decorators: return only the part after the last dot
                int lastDot = fullName.lastIndexOf('.');
                if (lastDot != -1) {
                    return fullName.substring(lastDot + 1);
                }
                // Return the full name for non-namespaced decorators
                return fullName;
            }
            return null;
        }

        // Helper to get value for a key in a simple object literal string
        private java.util.function.BiFunction<String, String, String> extractValue = (source, key) -> {
            // Look for key:
            String pattern = key + ":";
            if (source.contains(pattern)) {
                // Start after the key and colon
                String valueSegment = source.substring(source.indexOf(pattern) + pattern.length())
                                            .trim();

                // Finds end of value (comma or closing brace)
                int end = valueSegment.indexOf(",");
                if (end == -1)
                    end = valueSegment.length(); // Go to end of string if no comma

                // Extract, strip whitespace and quotes, and remove surrounding braces
                String value = valueSegment.substring(0, end)
                                           .trim()
                                           .replace("{", "")
                                           .replace("}", "")
                                           .replace("'", "")
                                           .replace("\"", "");

                // Handle boolean or numeric primitives
                if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false") || value.matches("-?\\d+(\\.\\d+)?")) {
                    return value;
                }

                // Return original value (may still contain quotes, removed above)
                return value;
            }
            return null;
        };

        @Override
        public EntityMetadata visitClassDeclaration(TypeScriptParser.ClassDeclarationContext ctx) {

            TerminalNode classNameNode = ctx.identifier()
                                            .Identifier();
            if (classNameNode != null) {
                currentEntityMetadata.setClassName(classNameNode.getText());
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

            return super.visitClassDeclaration(ctx);
        }

        private void parseClassDecorator(TypeScriptParser.DecoratorContext ctx) {
            String decoratorName = getDecoratorBaseName(ctx);
            if (decoratorName == null)
                return;

            if ("Entity".equals(decoratorName)) {
                // Extract argument from @Entity('CarEntity')
                if (ctx.decoratorCallExpression() != null && ctx.decoratorCallExpression()
                                                                .arguments() != null
                        && ctx.decoratorCallExpression()
                              .arguments()
                              .argumentList()
                              .argument()
                              .size() > 0) {

                    String arg = ctx.decoratorCallExpression()
                                    .arguments()
                                    .argumentList()
                                    .argument(0)
                                    .getText();
                    if (arg.startsWith("'") || arg.startsWith("\"")) {
                        arg = arg.substring(1, arg.length() - 1);
                    }
                    currentEntityMetadata.setEntityName(arg);
                } else {
                    currentEntityMetadata.setEntityName(currentEntityMetadata.getClassName());
                }

            } else if ("Table".equals(decoratorName)) {
                // Extract argument from @Table({ name: 'CARS' })
                if (ctx.decoratorCallExpression() != null && ctx.decoratorCallExpression()
                                                                .arguments() != null
                        && ctx.decoratorCallExpression()
                              .arguments()
                              .argumentList()
                              .argument()
                              .size() > 0) {

                    String tableArgText = ctx.decoratorCallExpression()
                                             .arguments()
                                             .argumentList()
                                             .argument(0)
                                             .getText();
                    // Simplified: search for "name:" and extract value (removes quotes)
                    if (tableArgText.contains("name:")) {
                        String nameValue = tableArgText.substring(tableArgText.indexOf("name:") + 5)
                                                       .trim();
                        int end = nameValue.indexOf(",");
                        if (end == -1)
                            end = nameValue.length() - 1;

                        String value = nameValue.substring(0, end)
                                                .trim()
                                                .replace("'", "")
                                                .replace("\"", "");
                        currentEntityMetadata.setTableName(value);
                    } else {
                        currentEntityMetadata.setTableName(tableArgText.trim()
                                                                       .replace("'", "")
                                                                       .replace("\"", ""));
                    }
                }
            }
        }

        /**
         * Overrides the visit method for class members. We manually iterate over children to find the
         * DecoratorContexts.
         */
        @Override
        public EntityMetadata visitClassElement(TypeScriptParser.ClassElementContext ctx) {

            TypeScriptParser.PropertyDeclarationExpressionContext propertyDeclaration = null;
            List<TypeScriptParser.DecoratorContext> decorators = new ArrayList<>();

            for (int i = 0; i < ctx.getChildCount(); i++) {
                ParseTree child = ctx.getChild(i);

                if (child instanceof TypeScriptParser.DecoratorListContext) {
                    decorators.addAll(((TypeScriptParser.DecoratorListContext) child).decorator());
                } else if (child instanceof TypeScriptParser.DecoratorContext) {
                    decorators.add((TypeScriptParser.DecoratorContext) child);
                }
                // Identify the actual property declaration node
                else if (child instanceof TypeScriptParser.PropertyDeclarationExpressionContext) {
                    propertyDeclaration = (TypeScriptParser.PropertyDeclarationExpressionContext) child;
                }
            }

            if (propertyDeclaration != null) {

                EntityFieldMetadata fieldMetadata = new EntityFieldMetadata();
                ColumnDetails columnDetails = new ColumnDetails();
                fieldMetadata.setColumnDetails(columnDetails);

                fieldMetadata.setPropertyName(propertyDeclaration.propertyName()
                                                                 .getText());

                if (propertyDeclaration.typeAnnotation() != null) {
                    // typeAnnotation includes the colon, type_ is the actual type content.
                    fieldMetadata.setTypeScriptType(propertyDeclaration.typeAnnotation()
                                                                       .type_()
                                                                       .getText());
                } else {
                    fieldMetadata.setTypeScriptType("unknown");
                }

                for (TypeScriptParser.DecoratorContext decoratorCtx : decorators) {
                    parsePropertyDecorator(decoratorCtx, fieldMetadata, columnDetails);
                }

                currentEntityMetadata.getFields()
                                     .add(fieldMetadata);
            }

            super.visitClassElement(ctx);
            return currentEntityMetadata;
        }

        private void parsePropertyDecorator(TypeScriptParser.DecoratorContext ctx, EntityFieldMetadata fieldMetadata,
                ColumnDetails columnDetails) {

            String decoratorName = getDecoratorBaseName(ctx);
            if (decoratorName == null)
                return;

            if ("Id".equals(decoratorName)) {
                fieldMetadata.setIdentifier(true);

            } else if ("Generated".equals(decoratorName)) {
                // Expects @Generated('sequence')
                if (ctx.decoratorCallExpression() != null && ctx.decoratorCallExpression()
                                                                .arguments() != null
                        && ctx.decoratorCallExpression()
                              .arguments()
                              .argumentList()
                              .argument()
                              .size() > 0) {

                    String strategy = ctx.decoratorCallExpression()
                                         .arguments()
                                         .argumentList()
                                         .argument(0)
                                         .getText();
                    if (strategy.startsWith("'") || strategy.startsWith("\"")) {
                        strategy = strategy.substring(1, strategy.length() - 1);
                    }
                    fieldMetadata.setGenerationStrategy(strategy);
                }

            } else if ("Column".equals(decoratorName)) {
                // Expects @Column({ name: 'car_id', type: 'int', ... })
                if (ctx.decoratorCallExpression() != null && ctx.decoratorCallExpression()
                                                                .arguments() != null
                        && ctx.decoratorCallExpression()
                              .arguments()
                              .argumentList()
                              .argument()
                              .size() > 0) {

                    String argText = ctx.decoratorCallExpression()
                                        .arguments()
                                        .argumentList()
                                        .argument(0)
                                        .getText();

                    String name = extractValue.apply(argText, "name");
                    if (name != null)
                        columnDetails.setColumnName(name);

                    String type = extractValue.apply(argText, "type");
                    if (type != null)
                        columnDetails.setDatabaseType(type);

                    String length = extractValue.apply(argText, "length");
                    if (length != null && !length.isEmpty()) {
                        try {
                            columnDetails.setLength(Integer.parseInt(length));
                        } catch (NumberFormatException e) {
                            System.err.println("Warning: Could not parse length value: " + length);
                        }
                    }

                    String nullable = extractValue.apply(argText, "nullable");
                    if (nullable != null)
                        columnDetails.setNullable(Boolean.parseBoolean(nullable));

                    String defaultValue = extractValue.apply(argText, "defaultValue");
                    if (defaultValue != null)
                        columnDetails.setDefaultValue(defaultValue);
                }
            } else if ("OneToMany".equals(decoratorName)) {
                // Expects @OneToMany(() => OrderItem, { table: '...', joinColumn: '...', ... })
                if (ctx.decoratorCallExpression() != null && ctx.decoratorCallExpression()
                                                                .arguments() != null
                        && ctx.decoratorCallExpression()
                              .arguments()
                              .argumentList()
                              .argument()
                              .size() >= 1) {

                    List<TypeScriptParser.ArgumentContext> args = ctx.decoratorCallExpression()
                                                                     .arguments()
                                                                     .argumentList()
                                                                     .argument();

                    // 1. Extract Target Class (from the first argument: () => ClassName)
                    String targetClassExpression = args.get(0)
                                                       .getText();
                    String targetClassName = targetClassExpression.replace("()", "")
                                                                  .replace("=>", "")
                                                                  .trim();

                    // Simple cleanup
                    if (targetClassName.contains(" ")) {
                        targetClassName = targetClassName.substring(targetClassName.lastIndexOf(" ") + 1)
                                                         .trim();
                    }

                    // Create and set collection details
                    CollectionDetails collectionDetails = new CollectionDetails();
                    collectionDetails.setTargetClass(targetClassName);
                    fieldMetadata.setCollectionDetails(collectionDetails);
                    fieldMetadata.setCollection(true); // Mark as a collection

                    // 2. Parse options object (second argument)
                    if (args.size() > 1) {
                        String argText = args.get(1)
                                             .getText(); // The options object text (e.g., { table: '...', ... })

                        String name = extractValue.apply(argText, "name");
                        if (name != null)
                            collectionDetails.setTableName(name);

                        String tableName = extractValue.apply(argText, "table");
                        if (tableName != null)
                            collectionDetails.setTableName(tableName);

                        String joinColumn = extractValue.apply(argText, "joinColumn");
                        if (joinColumn != null)
                            collectionDetails.setJoinColumn(joinColumn);

                        String cascade = extractValue.apply(argText, "cascade");
                        if (cascade != null)
                            collectionDetails.setCascade(cascade);

                        String inverse = extractValue.apply(argText, "inverse");
                        if (inverse != null)
                            collectionDetails.setInverse(Boolean.parseBoolean(inverse));

                        String lazy = extractValue.apply(argText, "lazy");
                        if (lazy != null)
                            collectionDetails.setLazy(Boolean.parseBoolean(lazy));

                        String fetch = extractValue.apply(argText, "fetch");
                        if (fetch != null)
                            collectionDetails.setFetch(fetch);

                        String joinColumnNotNull = extractValue.apply(argText, "joinColumnNotNull");
                        if (joinColumnNotNull != null)
                            collectionDetails.setJoinColumnNotNull(Boolean.parseBoolean(joinColumnNotNull));
                    }
                }
            }
        }
    }

}
