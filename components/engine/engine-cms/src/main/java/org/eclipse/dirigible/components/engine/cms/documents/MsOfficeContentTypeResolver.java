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

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.springframework.stereotype.Component;

/**
 * Serves and stores Office documents with the legacy Microsoft mime types, for clients that only
 * open them under those. Off by default; enable with
 * {@code DIRIGIBLE_DOCUMENTS_EXT_CONTENT_TYPE_MS_ENABLED=true}.
 * <p>
 * The flag is read per call rather than cached, so the setting can be flipped per tenant at
 * runtime.
 */
@Component
class MsOfficeContentTypeResolver implements DocumentContentTypeResolver {

    private static final String POWERPOINT = "application/vnd.ms-powerpoint";
    private static final String WORD = "application/msword";
    private static final String EXCEL = "application/vnd.ms-excel";

    @Override
    public String beforeUpload(String fileName, String contentType) {
        return resolve(fileName, contentType);
    }

    @Override
    public String beforeDownload(String fileName, String contentType) {
        return resolve(fileName, contentType);
    }

    private String resolve(String fileName, String contentType) {
        if (fileName == null || !DirigibleConfig.DOCUMENTS_CONTENT_TYPE_MS_ENABLED.getBooleanValue()) {
            return contentType;
        }
        String name = fileName.toLowerCase();
        if (name.endsWith(".pptx")) {
            return POWERPOINT;
        }
        if (name.endsWith(".docx")) {
            return WORD;
        }
        if (name.endsWith(".xlsx")) {
            return EXCEL;
        }
        return contentType;
    }
}
