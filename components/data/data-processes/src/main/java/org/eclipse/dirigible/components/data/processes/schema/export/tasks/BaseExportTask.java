/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.processes.schema.export.tasks;

import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.engine.bpm.flowable.delegate.BPMTask;
import org.eclipse.dirigible.components.engine.bpm.flowable.delegate.TaskExecution;
import org.eclipse.dirigible.components.engine.cms.*;
import org.springframework.http.MediaType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

abstract class BaseExportTask extends BPMTask {

    @Override
    protected final void execute(TaskExecution execution) {
        ExportProcessContext context = new ExportProcessContext(execution);
        execute(context);
    }

    protected abstract void execute(ExportProcessContext context);

    protected void saveObjectAsJsonDocument(Object object, String fileName, String folderPath) {
        String exportTopologyJson = JsonHelper.toJson(object);
        saveDocument(exportTopologyJson, fileName, MediaType.APPLICATION_JSON_VALUE, folderPath);
    }

    private void saveDocument(String content, String fileName, String mediaType, String folderPath) {
        CmisFolder folder = getFolder(folderPath);
        saveDocument(content, fileName, mediaType, folder);
    }

    private void saveDocument(String content, String fileName, String mediaType, CmisFolder folder) {
        CmisSession cmisSession = CmisSessionFactory.getSession();

        Map<String, String> fileProps = Map.of(CmisConstants.OBJECT_TYPE_ID, CmisConstants.OBJECT_TYPE_DOCUMENT, //
                CmisConstants.NAME, fileName);

        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        try (InputStream inStream = new ByteArrayInputStream(bytes)) {

            long length = bytes.length; // not needed?
            CmisContentStream contentStream = cmisSession.getObjectFactory()
                                                         .createContentStream(fileName, length, mediaType, inStream);

            folder.createDocument(fileProps, contentStream);
        } catch (IOException ex) {
            throw new SchemaExportException("Failed to create document with name [" + fileName + "] in folder [" + folder + "]", ex);
        }
    }

    protected CmisFolder getFolder(String folderPath) {
        CmisSession cmisSession = CmisSessionFactory.getSession();

        try {
            CmisObject folderAsObject = cmisSession.getObjectByPath(folderPath);
            if (folderAsObject instanceof CmisFolder folder) {
                return folder;
            }
            throw new SchemaExportException("Returned cmis object " + folderAsObject + " is not a folder");
        } catch (IOException ex) {
            throw new SchemaExportException("Failed to get folder for path " + folderPath, ex);
        }
    }

    protected void saveDocument(InputStream contentInputStream, long contentLength, String fileName, String mediaType, String folderPath) {
        CmisSession cmisSession = CmisSessionFactory.getSession();

        Map<String, String> fileProps = Map.of(CmisConstants.OBJECT_TYPE_ID, CmisConstants.OBJECT_TYPE_DOCUMENT, //
                CmisConstants.NAME, fileName);

        CmisFolder folder = getFolder(folderPath);

        try {
            CmisContentStream contentStream = cmisSession.getObjectFactory()
                                                         .createContentStream(fileName, contentLength, mediaType, contentInputStream);

            folder.createDocument(fileProps, contentStream);
        } catch (IOException ex) {
            throw new SchemaExportException("Failed to create document with name [" + fileName + "] in folder [" + folder + "]", ex);
        }
    }

    protected void saveObjectAsJsonDocument(Object object, String fileName, CmisFolder folder) {
        String exportTopologyJson = JsonHelper.toJson(object);
        saveDocument(exportTopologyJson, fileName, MediaType.APPLICATION_JSON_VALUE, folder);
    }
}
