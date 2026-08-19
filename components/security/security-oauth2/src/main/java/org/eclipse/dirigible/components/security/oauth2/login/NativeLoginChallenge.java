/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth2.login;

import java.util.Map;

/**
 * A challenge the identity provider requires before it issues tokens (e.g. an MFA code or a forced
 * new password). The caller answers it on a follow-up call carrying the challenge name and the
 * opaque session state back.
 *
 * @param name the provider's challenge name (e.g. {@code SOFTWARE_TOKEN_MFA},
 *        {@code NEW_PASSWORD_REQUIRED})
 * @param session the opaque provider session state to carry into the answer
 * @param parameters the provider's challenge parameters (e.g. the code delivery destination)
 */
public record NativeLoginChallenge(String name, String session, Map<String, String> parameters) implements NativeLoginResult {
}
