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
    void throws_when_no_type_declaration_present() {
        assertThrows(JavaSourceParser.JavaSourceParseException.class, () -> JavaSourceParser.parse("package only.this;\n"));
    }

}
