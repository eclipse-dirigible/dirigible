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

/**
 * Adjusts the content type of a document as it enters or leaves the CMS.
 * <p>
 * The extension surface for content types that must not be served or stored as the platform's
 * default guess - the canonical case being Office documents that some clients only open when they
 * carry the legacy Microsoft mime types. Contribute an implementation as a Spring
 * {@code @Component}; every one is consulted in {@code @Order} until one changes the value.
 * <p>
 * Replaces the {@code ui-documents-content-type} JavaScript extension point, which could not be
 * consumed once the Documents backend became Java.
 */
public interface DocumentContentTypeResolver {

    /**
     * The content type a document should be STORED with.
     *
     * @param fileName the document's name
     * @param contentType the content type the client announced
     * @return the content type to store, or the given one when this resolver does not apply
     */
    String beforeUpload(String fileName, String contentType);

    /**
     * The content type a document should be SERVED with.
     *
     * @param fileName the document's name
     * @param contentType the content type the document is stored with
     * @return the content type to serve, or the given one when this resolver does not apply
     */
    String beforeDownload(String fileName, String contentType);
}
