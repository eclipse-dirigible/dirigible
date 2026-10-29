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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.eclipse.dirigible.components.ide.workspace.domain.File;
import org.junit.jupiter.api.Test;

/**
 * The base-name descriptor a project generated before #7057 carries is removed once a generation
 * has replaced it - and only when it records a template the platform no longer ships.
 */
class ModelGenerationServiceRetiredDescriptorTest {

    @Test
    void theLegacyDescriptorIsNamedAfterTheModelsBaseName() {
        assertThat(ModelGenerationService.legacyDescriptorPath("codbex-salaries.model")).isEqualTo("codbex-salaries.gen");
        assertThat(ModelGenerationService.legacyDescriptorPath("models/orders.model")).isEqualTo("models/orders.gen");
    }

    @Test
    void aModelFileWithoutAnExtensionHasNoLegacyDescriptor() {
        assertThat(ModelGenerationService.legacyDescriptorPath("orders")).isNull();
        assertThat(ModelGenerationService.legacyDescriptorPath("models.v2/orders")).isNull();
    }

    @Test
    void aDescriptorRecordingARetiredTemplateIsRecognised() {
        assertThat(ModelGenerationService.recordsRetiredTemplate(
                descriptor("{\"templateId\": \"template-application-angular/template/template.js\"}"))).isTrue();
    }

    @Test
    void aDescriptorRecordingAShippedTemplateIsKept() {
        assertThat(ModelGenerationService.recordsRetiredTemplate(
                descriptor("{\"templateId\": \"template-application-ui-harmonia-java/template/template.js\"}"))).isFalse();
    }

    @Test
    void aDescriptorThatDoesNotParseIsKept() {
        assertThat(ModelGenerationService.recordsRetiredTemplate(descriptor("not json"))).isFalse();
    }

    private static File descriptor(String content) {
        File file = mock(File.class);
        when(file.getContent()).thenReturn(content.getBytes(StandardCharsets.UTF_8));
        when(file.getPath()).thenReturn("/workspace/orders/orders.gen");
        return file;
    }
}
