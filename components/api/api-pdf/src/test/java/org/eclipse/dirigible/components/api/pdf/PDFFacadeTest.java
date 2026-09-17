/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
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
import java.nio.charset.StandardCharsets;

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

    /** A 1x1 red PNG - the smallest image that proves the bytes reached the renderer. */
    private static final String ONE_PIXEL_PNG_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADElEQVR4nGP4z8AAAAMBAQDJ/pLvAAAAAElFTkSuQmCC";

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
     * Generate pdf with Cyrillic content test. The base-14 fonts have no Cyrillic glyphs (FOP renders
     * them as '#'), so the bundled DejaVu Sans must be registered and embedded (as a subset) instead.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Test
    public void generateCyrillicPdfTest() throws IOException {
        String template = IOUtils.toString(getClass().getClassLoader()
                                                     .getResourceAsStream("template-cyrillic.xsl"),
                StandardCharsets.UTF_8);
        String data = IOUtils.toString(getClass().getClassLoader()
                                                 .getResourceAsStream("data.xml"),
                Charset.defaultCharset());

        byte[] pdf = PDFFacade.generate(template, data);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        String content = new String(pdf, StandardCharsets.ISO_8859_1);
        assertTrue(content.contains("DejaVuSans"), "The bundled DejaVu Sans font should be embedded in the PDF");
    }

    /**
     * An image carried inline in a {@code data:} URI is embedded in the PDF. Inlining is the only way a
     * document can carry an image the platform holds itself (a logo in the tenant content store, a
     * record's attachment): the rendered stylesheet reaches FOP with no session, credentials or tenant
     * scope, so nothing may be fetched later. FOP reads the scheme natively
     * ({@code InternalResourceResolver}), which is what this pins - a FOP upgrade that dropped it would
     * silently stop printing every logo.
     */
    @Test
    public void generatePdfWithInlineImageTest() {
        String pngDataUri = "data:image/png;base64," + ONE_PIXEL_PNG_BASE64;
        String template = """
                <?xml version="1.0" encoding="UTF-8"?>
                <xsl:stylesheet version="1.1" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                                xmlns:fo="http://www.w3.org/1999/XSL/Format">
                    <xsl:template match="/">
                        <fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
                            <fo:layout-master-set>
                                <fo:simple-page-master master-name="page" page-height="29.7cm" page-width="21cm">
                                    <fo:region-body/>
                                </fo:simple-page-master>
                            </fo:layout-master-set>
                            <fo:page-sequence master-reference="page">
                                <fo:flow flow-name="xsl-region-body">
                                    <fo:block>
                                        <fo:external-graphic src="%s" content-width="120pt"/>
                                    </fo:block>
                                </fo:flow>
                            </fo:page-sequence>
                        </fo:root>
                    </xsl:template>
                </xsl:stylesheet>
                """.formatted(pngDataUri);

        byte[] pdf = PDFFacade.generate(template, "<data/>");

        assertNotNull(pdf);
        String content = new String(pdf, StandardCharsets.ISO_8859_1);
        assertTrue(content.contains("/Subtype /Image"), "The inline image should be embedded as a PDF image XObject");
        String noImage = new String(
                PDFFacade.generate(template.replace("data:image/png;base64," + ONE_PIXEL_PNG_BASE64, "nope:///x.png"), "<data/>"),
                StandardCharsets.ISO_8859_1);
        assertTrue(!noImage.contains("/Subtype /Image"),
                "An unreadable graphic renders nothing and does not abort the PDF - which is what makes a missing image fail-soft");
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
