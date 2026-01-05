/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.processes.schema.imp.tasks;

import org.eclipse.dirigible.components.engine.bpm.flowable.delegate.BPMTask;
import org.eclipse.dirigible.components.engine.bpm.flowable.delegate.TaskExecution;
import org.eclipse.dirigible.components.engine.cms.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

abstract class BaseImportTask extends BPMTask {

    @Override
    protected final void execute(TaskExecution execution) {
        ImportProcessContext context = new ImportProcessContext(execution);
        execute(context);
    }

    protected abstract void execute(ImportProcessContext context);

    protected String loadDocumentContent(String path) {
        try {
            try (InputStream inputStream = loadDocumentContentAsStream(path)) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException ex) {
            throw new SchemaImportException("Failed to load file from path " + path, ex);
        }
    }

    protected InputStream loadDocumentContentAsStream(String path) {
        try {
            CmisDocument cmisDocument = getDocument(path);
            CmisContentStream contentStream = cmisDocument.getContentStream();
            return contentStream.getInputStream();

        } catch (IOException ex) {
            throw new SchemaImportException("Failed to load file from path " + path, ex);
        }
    }

    private CmisDocument getDocument(String documentPath) {
        CmisSession cmisSession = CmisSessionFactory.getSession();

        try {
            CmisObject documentAsObject = cmisSession.getObjectByPath(documentPath);
            if (documentAsObject instanceof CmisDocument document) {
                return document;
            }
            throw new SchemaImportException("Returned cmis object " + documentAsObject + " is not a document");
        } catch (IOException ex) {
            throw new SchemaImportException("Failed to get document for path " + documentPath, ex);
        }
    }
}
