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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.apache.fop.apps.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

/**
 * The Class PDFFacade.
 */
@Component
public class PDFFacade {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(PDFFacade.class);

    /**
     * The classpath folder of the bundled Unicode fonts. FOP's built-in base-14 fonts (Helvetica,
     * Times, Courier) are metric-only and Latin-1 encoded, so any non-Latin glyph (e.g. Cyrillic)
     * renders as '#'. DejaVu Sans is embedded as a subset to cover Latin, Cyrillic and Greek;
     * stylesheets opt in via font-family="DejaVu Sans, Helvetica".
     */
    private static final String FONTS_RESOURCE_FOLDER = "/META-INF/fonts/dejavu/";

    /** The bundled font files. */
    private static final String[] FONT_FILES =
            {"DejaVuSans.ttf", "DejaVuSans-Bold.ttf", "DejaVuSans-Oblique.ttf", "DejaVuSans-BoldOblique.ttf"};

    /** The FOP configuration registering the bundled fonts for the PDF renderer. */
    private static final String FOP_CONFIG = """
            <fop version="1.0">
                <renderers>
                    <renderer mime="application/pdf">
                        <fonts>
                            <font kerning="yes" embed-url="DejaVuSans.ttf" embedding-mode="subset">
                                <font-triplet name="DejaVu Sans" style="normal" weight="normal"/>
                            </font>
                            <font kerning="yes" embed-url="DejaVuSans-Bold.ttf" embedding-mode="subset">
                                <font-triplet name="DejaVu Sans" style="normal" weight="bold"/>
                            </font>
                            <font kerning="yes" embed-url="DejaVuSans-Oblique.ttf" embedding-mode="subset">
                                <font-triplet name="DejaVu Sans" style="italic" weight="normal"/>
                            </font>
                            <font kerning="yes" embed-url="DejaVuSans-BoldOblique.ttf" embedding-mode="subset">
                                <font-triplet name="DejaVu Sans" style="italic" weight="bold"/>
                            </font>
                        </fonts>
                    </renderer>
                </renderers>
            </fop>""";

    /** The shared factory (the font configuration is parsed once). */
    private static volatile FopFactory fopFactory;

    /**
     * Generate.
     *
     * @param template the template
     * @param data the data
     * @return the byte[]
     */
    public static byte[] generate(String template, String data) {
        if (logger.isInfoEnabled()) {
            logger.info("Generating PDF from template: [\n{}\n] and data: [\n{}\n]", template, data);
        }

        try {
            StreamSource templateSource = new StreamSource(IOUtils.toInputStream(template, Charset.defaultCharset()));
            StreamSource dataSource = new StreamSource(IOUtils.toInputStream(data, Charset.defaultCharset()));

            FopFactory fopFactory = getFopFactory();
            FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, baos);

            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(templateSource);

            Result result = new SAXResult(fop.getDefaultHandler());

            transformer.transform(dataSource, result);
            return baos.toByteArray();
        } catch (FOPException | TransformerException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage());
            }
            throw new PDFException(e.getMessage(), e);
        }
    }

    /**
     * Gets the shared FOP factory with the bundled Unicode fonts registered. Falls back to an
     * unconfigured factory (base-14 fonts only) if the fonts cannot be registered.
     *
     * @return the FOP factory
     */
    private static FopFactory getFopFactory() {
        FopFactory factory = fopFactory;
        if (factory == null) {
            synchronized (PDFFacade.class) {
                factory = fopFactory;
                if (factory == null) {
                    factory = createFopFactory();
                    fopFactory = factory;
                }
            }
        }
        return factory;
    }

    /**
     * Creates the FOP factory. The bundled TTF files are extracted from the classpath to a temporary
     * directory, which serves as the base URI the configuration's embed-url entries resolve against
     * (FOP cannot load fonts from inside a jar directly).
     *
     * @return the FOP factory
     */
    private static FopFactory createFopFactory() {
        try {
            Path fontsDir = extractFonts();
            try (InputStream config = new ByteArrayInputStream(FOP_CONFIG.getBytes(StandardCharsets.UTF_8))) {
                FopConfParser parser = new FopConfParser(config, fontsDir.toUri());
                return parser.getFopFactoryBuilder()
                             .build();
            }
        } catch (IOException | SAXException e) {
            if (logger.isWarnEnabled()) {
                logger.warn("Failed to register the bundled Unicode fonts, falling back to the FOP base-14 fonts"
                        + " (non-Latin characters will render as '#'): {}", e.getMessage(), e);
            }
            return FopFactory.newInstance(new File(".").toURI());
        }
    }

    /**
     * Extract the bundled fonts to a temporary directory.
     *
     * @return the directory the fonts were extracted to
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static Path extractFonts() throws IOException {
        Path fontsDir = Files.createTempDirectory("dirigible-fop-fonts");
        fontsDir.toFile()
                .deleteOnExit();
        for (String fontFile : FONT_FILES) {
            try (InputStream font = PDFFacade.class.getResourceAsStream(FONTS_RESOURCE_FOLDER + fontFile)) {
                if (font == null) {
                    throw new IOException("Bundled font not found on the classpath: " + FONTS_RESOURCE_FOLDER + fontFile);
                }
                Path target = fontsDir.resolve(fontFile);
                Files.copy(font, target, StandardCopyOption.REPLACE_EXISTING);
                target.toFile()
                      .deleteOnExit();
            }
        }
        return fontsDir;
    }
}
