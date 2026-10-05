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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Accessibility rules of the Harmonia templates that the axe-core scan of a generated application
 * ({@code AccessibilityHarmoniaIT}) cannot reach, because no intent application renders the markup
 * - the perspective list view exists only for hand-authored models (#7645).
 */
class HarmoniaAccessibilityMarkupIT {

    private static final List<String> TEMPLATE_PATTERNS =
            List.of("classpath*:META-INF/dirigible/template-application-ui-harmonia-java/**/*.template",
                    "classpath*:META-INF/dirigible/template-form-builder-harmonia/**/*.template");

    /** The opening tag of a table header cell. */
    private static final Pattern TABLE_HEAD = Pattern.compile("<th\\b[^>]*>");

    /** The opening tag of a step indicator trigger. */
    private static final Pattern STEP_TRIGGER = Pattern.compile("<[a-z]+\\b[^>]*\\bx-h-step-indicator-trigger\\b[^>]*>");

    @Test
    void aSortableHeaderSortsThroughAButton() throws IOException {
        List<String> problems = scan(TABLE_HEAD, tag -> tag.contains("@click"));
        assertTrue(problems.isEmpty(), "a header cell that sorts on click is neither focusable nor operable from the keyboard - "
                + "put a <button type=\"button\"> inside it and bind :aria-sort on the cell:\n" + String.join("\n", problems));
    }

    @Test
    void theActiveStepIsAnnounced() throws IOException {
        List<String> problems = scan(STEP_TRIGGER, tag -> !tag.contains(":aria-current"));
        assertTrue(problems.isEmpty(), "Harmonia's step indicator marks the active step visually only - "
                + "bind :aria-current=\"... ? 'step' : null\" on each trigger:\n" + String.join("\n", problems));
    }

    private static List<String> scan(Pattern tags, Predicate<String> isProblem) throws IOException {
        List<String> problems = new ArrayList<>();
        int templates = 0;
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        for (String pattern : TEMPLATE_PATTERNS) {
            for (Resource template : resolver.getResources(pattern)) {
                templates++;
                Matcher tag = tags.matcher(template.getContentAsString(StandardCharsets.UTF_8));
                while (tag.find()) {
                    if (isProblem.test(tag.group())) {
                        problems.add(template.getFilename() + ": " + tag.group());
                    }
                }
            }
        }
        assertTrue(templates > 20, "only " + templates + " Harmonia templates found on the classpath");
        return problems;
    }
}
