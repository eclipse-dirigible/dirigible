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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JavaSourceParserTest {

    @Test
    void parses_package_and_class_name() {
        JavaSourceParser.ParsedSource p = JavaSourceParser.parse("""
                package com.example.foo;

                public class Hello {
                }
                """);

        assertEquals("com.example.foo", p.packageName());
        assertEquals("Hello", p.simpleName());
        assertEquals("com.example.foo.Hello", p.fqn());
    }

    @Test
    void parses_default_package() {
        JavaSourceParser.ParsedSource p = JavaSourceParser.parse("class Bare {}");
        assertEquals("", p.packageName());
        assertEquals("Bare", p.simpleName());
        assertEquals("Bare", p.fqn());
    }

    @Test
    void ignores_package_token_inside_strings_and_comments() {
        JavaSourceParser.ParsedSource p = JavaSourceParser.parse("""
                // package fake.one;
                /* package fake.two; */
                package real.three;
                public final class Target {
                    String s = "package fake.four;";
                }
                """);
        assertEquals("real.three", p.packageName());
        assertEquals("Target", p.simpleName());
    }

    @Test
    void recognises_records_interfaces_enums() {
        assertEquals("R", JavaSourceParser.parse("public record R(int x) {}")
                                          .simpleName());
        assertEquals("I", JavaSourceParser.parse("interface I {}")
                                          .simpleName());
        assertEquals("E", JavaSourceParser.parse("enum E { A, B }")
                                          .simpleName());
    }

    @Test
    void parses_a_source_carrying_a_very_long_escaped_string_literal() {
        // The shape that killed synchronization platform-wide: a generated report repository whose
        // QUERY constant is one single-line SQL string full of escaped identifier quotes. The
        // regex-based stripper spent one stack frame per escape and overflowed the thread stack from a
        // few hundred escapes on - and the StackOverflowError escaped the whole SynchronizationJob.
        StringBuilder literal = new StringBuilder("\"SELECT ");
        for (int i = 0; i < 20_000; i++) {
            literal.append("\\\"COLUMN_")
                   .append(i)
                   .append("\\\", ");
        }
        literal.append("1\"");
        String source = "package gen.report;\n" + "public final class BigQueryRepository {\n" + "    static final String QUERY = " + literal
                + ";\n" + "}\n";
        assertTrue(source.length() > 100_000, "the literal must be well past any plausible stack budget");

        JavaSourceParser.ParsedSource p = JavaSourceParser.parse(source);

        assertEquals("gen.report.BigQueryRepository", p.fqn());
    }

    @Test
    void a_comment_opener_inside_a_string_literal_does_not_open_a_comment() {
        // Stripping comments before literals got the nesting backwards: the unterminated /* inside the
        // annotation value paired with the javadoc's */ and swallowed the type declaration with it.
        JavaSourceParser.ParsedSource p = JavaSourceParser.parse("""
                package real.pkg;
                @SuppressWarnings("/*")
                public class Target {
                    /** Javadoc. */
                    void m() {}
                }
                """);

        assertEquals("real.pkg", p.packageName());
        assertEquals("Target", p.simpleName());
    }

    @Test
    void ignores_a_type_declaration_inside_a_text_block() {
        // A text block is a literal too - scanned as a pair of empty strings it leaks its content into
        // the code stream, and content that happens to declare a type wins the first-match race.
        String source = "package real.pkg;\n" + "@SuppressWarnings(\"\"\"\n" + "        class Fake {}\n" + "        \"\"\")\n"
                + "public class Target {}\n";

        JavaSourceParser.ParsedSource p = JavaSourceParser.parse(source);

        assertEquals("Target", p.simpleName());
    }

    @Test
    void throws_when_no_type_declaration_present() {
        assertThrows(JavaSourceParser.JavaSourceParseException.class, () -> JavaSourceParser.parse("package only.this;\n"));
    }

}
