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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * The required {@code *} marker on every generated Harmonia form surface (dirigible #7155).
 *
 * <p>
 * A colour class that Harmonia does not define fails in the one way nothing catches: the marker
 * still renders, in the inherited text colour, so it reads as a literal asterisk beside the label
 * rather than as the "this one is required" signal - and it does so only on the surfaces carrying
 * the wrong token, which is how the four self-service views drifted to {@code text-destructive} (a
 * Tailwind-shaped name with zero occurrences in the pinned Harmonia dist) while the power form and
 * document views used {@code text-negative}.
 *
 * <p>
 * Two halves, and the drift needs both: every marker on every surface carries the same class, and
 * that class is one the pinned Harmonia stylesheet really defines. The stylesheet is read out of
 * the webjar on the test classpath rather than restated here, so a Harmonia version that renamed
 * the token fails this test instead of quietly greying out every asterisk in the fleet.
 */
class HarmoniaRequiredMarkerIT {

    private static final String UI_BASE = "/META-INF/dirigible/template-application-ui-harmonia-java/ui/";

    /** Every generated surface that renders a required marker, power and self-service alike. */
    private static final List<String> MARKER_SURFACES = List.of("perspective/manage/form-view.html.template",
            "perspective/document/document-view.html.template", "my/my-form-view.html.template", "my/my-document-view.html.template",
            "partner/partner-form-view.html.template", "partner/partner-document-view.html.template", "admin/admin-index.html.template");

    /** The marker span, whichever order its class and Alpine attributes happen to be written in. */
    private static final Pattern MARKER = Pattern.compile("<span([^>]*)>\\s*\\*</span>");

    private static final Pattern CLASS = Pattern.compile("class=\"([^\"]*)\"");

    private static final String MARKER_CLASS = "text-negative";

    /**
     * A surface matching no marker at all is a failure too: the markup would have moved far enough for
     * the sweep to pass over it, which is how a drifted token stays invisible.
     */
    @Test
    void everyRequiredMarkerCarriesTheSameColourClass() throws Exception {
        for (String surface : MARKER_SURFACES) {
            String content = read(UI_BASE + surface);

            List<String> classes = markerClasses(content);
            assertFalse(classes.isEmpty(), surface + " renders no required marker at all - has the markup moved?");
            for (String marker : classes) {
                assertTrue(marker.contains(MARKER_CLASS),
                        surface + " colours a required marker with [" + marker + "] instead of " + MARKER_CLASS);
            }
        }
    }

    /**
     * Harmonia's palette tokens are {@code negative} / {@code positive} / {@code warning} /
     * {@code information}; a name borrowed from another Tailwind preset resolves to nothing and the
     * browser reports no error.
     */
    @Test
    void theMarkerColourClassIsDefinedByThePinnedHarmoniaStylesheet() throws Exception {
        assertTrue(readHarmoniaStylesheet().contains("." + MARKER_CLASS + "{"),
                MARKER_CLASS + " is not defined by the pinned Harmonia dist - the marker would render in the inherited colour");
    }

    private static List<String> markerClasses(String content) {
        List<String> classes = new ArrayList<>();
        Matcher markers = MARKER.matcher(content);
        while (markers.find()) {
            Matcher attribute = CLASS.matcher(markers.group(1));
            classes.add(attribute.find() ? attribute.group(1) : "");
        }
        return classes;
    }

    /**
     * Read the stylesheet out of the {@code codbex__harmonia} webjar on the classpath. Its resource
     * path carries the version, so the entry is located by its tail rather than named outright.
     */
    private static String readHarmoniaStylesheet() throws IOException {
        for (String entry : System.getProperty("java.class.path")
                                  .split(File.pathSeparator)) {
            if (!new File(entry).getName()
                                .startsWith("codbex__harmonia")) {
                continue;
            }
            try (JarFile jar = new JarFile(entry)) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry candidate = entries.nextElement();
                    if (candidate.getName()
                                 .endsWith("/dist/harmonia.css")) {
                        try (InputStream content = jar.getInputStream(candidate)) {
                            return new String(content.readAllBytes(), StandardCharsets.UTF_8);
                        }
                    }
                }
            }
            throw new IllegalStateException("The codbex__harmonia webjar [" + entry + "] carries no dist/harmonia.css");
        }
        throw new IllegalStateException("The codbex__harmonia webjar is not on the test classpath");
    }

    private static String read(String resource) throws IOException {
        try (InputStream content = HarmoniaRequiredMarkerIT.class.getResourceAsStream(resource)) {
            assertNotNull(content, "Missing template resource " + resource);
            return new String(content.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
