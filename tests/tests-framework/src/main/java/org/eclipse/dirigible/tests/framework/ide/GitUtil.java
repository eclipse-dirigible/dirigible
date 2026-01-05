/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.framework.ide;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitUtil {

    private static final Pattern REPO_PATTERN = Pattern.compile("([^/]+?)(?:\\.git)?$");

    public static String extractRepoName(String url) {
        Matcher matcher = REPO_PATTERN.matcher(url);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new IllegalArgumentException("Failed to extract repository name from url " + url);
        }
    }
}
