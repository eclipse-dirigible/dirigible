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

import java.util.List;

import org.eclipse.dirigible.commons.api.helpers.ContentTypeHelper;
import org.springframework.stereotype.Component;

/**
 * Resolves the content type a document is stored with and served under, consulting every
 * {@link DocumentContentTypeResolver} in bean order.
 */
@Component
public class ContentTypeResolver {

    private final List<DocumentContentTypeResolver> resolvers;

    ContentTypeResolver(List<DocumentContentTypeResolver> resolvers) {
        this.resolvers = List.copyOf(resolvers);
    }

    /**
     * The content type to store a document with.
     *
     * @param fileName the document's name
     * @param contentType the content type the client announced (may be null)
     * @return the resolved content type
     */
    public String beforeUpload(String fileName, String contentType) {
        String resolved = defaultContentType(fileName, contentType);
        for (DocumentContentTypeResolver resolver : resolvers) {
            resolved = resolver.beforeUpload(fileName, resolved);
        }
        return resolved;
    }

    /**
     * The content type to serve a document under.
     *
     * @param fileName the document's name
     * @param contentType the content type the document is stored with (may be null)
     * @return the resolved content type
     */
    public String beforeDownload(String fileName, String contentType) {
        String resolved = defaultContentType(fileName, contentType);
        for (DocumentContentTypeResolver resolver : resolvers) {
            resolved = resolver.beforeDownload(fileName, resolved);
        }
        return resolved;
    }

    /**
     * The given content type, or the platform's type for that file extension when the client sent none.
     * {@code ContentTypeHelper} already substitutes its own default for an unknown extension, so this
     * never returns null.
     */
    private String defaultContentType(String fileName, String contentType) {
        if (contentType != null && !contentType.isBlank()) {
            return contentType;
        }
        return ContentTypeHelper.getContentType(ContentTypeHelper.getExtension(fileName));
    }
}
