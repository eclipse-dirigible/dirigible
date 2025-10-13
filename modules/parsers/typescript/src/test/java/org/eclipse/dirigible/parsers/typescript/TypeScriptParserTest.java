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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class TypeScriptParserTest {

    private ParseTree setupParser(String code) {
        CharStream input = CharStreams.fromString(code);
        TypeScriptLexer lexer = new TypeScriptLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        TypeScriptParser parser = new TypeScriptParser(tokens);
        parser.setBuildParseTree(true);
        return parser.program();
    }

    @Test
    public void shouldExtractClassNameCorrectly() throws IOException {
        String tsSource = IOUtils.toString(TypeScriptParserTest.class.getResourceAsStream("/Car.ts"), StandardCharsets.UTF_8);
        ParseTree tree = setupParser(tsSource);

        // 2. Instantiate and run the custom visitor
        ClassNameExtractorVisitor visitor = new ClassNameExtractorVisitor();

        // The visit method returns the result of the overridden visitProgram, which is the class name
        String actualClassName = visitor.visit(tree);

        // 3. Assert the result
        assertNotNull("The class name should not be null.", actualClassName);
        assertEquals("The extracted class name should be 'Car'.", "Car", actualClassName);
    }

}
