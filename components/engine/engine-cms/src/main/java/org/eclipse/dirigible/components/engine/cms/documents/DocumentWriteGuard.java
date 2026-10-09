/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms.documents;

import java.util.Optional;

/**
 * Protects documents that another part of the platform owns from being changed through the
 * Documents perspective: replaced by an upload, renamed or deleted. The CMS access grants decide
 * who may write a path; a guard decides that a particular document must not be changed by hand at
 * all - the canonical case being a shipped print template version, which is immutable and converged
 * back to its shipped bytes. A folder is renamed or deleted only when no document under it, at any
 * depth, is refused. Contribute an implementation as a Spring {@code @Component}; every one is
 * consulted, and the first refusal answers the request with 409 and its reason.
 */
public interface DocumentWriteGuard {

    /**
     * Why the document at a path must not be changed through the Documents perspective.
     *
     * @param path the absolute CMS path of the document
     * @return the reason, shown to the user, or empty when this guard does not protect the path
     */
    Optional<String> refusal(String path);
}
