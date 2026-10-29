/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.template.service.model;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The generation templates the platform no longer ships, and the Java template that replaces each.
 *
 * <p>
 * The AngularJS and TypeScript application stacks were removed in 14.16.0 (#6588), leaving one
 * stack: a Harmonia UI over a client-Java backend. A project generated before that still records
 * one of their ids in its {@code .gen} descriptor, so its Regenerate names a template that is gone.
 * Such an id is answered with its successor rather than with a bare "not supported", which is what
 * turns the regeneration of a TypeScript project into a single, confirmed action (#8051). A retired
 * template without a successor - a feed, a UI generated without its backend - has no Java
 * counterpart, and the project has to choose a template instead.
 */
final class RetiredTemplates {

    /** The full-stack Java application: schema, repositories, controllers and a Harmonia UI. */
    private static final String HARMONIA_APPLICATION = "template-application-ui-harmonia-java/template/template.js";

    /** The Java repositories and entities. */
    private static final String JAVA_DAO = "template-application-dao-java/template/template.js";

    /** The Java controllers, together with the repositories they need. */
    private static final String JAVA_REST = "template-application-rest-java/template/template.js";

    /** The Harmonia form builder. */
    private static final String HARMONIA_FORM = "template-form-builder-harmonia/template/template.js";

    /** Each retired template with a Java successor, keyed by its module path. */
    private static final Map<String, String> SUCCESSORS = Map.ofEntries(//
            Map.entry("template-application-angular/template/template.js", HARMONIA_APPLICATION), //
            Map.entry("template-application-angular-v2/template/template.js", HARMONIA_APPLICATION), //
            Map.entry("template-application-angular-java/template/template.js", HARMONIA_APPLICATION), //
            Map.entry("template-application-dao/template/template.js", JAVA_DAO), //
            Map.entry("template-application-dao-v2/template/template.js", JAVA_DAO), //
            Map.entry("template-application-data/template/template.js", JAVA_DAO), //
            Map.entry("template-application-data-v2/template/template.js", JAVA_DAO), //
            Map.entry("template-application-rest/template/template.js", JAVA_REST), //
            Map.entry("template-application-rest-v2/template/template.js", JAVA_REST), //
            Map.entry("template-form-builder-angularjs/template/template.js", HARMONIA_FORM));

    /** The retired templates nothing replaces. */
    private static final Set<String> WITHOUT_SUCCESSOR = Set.of(//
            "template-application-ui-angular/template/template.js", //
            "template-application-ui-angular-v2/template/template.js", //
            "template-application-ui-angular-java/template/template.js", //
            "template-application-feed/template/template.js", //
            "template-application-feed-v2/template/template.js");

    private RetiredTemplates() {}

    /**
     * Tests whether a template id names a template the platform no longer ships.
     *
     * @param templateId the template's module path
     * @return true when it was retired
     */
    static boolean isRetired(String templateId) {
        return templateId != null && (SUCCESSORS.containsKey(templateId) || WITHOUT_SUCCESSOR.contains(templateId));
    }

    /**
     * The Java template that replaces a retired one.
     *
     * @param templateId the retired template's module path
     * @return its successor, or empty when it has none or was never retired
     */
    static Optional<String> successorOf(String templateId) {
        return templateId == null ? Optional.empty() : Optional.ofNullable(SUCCESSORS.get(templateId));
    }
}
