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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.engine.java.runtime.JavaSourceCompiler.BatchResult;
import org.eclipse.dirigible.engine.java.runtime.JavaSourceCompiler.SourceUnit;
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

    @Test
    void exposes_structured_diagnostics_with_position_for_a_failure() {
        BatchResult result = compiler.compileBatch(List.of(new SourceUnit("com.example.Broken", """
                package com.example;
                public class Broken {
                    public void method() {
                        notARealMethod();
                    }
                }
                """)));

        assertTrue(result.failures()
                         .containsKey("com.example.Broken"));
        List<CompileDiagnostic> diagnostics = result.diagnostics()
                                                    .get("com.example.Broken");
        assertNotNull(diagnostics, "structured diagnostics should accompany the failure");
        assertFalse(diagnostics.isEmpty());
        CompileDiagnostic first = diagnostics.get(0);
        assertTrue(first.error(), "the diagnostic should be an error");
        assertEquals(4, first.line(), "the error should be reported on the offending source line");
        assertTrue(first.column() > 0, "a positioned diagnostic should carry a column");
        assertNotNull(first.message());
    }

    @Test
    void successful_compilation_has_no_diagnostics() {
        BatchResult result = compiler.compileBatch(List.of(new SourceUnit("com.example.Ok", """
                package com.example;
                public class Ok {}
                """)));
        assertTrue(result.failures()
                         .isEmpty());
        assertTrue(result.diagnostics()
                         .isEmpty());
    }

    /**
     * javac emits no class file at all for a batch that holds an error, so before the salvage rounds a
     * single dangling import took every other unit's output down with it - and on a first publish there
     * is no last-good bytecode for {@code JavaLoader} to fall back on, which is how a project ended up
     * with no controllers, no repositories and no listeners at all (#7192).
     */
    @Test
    void an_unresolvable_import_in_one_unit_does_not_zero_the_batch() {
        BatchResult result = compiler.compileBatch(List.of(new SourceUnit("com.example.Broken", """
                package com.example;
                import com.example.missing.Absent;
                public class Broken {
                    public Absent get() { return null; }
                }
                """), new SourceUnit("com.example.Independent", """
                package com.example;
                public class Independent {
                    public String greet() { return "hello"; }
                }
                """)));

        assertEquals(Set.of("com.example.Broken"), result.failures()
                                                         .keySet());
        assertNotNull(result.bytecode()
                            .get("com.example.Independent"));
        assertFalse(result.bytecode()
                          .containsKey("com.example.Broken"));
        assertFalse(result.diagnostics()
                          .get("com.example.Broken")
                          .isEmpty());
    }

    @Test
    void a_dependent_of_a_broken_unit_fails_with_it_and_the_rest_still_compiles() {
        BatchResult result = compiler.compileBatch(List.of(new SourceUnit("com.example.Broken", """
                package com.example;
                import com.example.missing.Absent;
                public class Broken {
                    public Absent get() { return null; }
                }
                """), new SourceUnit("com.example.Dependent", """
                package com.example;
                public class Dependent {
                    private final Broken broken = new Broken();
                    public Broken get() { return broken; }
                }
                """), new SourceUnit("com.example.Independent", """
                package com.example;
                public class Independent {
                    public String greet() { return "hello"; }
                }
                """)));

        assertEquals(Set.of("com.example.Broken", "com.example.Dependent"), result.failures()
                                                                                  .keySet());
        assertNotNull(result.bytecode()
                            .get("com.example.Independent"));
        // the dependent is reported with its own diagnostic, not with the generic "no class file"
        assertTrue(result.failures()
                         .get("com.example.Dependent")
                         .contains("cannot find symbol"));
    }

}
