/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth2.resourceserver;

/**
 * The identity providers whose bearer tokens the OAuth2 login profiles accept. They mark the kind
 * of a token differently, which is the one thing the shared bearer support has to know about them.
 */
public enum IdentityProvider {

    /** AWS Cognito marks the kind in the {@code token_use} claim ({@code id} or {@code access}). */
    COGNITO,

    /**
     * Keycloak marks the kind in the {@code typ} claim ({@code ID}, {@code Bearer}, {@code Refresh}).
     */
    KEYCLOAK
}
