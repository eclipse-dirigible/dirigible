/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.repository.api;

/**
 * The Interface IRepositoryStructure.
 */
public interface IRepositoryStructure {

    /** The Constant SEPARATOR. */
    public static final String SEPARATOR = "/"; //$NON-NLS-1$

    /** The Constant KEYWORD_REGISTRY. */
    public static final String KEYWORD_REGISTRY = "registry"; //$NON-NLS-1$

    /** The Constant KEYWORD_PUBLIC. */
    public static final String KEYWORD_PUBLIC = "public"; //$NON-NLS-1$

    /** The Constant KEYWORD_USERS. */
    public static final String KEYWORD_USERS = "users"; //$NON-NLS-1$

    /** The Constant KEYWORD_WORKSPACE. */
    public static final String KEYWORD_WORKSPACE = "workspace"; //$NON-NLS-1$

    /** The Constant PATH_ROOT. */
    public static final String PATH_ROOT = SEPARATOR;

    /** The Constant PATH_REGISTRY. */
    public static final String PATH_REGISTRY = PATH_ROOT + KEYWORD_REGISTRY; // /registry

    /** The Constant PATH_REGISTRY_PUBLIC. */
    public static final String PATH_REGISTRY_PUBLIC = PATH_REGISTRY + SEPARATOR + KEYWORD_PUBLIC; // /registry/public

    /** The Constant PATH_USERS. */
    public static final String PATH_USERS = PATH_ROOT + KEYWORD_USERS; // /users

    /** The Constant PATTERN_USERS_WORKSPACE_DEFAULT. */
    public static final String PATTERN_USERS_WORKSPACE_DEFAULT = PATH_USERS + SEPARATOR + "{0}" + SEPARATOR + KEYWORD_WORKSPACE; //$NON-NLS-1$ /users/john/workspace

    /** The Constant PATTERN_USERS_WORKSPACE_NAMED. */
    public static final String PATTERN_USERS_WORKSPACE_NAMED = PATH_USERS + SEPARATOR + "{0}" + SEPARATOR + "{1}"; //$NON-NLS-1$ /users/john/product3
}
