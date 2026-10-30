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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * A retired template is answered with the Java template that replaces it - and every successor is a
 * template the Java pipeline actually generates.
 */
class RetiredTemplatesTest {

    @Test
    void theTypeScriptApplicationIsReplacedByTheHarmoniaJavaApplication() {
        String retired = "template-application-angular/template/template.js";

        assertThat(RetiredTemplates.isRetired(retired)).isTrue();
        assertThat(RetiredTemplates.successorOf(retired)).contains("template-application-ui-harmonia-java/template/template.js");
    }

    @Test
    void theAngularJsFormBuilderIsReplacedByTheHarmoniaOne() {
        assertThat(RetiredTemplates.successorOf("template-form-builder-angularjs/template/template.js")).contains(
                "template-form-builder-harmonia/template/template.js");
    }

    @Test
    void aRetiredTemplateWithoutAJavaCounterpartHasNoSuccessor() {
        String retired = "template-application-feed/template/template.js";

        assertThat(RetiredTemplates.isRetired(retired)).isTrue();
        assertThat(RetiredTemplates.successorOf(retired)).isEmpty();
    }

    @Test
    void aShippedTemplateIsNotRetired() {
        assertThat(RetiredTemplates.isRetired("template-application-ui-harmonia-java/template/template.js")).isFalse();
        assertThat(RetiredTemplates.isRetired("template-mapping-javascript/template/template.js")).isFalse();
    }

    @Test
    void everySuccessorIsGeneratedByTheJavaPipeline() {
        for (String retired : new String[] {"template-application-angular/template/template.js",
                "template-application-angular-v2/template/template.js", "template-application-angular-java/template/template.js",
                "template-application-dao/template/template.js", "template-application-dao-v2/template/template.js",
                "template-application-data/template/template.js", "template-application-data-v2/template/template.js",
                "template-application-rest/template/template.js", "template-application-rest-v2/template/template.js",
                "template-form-builder-angularjs/template/template.js"}) {
            assertThat(RetiredTemplates.successorOf(retired)).hasValueSatisfying(
                    successor -> assertThat(ModelTemplateAdapters.supports(successor)).as(successor)
                                                                                      .isTrue());
        }
    }
}
