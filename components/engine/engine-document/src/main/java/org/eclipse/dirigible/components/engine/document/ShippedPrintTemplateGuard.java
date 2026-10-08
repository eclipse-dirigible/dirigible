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

import java.util.Optional;

import org.eclipse.dirigible.components.engine.cms.documents.DocumentWriteGuard;
import org.springframework.stereotype.Component;

/**
 * Keeps the Documents perspective from changing a shipped print template version,
 * {@code Templates/<Entity>/Print/<lang>/<name>@<version>.print}: an upload over it, a rename and a
 * delete are refused, and so is creating a document of that shape, which would pose as a version
 * the platform shipped. A version is immutable - the synchronizer converges it back to its shipped
 * bytes whenever the template is reprocessed, so an edit made there would be lost; a tenant
 * customises a duplicate instead (Settings, Print Templates).
 */
@Component
class ShippedPrintTemplateGuard implements DocumentWriteGuard {

    @Override
    public Optional<String> refusal(String path) {
        if (path == null) {
            return Optional.empty();
        }
        String[] segments = path.replace('\\', '/')
                                .replaceFirst("^/+", "")
                                .split("/");
        if (segments.length != 5 || !"Templates".equals(segments[0]) || !"Print".equals(segments[2])) {
            return Optional.empty();
        }
        return PrintTemplateName.fromFileName(segments[4])
                                .filter(PrintTemplateName::isShipped)
                                .map(name -> "[" + name.reference() + "] is a shipped print template version, which is immutable"
                                        + " - duplicate it in Settings > Print Templates and customise the copy");
    }
}
