/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.template;

import java.io.IOException;
import org.eclipse.dirigible.components.api.templates.TemplateEnginesFacade;

/**
 * Renders a template against a JSON parameter document. Three engines are available:
 *
 * <ul>
 * <li><strong>Mustache</strong> — logic-less, perfect for emails and HTML fragments.</li>
 * <li><strong>Velocity</strong> — Apache Velocity with the platform's default tool set.</li>
 * <li><strong>JavaScript</strong> — evaluates {@code ${expr}} fragments through GraalJS for the
 * full power of script expressions inside the template.</li>
 * </ul>
 *
 * {@link #generate(String, String)} picks the default engine (Mustache); the named variants are for
 * explicit choice. {@link #generate(String, String, String, String, String)} lets you change the
 * marker pair (defaults are <code>&#123;&#123; &#125;&#125;</code> for Mustache) — useful when the
 * template body itself contains delimiter characters.
 */
public final class TemplateEngines {

    private TemplateEngines() {}

    public static String generate(String template, String parametersJson) throws IOException {
        return TemplateEnginesFacade.getDefaultEngine()
                                    .generate(null, template, parametersJson);
    }

    public static String generateMustache(String template, String parametersJson) throws IOException {
        return TemplateEnginesFacade.getMustacheEngine()
                                    .generate(null, template, parametersJson);
    }

    public static String generateVelocity(String template, String parametersJson) throws IOException {
        return TemplateEnginesFacade.getVelocityEngine()
                                    .generate(null, template, parametersJson);
    }

    public static String generateJavascript(String template, String parametersJson) throws IOException {
        return TemplateEnginesFacade.getJavascriptEngine()
                                    .generate(null, template, parametersJson);
    }

    public static String generate(String location, String template, String parametersJson, String startMarker, String endMarker)
            throws IOException {
        return TemplateEnginesFacade.getDefaultEngine()
                                    .generate(location, template, parametersJson, startMarker, endMarker);
    }
}
