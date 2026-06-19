/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.parser;

import java.util.Collections;
import java.util.List;

/**
 * Thrown by {@link IntentParser} when the parsed model contains structural problems (duplicate
 * names, dangling references, unknown enum-like values). Carries the full list of issues so the
 * author can fix all of them in one edit.
 */
public class IntentValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final List<String> issues;

    public IntentValidationException(List<String> issues) {
        super(formatMessage(issues));
        this.issues = Collections.unmodifiableList(issues);
    }

    public List<String> getIssues() {
        return issues;
    }

    private static String formatMessage(List<String> issues) {
        StringBuilder sb = new StringBuilder("Intent validation failed (").append(issues.size())
                                                                          .append(" issue")
                                                                          .append(issues.size() == 1 ? "" : "s")
                                                                          .append("):");
        for (String issue : issues) {
            sb.append("\n  - ")
              .append(issue);
        }
        return sb.toString();
    }
}
