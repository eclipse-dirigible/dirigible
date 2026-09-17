/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * Tests for resolving a print template's image source against the content store.
 */
class PrintImageResolverTest {

    private final CmsStore cmsStore = mock(CmsStore.class);
    private final PrintImageResolver resolver = new PrintImageResolver(cmsStore);

    @Test
    void aContentStorePathIsInlinedAsADataUri() throws IOException {
        when(cmsStore.readDocument(anyString(), anyLong())).thenReturn(
                Optional.of(new CmsStore.Content("PNG".getBytes(StandardCharsets.UTF_8), "image/png")));

        assertEquals("data:image/png;base64,UE5H", resolver.resolve("/Templates/Print/logo.png"));
    }

    @Test
    void aDataUriIsPassedThroughUntouchedAndNeverRead() throws IOException {
        String inline = "data:image/png;base64,UE5H";

        assertEquals(inline, resolver.resolve(inline));
        verify(cmsStore, never()).readDocument(anyString(), anyLong());
    }

    @Test
    void anAddressableUriIsLeftToTheRenderer() throws IOException {
        assertEquals("https://example.com/logo.png", resolver.resolve("https://example.com/logo.png"));
        verify(cmsStore, never()).readDocument(anyString(), anyLong());
    }

    /** A missing file is the everyday case - a tenant that has not uploaded its logo yet. */
    @Test
    void aMissingFileResolvesToNothing() throws IOException {
        when(cmsStore.readDocument(anyString(), anyLong())).thenReturn(Optional.empty());

        assertNull(resolver.resolve("/Templates/Print/logo.png"));
    }

    /**
     * A document that is not an image is refused rather than embedded: FOP would fail on the bytes, and
     * the source may come from data (an attachment path), not only from the template.
     */
    @Test
    void aDocumentThatIsNotAnImageIsRefused() throws IOException {
        when(cmsStore.readDocument(anyString(), anyLong())).thenReturn(
                Optional.of(new CmsStore.Content("%PDF".getBytes(StandardCharsets.UTF_8), "application/pdf")));

        assertNull(resolver.resolve("/Attachments/Company/2026/09/x/contract.pdf"));
    }

    /**
     * An attachment's content type is whatever the uploading browser's multipart header claimed, and it
     * is interpolated into the data URI - so a type that is not a plain image type is refused outright
     * rather than escaped on the way out.
     */
    @Test
    void aMediaTypeThatIsNotAPlainImageTypeIsRefused() throws IOException {
        when(cmsStore.readDocument(anyString(), anyLong())).thenReturn(
                Optional.of(new CmsStore.Content("PNG".getBytes(StandardCharsets.UTF_8), "image/png\" onload=\"x")));

        assertNull(resolver.resolve("/Attachments/Company/2026/09/x/logo.png"));
    }

    @Test
    void anUnreadableStoreResolvesToNothing() throws IOException {
        when(cmsStore.readDocument(anyString(), anyLong())).thenThrow(new IOException("store down"));

        assertNull(resolver.resolve("/Templates/Print/logo.png"));
    }

    /** The store's root is the tenant's boundary, so a traversing path is never read. */
    @Test
    void aTraversingPathIsRefusedWithoutReading() throws IOException {
        assertNull(resolver.resolve("/Templates/../../../etc/passwd"));
        verify(cmsStore, never()).readDocument(anyString(), anyLong());
    }
}
