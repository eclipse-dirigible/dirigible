/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.parsers.typescript;

import org.eclipse.dirigible.parsers.typescript.TypeScriptParser.ClassDeclarationContext;

public class ClassNameExtractorVisitor extends TypeScriptParserBaseVisitor<String> {

    @Override
    public String visitClassDeclaration(ClassDeclarationContext ctx) {
        if (ctx.identifier() != null) {
            return ctx.identifier()
                      .getText();
        }

        return null;
    }

    @Override
    public String visitProgram(TypeScriptParser.ProgramContext ctx) {
        for (TypeScriptParser.SourceElementContext element : ctx.sourceElements()
                                                                .sourceElement()) {
            if (element.statement() != null && element.statement()
                                                      .classDeclaration() != null) {
                if (element.statement()
                           .classDeclaration() != null) {
                    String className = visitClassDeclaration(element.statement()
                                                                    .classDeclaration());
                    if (className != null) {
                        return className;
                    }
                }
            }
        }
        return null;
    }
}
