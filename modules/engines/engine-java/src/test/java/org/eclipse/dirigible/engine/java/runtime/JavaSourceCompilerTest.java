/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class JavaSourceCompilerTest {

    private final JavaSourceCompiler compiler = new JavaSourceCompiler();

    @Test
    void compiles_a_simple_class() {
        Map<String, byte[]> classes = compiler.compile("com.example.Hello", """
                package com.example;
                public class Hello {
                    public String greet() { return "hello"; }
                }
                """);
        assertEquals(1, classes.size());
        byte[] bytes = classes.get("com.example.Hello");
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test
    void emits_nested_class_bytecode() {
        Map<String, byte[]> classes = compiler.compile("com.example.Outer", """
                package com.example;
                public class Outer {
                    public static class Inner {}
                }
                """);
        assertNotNull(classes.get("com.example.Outer"));
        assertNotNull(classes.get("com.example.Outer$Inner"));
    }

    @Test
    void surfaces_diagnostics_on_compilation_error() {
        JavaCompilationException ex = assertThrows(JavaCompilationException.class, () -> compiler.compile("com.example.Broken", """
                package com.example;
                public class Broken {
                    public void method() {
                        notARealMethod();
                    }
                }
                """));
        assertTrue(ex.getMessage()
                     .contains("Broken"),
                "diagnostic should reference the offending class");
    }

}
