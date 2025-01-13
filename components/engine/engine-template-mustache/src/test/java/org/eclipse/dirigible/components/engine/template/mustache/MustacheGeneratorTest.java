/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.template.mustache;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.dirigible.components.engine.template.TemplateEngine;
import org.junit.jupiter.api.Test;

import com.github.mustachejava.util.DecoratedCollection;

/**
 * The Class MustacheGeneratorTest.
 */
public class MustacheGeneratorTest {

    /**
     * Generate.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Test
    public void generate() throws IOException {
        TemplateEngine generationEngine = new MustacheGenerationEngine();
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("testParameter", "testValue");
        byte[] result = generationEngine.generate(parameters, "/location", "test {{testParameter}}".getBytes(), "{{", "}}");
        assertEquals("test testValue", new String(result));
    }

    /**
     * Generate collection decorated.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Test
    public void generateCollectionDecorated() throws IOException {
        TemplateEngine generationEngine = new MustacheGenerationEngine();
        Map<String, Object> parameters = new HashMap<String, Object>() {
            {
                put("elements", new DecoratedCollection(Arrays.asList(new HashMap<String, Object>() {
                    {
                        put("name", "name1");
                    }
                }, new HashMap<String, Object>() {
                    {
                        put("name", "name2");
                    }
                }, new HashMap<String, Object>() {
                    {
                        put("name", "name3");
                    }
                })));
            }
        };

        byte[] result = generationEngine.generate(parameters, "/location",
                "test {{#elements}}{{value.name}}{{^last}}, {{/last}}{{/elements}}".getBytes(), "{{", "}}");
        assertEquals("test name1, name2, name3", new String(result));
    }

    /**
     * Generate collection simple.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Test
    public void generateCollectionSimple() throws IOException {
        TemplateEngine generationEngine = new MustacheGenerationEngine();
        Map<String, Object> parameters = new HashMap<String, Object>() {
            {
                put("elements", Arrays.asList(new HashMap<String, Object>() {
                    {
                        put("name", "name1");
                    }
                }, new HashMap<String, Object>() {
                    {
                        put("name", "name2");
                    }
                }, new HashMap<String, Object>() {
                    {
                        put("name", "name3");
                    }
                }));
            }
        };

        byte[] result =
                generationEngine.generate(parameters, "/location", "test {{#elements}}{{name}} {{/elements}}".getBytes(), "{{", "}}");
        assertEquals("test name1 name2 name3 ", new String(result));
    }

    /**
     * Generate collection decorated default.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Test
    public void generateCollectionDecoratedDefault() throws IOException {
        TemplateEngine generationEngine = new MustacheGenerationEngine();
        Map<String, Object> parameters = new HashMap<String, Object>() {
            {
                put("elements", Arrays.asList(new HashMap<String, Object>() {
                    {
                        put("name", "name1");
                    }
                }, new HashMap<String, Object>() {
                    {
                        put("name", "name2");
                    }
                }, new HashMap<String, Object>() {
                    {
                        put("name", "name3");
                    }
                }));
            }
        };

        byte[] result = generationEngine.generate(parameters, "/location",
                "test {{#elements_}}{{value.name}}{{^last}}, {{/last}}{{/elements_}}".getBytes(), "{{", "}}");
        assertEquals("test name1, name2, name3", new String(result));
    }

    /**
     * Generate collection decorated nested.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Test
    public void generateCollectionDecoratedNested() throws IOException {
        TemplateEngine generationEngine = new MustacheGenerationEngine();
        Map<String, Object> parameters = new HashMap<String, Object>() {
            {
                put("elements", Arrays.asList(new HashMap<String, Object>() {
                    {
                        put("properties", Arrays.asList(new HashMap<String, Object>() {
                            {
                                put("table", "table1");
                            }
                        }, new HashMap<String, Object>() {
                            {
                                put("table", "table2");
                            }
                        }));
                    }
                }));
            }
        };
        byte[] result = generationEngine.generate(parameters, "/location",
                "test {{#elements_}}{{#value.properties_}}{{value.table}}{{^last}}, {{/last}}{{/value.properties_}}{{/elements_}}".getBytes(),
                "{{", "}}");
        assertEquals("test table1, table2", new String(result));
    }
}
