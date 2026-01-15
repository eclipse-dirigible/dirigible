/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.openapi.generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.dirigible.components.data.store.model.EntityFieldMetadata;
import org.eclipse.dirigible.components.data.store.model.EntityMetadata;
import org.eclipse.dirigible.components.data.store.parser.EntityParser;
import org.eclipse.dirigible.parsers.typescript.TypeScriptLexer;
import org.eclipse.dirigible.parsers.typescript.TypeScriptParser;
import org.eclipse.dirigible.parsers.typescript.TypeScriptParserBaseVisitor;

/**
 * Parses TypeScript Controller source files and generates an OpenAPI (Swagger) specification JSON.
 */
public class OpenApiParser {

    public static final Map<String, Map<String, Object>> SCHEMAS = new HashMap<>();

    static {
        SCHEMAS.put("string", map("type", "string"));
        SCHEMAS.put("number", map("type", "number"));
        SCHEMAS.put("any", map("type", "object"));
    }

    private static Map<String, Object> map(Object... args) {
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "Arguments to map() must be key-value pairs (even number of arguments). Received: " + args.length);
        }
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < args.length; i += 2) {
            map.put((String) args[i], args[i + 1]);
        }
        return map;
    }

    /**
     * Entry point for parsing the OpenAPI specification.
     *
     * @param location The actual source location
     * @param source The TypeScript source code string of the controller.
     * @return The OpenAPI specification as a formatted JSON string.
     */
    public static Map<String, Object> parse(String location, String source) {
        CharStream input = CharStreams.fromString(source);
        TypeScriptLexer lexer = new TypeScriptLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        TypeScriptParser parser = new TypeScriptParser(tokens);

        ParseTree tree = parser.program();

        ControllerMetadataVisitor visitor = new ControllerMetadataVisitor(location);
        Map<String, Object> controllerMetadata = visitor.visit(tree);

        Map<String, String> entityImports = (Map<String, String>) controllerMetadata.get("entityImports");
        if (!entityImports.isEmpty()) {
            entityImports.forEach((entityName, path) -> {

                String relative = path.endsWith(".ts") ? path : path + ".ts";
                EntityMetadata entityMetadata = EntityParser.ENTITIES.get(FilenameUtils.getBaseName(relative));
                if (entityMetadata != null) {
                    Map<String, Object> entityProperties = new HashMap<>();
                    for (EntityFieldMetadata efm : entityMetadata.getFields()) {
                        entityProperties.put(efm.getPropertyName(),
                                map("type", efm.getTypeScriptType(), "description", efm.getDocumentation()));
                    }
                    SCHEMAS.put(entityMetadata.getEntityName(),
                            map("type", "object", "properties", entityProperties, "description", entityMetadata.getDocumentation()));
                } else if (relative.toLowerCase()
                                   .endsWith("entity.ts")) {
                    throw new RuntimeException("OpenAPI generator failed to load [" + location + "], because it depends on [" + relative
                            + "] which is not loaded yet.");
                }
            });
        }

        return controllerMetadata;
    }

    /**
     * Visitor to extract controller and method metadata from the ParseTree.
     */
    private static class ControllerMetadataVisitor extends TypeScriptParserBaseVisitor<Map<String, Object>> {

        private String controllerPath = "/";
        private String controllerName = "Controller";
        private String documentation = "";
        private final List<Map<String, Object>> routes = new ArrayList<>();
        private final Map<String, String> entityImports = new HashMap<>();

        public ControllerMetadataVisitor(String controllerPath) {
            super();
            this.controllerPath = controllerPath;
        }

        @Override
        public Map<String, Object> visitProgram(TypeScriptParser.ProgramContext ctx) {
            super.visitProgram(ctx);

            Map<String, Object> result = new HashMap<>();
            result.put("path", controllerPath);
            result.put("name", controllerName);
            result.put("description", documentation);
            result.put("routes", routes);
            result.put("entityImports", entityImports);
            return result;
        }

        @Override
        public Map<String, Object> visitImportStatement(TypeScriptParser.ImportStatementContext ctx) {

            TypeScriptParser.ImportFromBlockContext fromBlock = ctx.importFromBlock();
            String importPath = "";

            if (fromBlock.importFrom() != null && fromBlock.importFrom()
                                                           .StringLiteral() != null) {
                importPath = fromBlock.importFrom()
                                      .StringLiteral()
                                      .getText()
                                      .replaceAll("['\"]", "");
            } else if (fromBlock.StringLiteral() != null) {
                importPath = fromBlock.StringLiteral()
                                      .getText()
                                      .replaceAll("['\"]", "");
            }

            if (fromBlock.importModuleItems() != null) {
                TypeScriptParser.ImportModuleItemsContext moduleItems = fromBlock.importModuleItems();

                for (TypeScriptParser.ImportAliasNameContext aliasNameCtx : moduleItems.importAliasName()) {

                    TypeScriptParser.IdentifierNameContext name = aliasNameCtx.moduleExportName()
                                                                              .identifierName();
                    String importedName = name.getText();

                    if (!importPath.contains("sdk")) {
                        entityImports.put(importedName, importPath);
                    }
                }
            }

            return super.visitImportStatement(ctx);
        }

        @Override
        public Map<String, Object> visitClassDeclaration(TypeScriptParser.ClassDeclarationContext ctx) {
            TerminalNode classNameNode = ctx.identifier()
                                            .Identifier();
            if (classNameNode != null) {
                controllerName = classNameNode.getText();
            }

            if (ctx.decoratorList() != null) {
                for (TypeScriptParser.DecoratorContext decoratorCtx : ctx.decoratorList()
                                                                         .decorator()) {
                    if ("Controller".equals(getDecoratorBaseName(decoratorCtx))) {
                        controllerPath = extractDecoratorContent(decoratorCtx, controllerPath);
                    } else if ("Documentation".equals(getDecoratorBaseName(decoratorCtx))) {
                        documentation = extractDecoratorContent(decoratorCtx, documentation);
                    }
                }
            }

            return super.visitClassDeclaration(ctx);
        }

        @Override
        public Map<String, Object> visitMethodDeclarationExpression(TypeScriptParser.MethodDeclarationExpressionContext methodDeclaration) {
            if (methodDeclaration.getParent() instanceof TypeScriptParser.ClassElementContext ctx) {

                List<TypeScriptParser.DecoratorContext> decorators = extractDecorators(ctx);
                String methodName = methodDeclaration.propertyName()
                                                     .getText();

                for (TypeScriptParser.DecoratorContext decoratorCtx : decorators) {
                    String method = getDecoratorBaseName(decoratorCtx);
                    String httpMethod = null;
                    String routePath = extractDecoratorContent(decoratorCtx, "/");
                    String fullPath = normalizePath(controllerPath + routePath);
                    String documentation = "";

                    switch (method) {
                        case "Get":
                            httpMethod = "get";
                            break;
                        case "Post":
                            httpMethod = "post";
                            break;
                        case "Put":
                            httpMethod = "put";
                            break;
                        case "Delete":
                            httpMethod = "delete";
                            break;
                        case "Documentation":
                            documentation = extractDecoratorContent(decoratorCtx, "");
                        default:
                            continue; // Skip non-HTTP decorators
                    }

                    Map<String, Object> route = new HashMap<>();
                    route.put("httpMethod", httpMethod);
                    route.put("path", fullPath);
                    route.put("methodName", methodName);
                    route.put("description", documentation);
                    route.put("tags", List.of(controllerName));

                    extractMethodDetails(methodDeclaration, route);

                    routes.add(route);
                }
            }
            return super.visitMethodDeclarationExpression(methodDeclaration);
        }

        private void extractMethodDetails(TypeScriptParser.MethodDeclarationExpressionContext methodCtx, Map<String, Object> route) {
            List<Map<String, Object>> parameters = new ArrayList<>();
            String requestBodyRef = null;

            if (methodCtx.callSignature() != null && methodCtx.callSignature()
                                                              .parameterList() != null) {

                List<TypeScriptParser.ParameterContext> parameterContexts = methodCtx.callSignature()
                                                                                     .parameterList()
                                                                                     .parameter();

                List<TypeScriptParser.RequiredParameterContext> params = new ArrayList<>();

                for (TypeScriptParser.ParameterContext paramCtx : parameterContexts) {
                    ParseTree firstChild = paramCtx.getChild(0);
                    if (firstChild instanceof TypeScriptParser.RequiredParameterContext) {
                        params.add((TypeScriptParser.RequiredParameterContext) firstChild);
                    }
                }

                for (TypeScriptParser.RequiredParameterContext paramCtx : params) {

                    String paramName = paramCtx.getChild(0)
                                               .getText();
                    String paramType = paramCtx.typeAnnotation() != null ? paramCtx.typeAnnotation()
                                                                                   .type_()
                                                                                   .getText()
                            : "any";

                    if (route.get("path")
                             .toString()
                             .contains("{" + paramName + "}")
                            || "filter".equals(paramName)) {
                        parameters.add(map("name", paramName, "in", "path", "required", true, "schema", SCHEMAS.get(paramType)));
                    } else if ("entity".equals(paramName) || "filter".equals(paramName)) {
                        requestBodyRef = paramType.replace("[]", "");
                    } else if ("string".equals(paramType) || "number".equals(paramType)) {
                        parameters.add(map("name", paramName, "in", "query", "required", true, "schema", SCHEMAS.get(paramType)));
                    }
                }
            }
            route.put("parameters", parameters);

            if (requestBodyRef != null) {
                route.put("requestBodyRef", requestBodyRef);
            }

            String responseType = "void";
            if (methodCtx.callSignature() != null && methodCtx.callSignature()
                                                              .typeAnnotation() != null) {
                responseType = methodCtx.callSignature()
                                        .typeAnnotation()
                                        .type_()
                                        .getText();
            }

            route.put("responseType", responseType);
        }

        private String getDecoratorBaseName(TypeScriptParser.DecoratorContext ctx) {
            TypeScriptParser.DecoratorMemberExpressionContext memberCtx = ctx.decoratorCallExpression() != null
                    ? ctx.decoratorCallExpression()
                         .decoratorMemberExpression()
                    : ctx.decoratorMemberExpression();

            if (memberCtx != null) {
                String fullName = memberCtx.getText();
                return fullName.substring(fullName.lastIndexOf('.') + 1);
            }
            return null;
        }

        private String extractDecoratorContent(TypeScriptParser.DecoratorContext ctx, String defaultPath) {
            if (ctx.decoratorCallExpression() != null && ctx.decoratorCallExpression()
                                                            .arguments() != null
                    && ctx.decoratorCallExpression()
                          .arguments()
                          .argumentList() != null
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
                return arg.replaceAll("['\"]", ""); // Remove quotes
            }
            return defaultPath;
        }

        private List<TypeScriptParser.DecoratorContext> extractDecorators(TypeScriptParser.ClassElementContext ctx) {
            List<TypeScriptParser.DecoratorContext> decorators = new ArrayList<>();
            for (int i = 0; i < ctx.getChildCount(); i++) {
                ParseTree child = ctx.getChild(i);
                if (child instanceof TypeScriptParser.DecoratorListContext) {
                    decorators.addAll(((TypeScriptParser.DecoratorListContext) child).decorator());
                } else if (child instanceof TypeScriptParser.DecoratorContext) {
                    decorators.add((TypeScriptParser.DecoratorContext) child);
                }
            }
            return decorators;
        }

        private String normalizePath(String path) {
            if (path.isEmpty())
                return "/";
            // Replace /:id with /{id} for OpenAPI path templating
            return path.replaceAll("/:([^/]+)", "/{$1}");
        }
    }

}
