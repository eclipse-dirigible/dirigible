/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.utils;

import org.eclipse.dirigible.components.engine.javascript.service.JavascriptService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@WithMockUser
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = {"org.eclipse.dirigible.components.*"})
public class UtilsSuiteTest {

    @Autowired
    private JavascriptService javascriptService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    protected WebApplicationContext wac;

    @SpringBootApplication
    static class TestConfiguration {
    }

    @Test
    public void executeBase64Test() throws Exception {
        javascriptService.handleRequest("utils-tests", "base64-decode.js", null, null, false);
        javascriptService.handleRequest("utils-tests", "base64-encode.js", null, null, false);
    }

    @Test
    public void executeDigestTest() throws Exception {
        javascriptService.handleRequest("utils-tests", "digest-md5.js", null, null, false);
        javascriptService.handleRequest("utils-tests", "digest-md5Hex.js", null, null, false);
        javascriptService.handleRequest("utils-tests", "digest-sha1.js", null, null, false);
        javascriptService.handleRequest("utils-tests", "digest-sha1Hex.js", null, null, false);
        javascriptService.handleRequest("utils-tests", "digest-sha256.js", null, null, false);
        javascriptService.handleRequest("utils-tests", "digest-sha384.js", null, null, false);
        javascriptService.handleRequest("utils-tests", "digest-sha512.js", null, null, false);
    }

    @Test
    public void executeEscapeTest() throws Exception {
        javascriptService.handleRequest("utils-tests", "escape-csv.js", null, null, false);
        javascriptService.handleRequest("utils-tests", "escape-html3.js", null, null, false);
        javascriptService.handleRequest("utils-tests", "escape-html4.js", null, null, false);
        javascriptService.handleRequest("utils-tests", "escape-java.js", null, null, false);
        javascriptService.handleRequest("utils-tests", "escape-javascript.js", null, null, false);
        javascriptService.handleRequest("utils-tests", "escape-json.js", null, null, false);
        javascriptService.handleRequest("utils-tests", "escape-xml.js", null, null, false);
        javascriptService.handleRequest("utils-tests", "unescape-csv.js", null, null, false);
        javascriptService.handleRequest("utils-tests", "unescape-html3.js", null, null, false);
        javascriptService.handleRequest("utils-tests", "unescape-html4.js", null, null, false);
        javascriptService.handleRequest("utils-tests", "unescape-java.js", null, null, false);
        javascriptService.handleRequest("utils-tests", "unescape-javascript.js", null, null, false);
        javascriptService.handleRequest("utils-tests", "unescape-json.js", null, null, false);
        javascriptService.handleRequest("utils-tests", "unescape-xml.js", null, null, false);
    }

    @Test
    public void executeHexTest() throws Exception {
        javascriptService.handleRequest("utils-tests", "hex-decode.js", null, null, false);
        javascriptService.handleRequest("utils-tests", "hex-encode.js", null, null, false);
    }

    @Test
    public void executeQRCodeTest() throws Exception {
        javascriptService.handleRequest("utils-tests", "qrcode-generate.js", null, null, false);
    }

    @Test
    public void executeURLTest() throws Exception {
        javascriptService.handleRequest("utils-tests", "url-decode.js", null, null, false);
        javascriptService.handleRequest("utils-tests", "url-encode.js", null, null, false);
        javascriptService.handleRequest("utils-tests", "url-escape-form.js", null, null, false);
        javascriptService.handleRequest("utils-tests", "url-escape-path.js", null, null, false);
    }
}
