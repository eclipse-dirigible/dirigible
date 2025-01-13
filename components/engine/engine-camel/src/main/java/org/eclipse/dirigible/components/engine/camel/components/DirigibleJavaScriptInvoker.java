/*
 * Copyright (c) 2010-2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.camel.components;

import org.apache.camel.Message;

public interface DirigibleJavaScriptInvoker {

    /**
     * Invoke dirigible JavaScript file
     *
     * @param camelMessage camel message
     * @param javaScriptPath a path to the JavaScript which should be executed
     */
    void invoke(Message camelMessage, String javaScriptPath);
}
