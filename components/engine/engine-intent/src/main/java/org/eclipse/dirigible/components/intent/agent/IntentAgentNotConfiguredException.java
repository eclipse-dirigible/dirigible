/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.agent;

/**
 * Raised when the Intent AI assistant is invoked but no API key is configured. The editor surfaces
 * this as a hint to set {@code DIRIGIBLE_INTENT_AI_API_KEY} rather than as an error.
 */
class IntentAgentNotConfiguredException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    IntentAgentNotConfiguredException(String message) {
        super(message);
    }
}
