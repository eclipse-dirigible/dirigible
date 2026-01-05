/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.framework.util;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ResourceUtil {

    public static String loadResource(String resourcePath) {
        try (InputStream inputStream = ResourceUtil.class.getResourceAsStream(resourcePath)) {
            if (null == inputStream) {
                throw new IllegalStateException("Missing resource with path " + resourcePath);
            }
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load resource with path " + resourcePath, ex);
        }
    }

}
