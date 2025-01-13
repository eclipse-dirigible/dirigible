/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.pdf;

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Class PDFFacadeTest.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ComponentScan(basePackages = {"org.eclipse.dirigible.components.*"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PDFFacadeTest {

    /**
     * Generate pdf test.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Test
    public void generatePdfTest() throws IOException {
        String template = IOUtils.toString(getClass().getClassLoader()
                                                     .getResourceAsStream("template.xsl"),
                Charset.defaultCharset());
        String data = IOUtils.toString(getClass().getClassLoader()
                                                 .getResourceAsStream("data.xml"),
                Charset.defaultCharset());

        byte[] pdf = PDFFacade.generate(template.toString(), data.toString());

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    /**
     * Generate larger pdf test.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Test
    public void generateLargerPdfTest() throws IOException {
        String template = IOUtils.toString(getClass().getClassLoader()
                                                     .getResourceAsStream("template.xsl"),
                Charset.defaultCharset());
        String data = IOUtils.toString(getClass().getClassLoader()
                                                 .getResourceAsStream("data.xml"),
                Charset.defaultCharset());

        byte[] pdf = PDFFacade.generate(template, data);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @SpringBootApplication
    static class TestConfiguration {
    }
}
