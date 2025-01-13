/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api.javascript.cms;

import org.eclipse.dirigible.components.engine.javascript.service.JavascriptService;
import org.eclipse.dirigible.integration.tests.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

@WithMockUser
public class CmsSuiteIT extends IntegrationTest {

    @Autowired
    private JavascriptService javascriptService;

    @Test
    void executeCMISTest() {
        javascriptService.handleRequest("cms-tests", "cmis-create-document.js", null, null, false);
        javascriptService.handleRequest("cms-tests", "cmis-create-folder.js", null, null, false);
        javascriptService.handleRequest("cms-tests", "cmis-get-children.js", null, null, false);
        javascriptService.handleRequest("cms-tests", "cmis-get-root-folder.js", null, null, false);
        javascriptService.handleRequest("cms-tests", "cmis-get-session.js", null, null, false);
    }
}
