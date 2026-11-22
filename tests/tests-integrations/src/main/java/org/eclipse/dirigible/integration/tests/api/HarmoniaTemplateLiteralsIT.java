/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * No English literal reaches the screen or a screen reader from the generated pages (dirigible
 * #8778).
 *
 * <p>
 * A generated application shown in Bulgarian used to read out "Close", "Choose date" and "Row
 * actions": the names of its icon buttons were static {@code aria-label} / {@code title}
 * attributes, written straight into the Harmonia templates, so they never went through {@code T()}.
 * This reads every template of {@code template-application-ui-harmonia-java} and fails on a static
 * {@code aria-label} or {@code title} that starts with a capital letter, and on visible text
 * written between two tags - each must be bound ({@code :aria-label="T('<key>', 'English')"},
 * {@code x-text="T(...)"}).
 */
class HarmoniaTemplateLiteralsIT {

    private static final String TEMPLATES = "classpath*:META-INF/dirigible/template-application-ui-harmonia-java/**/*.template";

    /** A static attribute a screen reader speaks or a tooltip shows; a bound one is preceded by ':'. */
    static final Pattern STATIC_ATTRIBUTE = Pattern.compile("(?<![:\\w.-])(aria-label|title)=\"([A-Z][^\"]*)\"");

    /** English words written between two tags of a page. */
    static final Pattern VISIBLE_TEXT = Pattern.compile(">\\s*([A-Z][A-Za-z]*(?:[ ',.:!?-]+[A-Za-z]+)*[.!?]?)\\s*</");

    @Test
    void noTemplateCarriesAnUntranslatedLiteral() {
        List<String> problems = new ArrayList<>();
        List<Resource> templates = resources();
        assertFalse(templates.isEmpty(), "no Harmonia template found on the classpath: " + TEMPLATES);
        for (Resource template : templates) {
            String path = template.getFilename();
            String content = read(template);
            collect(problems, path, content, STATIC_ATTRIBUTE, 2);
            if (path != null && path.endsWith(".html.template")) {
                collect(problems, path, content, VISIBLE_TEXT, 1);
            }
        }
        if (!problems.isEmpty()) {
            fail(problems.size() + " untranslated literal(s) - bind them through T():\n  " + String.join("\n  ", problems));
        }
    }

    /** The guard is only worth its name if it catches the shapes it was written against. */
    @Test
    void aReintroducedLiteralIsCaught() {
        List<String> problems = new ArrayList<>();
        collect(problems, "sample", """
                <button x-h-date-picker-trigger aria-label="Choose date"></button>
                <iframe title="Report preview"></iframe>
                <button type="button">Cancel</button>
                <button :aria-label="T('app:defaults.close', 'Close')"></button>
                <span x-text="T('app:defaults.new', 'New')"></span>
                """, STATIC_ATTRIBUTE, 2);
        collect(problems, "sample", "<button type=\"button\">Cancel</button>\n<span x-text=\"T('k', 'Fine')\"></span>", VISIBLE_TEXT, 1);
        assertEquals(List.of("sample:1 Choose date", "sample:2 Report preview", "sample:1 Cancel"), problems);
    }

    private static void collect(List<String> problems, String path, String content, Pattern pattern, int group) {
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            int line = 1;
            for (int index = 0; index < matcher.start(); index++) {
                if (content.charAt(index) == '\n') {
                    line++;
                }
            }
            problems.add(path + ":" + line + " " + matcher.group(group));
        }
    }

    private static List<Resource> resources() {
        try {
            return List.of(new PathMatchingResourcePatternResolver().getResources(TEMPLATES));
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot resolve " + TEMPLATES, ex);
        }
    }

    private static String read(Resource resource) {
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot read " + resource, ex);
        }
    }
}
