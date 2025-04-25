/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tracing;

import java.util.Map;
import java.util.TreeMap;

/**
 * The Class TaskStateUtil.
 */
public class TaskStateUtil {

    /**
     * Gets the variables.
     *
     * @param map the map
     * @return the variables
     */
    public static final Map<String, String> getVariables(Map<String, Object> map) {
        Map<String, String> result = new TreeMap<String, String>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof String) {
                result.put(entry.getKey(), (String) entry.getValue());
            } else {
                result.put(entry.getKey(), entry.getValue() != null ? entry.getValue()
                                                                           .toString()
                        : "null");
            }
        }
        return result;
    }

}
