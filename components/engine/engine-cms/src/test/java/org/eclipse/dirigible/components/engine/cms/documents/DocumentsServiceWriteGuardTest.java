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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.eclipse.dirigible.components.engine.cms.CmisDocument;
import org.eclipse.dirigible.components.engine.cms.ObjectType;
import org.eclipse.dirigible.components.engine.cms.service.CmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A {@link DocumentWriteGuard} keeps the Documents perspective from renaming or deleting a document
 * it protects, whatever the caller's CMS access grants allow.
 */
class DocumentsServiceWriteGuardTest {

    private static final String PROTECTED = "/Templates/SalesInvoice/Print/en/standard@1.28.0.print";

    private final CmsService cmsService = mock(CmsService.class);
    private final CmisDocument document = mock(CmisDocument.class);
    private DocumentsService documentsService;

    @BeforeEach
    void setUp() throws Exception {
        DocumentAccessEvaluator accessEvaluator = mock(DocumentAccessEvaluator.class);
        when(accessEvaluator.isWritable(anyString(), any())).thenReturn(true);
        DocumentWriteGuard guard = path -> path.endsWith("@1.28.0.print") ? Optional.of("it is shipped") : Optional.empty();
        documentsService = new DocumentsService(cmsService, accessEvaluator, mock(ContentTypeResolver.class), List.of(guard));
        when(document.getType()).thenReturn(ObjectType.DOCUMENT);
        when(cmsService.getObjectByPath(anyString())).thenReturn(document);
    }

    @Test
    void aProtectedDocumentIsNeitherRenamedNorDeleted() throws Exception {
        assertThrows(DocumentConflictException.class, () -> documentsService.rename(PROTECTED, "mine.print", null));
        assertThrows(DocumentConflictException.class, () -> documentsService.delete(List.of(PROTECTED), false, null));

        verify(document, never()).rename(anyString());
        verify(document, never()).delete();
    }

    @Test
    void noDocumentIsRenamedIntoAProtectedName() throws Exception {
        assertThrows(DocumentConflictException.class,
                () -> documentsService.rename("/Templates/SalesInvoice/Print/en/mine.print", "standard@1.28.0.print", null));

        verify(document, never()).rename(anyString());
    }

    @Test
    void anUnprotectedDocumentIsDeleted() throws Exception {
        documentsService.delete(List.of("/Templates/SalesInvoice/Print/en/mine.print"), false, null);

        verify(document).delete();
    }
}
