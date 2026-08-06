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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The content-type resolution chain that replaced the {@code ui-documents-content-type} JavaScript
 * extension point.
 */
class ContentTypeResolverTest {

    @Test
    void aContributedResolverOverridesTheAnnouncedType() {
        ContentTypeResolver resolver = new ContentTypeResolver(List.of(fixed("application/msword")));

        assertEquals("application/msword", resolver.beforeUpload("report.docx", "application/octet-stream"));
        assertEquals("application/msword", resolver.beforeDownload("report.docx", "application/octet-stream"));
    }

    @Test
    void withoutResolversTheAnnouncedTypeIsKept() {
        ContentTypeResolver resolver = new ContentTypeResolver(List.of());

        assertEquals("text/plain", resolver.beforeUpload("notes.txt", "text/plain"));
    }

    @Test
    void aMissingTypeIsDerivedFromTheExtension() {
        ContentTypeResolver resolver = new ContentTypeResolver(List.of());

        assertEquals("text/plain", resolver.beforeDownload("notes.txt", null));
    }

    @Test
    void anUnknownExtensionWithoutATypeTakesThePlatformDefault() {
        // ContentTypeHelper substitutes its own default for an unknown extension rather than
        // answering null, so the resolver has no separate fallback of its own.
        ContentTypeResolver resolver = new ContentTypeResolver(List.of());

        assertEquals("text/plain", resolver.beforeDownload("archive.unknownext", ""));
    }

    @Test
    void resolversAreChainedInOrder() {
        ContentTypeResolver resolver = new ContentTypeResolver(List.of(fixed("first/type"), fixed("second/type")));

        assertEquals("second/type", resolver.beforeUpload("any.bin", "text/plain"));
    }

    private static DocumentContentTypeResolver fixed(String contentType) {
        return new DocumentContentTypeResolver() {

            @Override
            public String beforeUpload(String fileName, String announced) {
                return contentType;
            }

            @Override
            public String beforeDownload(String fileName, String announced) {
                return contentType;
            }
        };
    }
}
