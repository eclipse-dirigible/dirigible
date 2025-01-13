/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api.java.camel;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.eclipse.dirigible.integration.tests.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@CamelSpringBootTest
class DirigibleJavaScriptComponentIT extends IntegrationTest {

    @Autowired
    private ProducerTemplate producerTemplate;

    @Test
    void testUsingJavaDSL() {
        String initialBody = "DirigibleJavaScriptComponentIT body";
        String result = producerTemplate.requestBody("direct:callDirigibleScript", initialBody, String.class);

        String expectedBody = initialBody.toUpperCase();
        assertThat(result).isEqualTo(expectedBody);
    }

}
