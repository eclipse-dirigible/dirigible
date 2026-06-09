/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.http;

import java.util.List;
import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.eclipse.dirigible.components.api.http.HttpUploadFacade;

/**
 * Parses {@code multipart/form-data} bodies — the standard mechanism for file uploads from HTML
 * forms and from {@code curl -F}. {@link #isMultipartContent()} short-circuits before the parse
 * (cheap header check) so a controller that handles both URL-encoded forms and uploads can branch
 * correctly.
 * <p>
 * Each {@link FileItem} reports its field name, content type, byte size, and streams its content
 * via {@link FileItem#getInputStream()}. The parser writes oversized parts to disk through the
 * platform's configured temp dir, so very large uploads will not blow up the heap.
 */
public final class Upload {

    private Upload() {}

    public static boolean isMultipartContent() {
        return HttpUploadFacade.isMultipartContent();
    }

    public static List<FileItem> parseRequest() throws FileUploadException {
        return HttpUploadFacade.parseRequest();
    }
}
