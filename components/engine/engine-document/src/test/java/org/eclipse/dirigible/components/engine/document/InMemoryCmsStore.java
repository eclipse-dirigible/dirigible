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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A {@link CmsStore} over a map of paths to bytes - the plain file operations the print template
 * catalogue is built on, without a CMS. Listings come back in name order, and there is no rename:
 * the catalogue must manage with copy and delete, as it does on the S3 and SharePoint CMS.
 */
class InMemoryCmsStore extends CmsStore {

    private final Map<String, byte[]> documents = new TreeMap<>();

    @Override
    List<String> listFolders(String folderPath) {
        Set<String> folders = new LinkedHashSet<>();
        String prefix = folderPath + "/";
        for (String path : documents.keySet()) {
            if (path.startsWith(prefix)) {
                String rest = path.substring(prefix.length());
                int separator = rest.indexOf('/');
                if (separator > 0) {
                    folders.add(rest.substring(0, separator));
                }
            }
        }
        return new ArrayList<>(folders);
    }

    @Override
    List<String> listDocuments(String folderPath) {
        List<String> names = new ArrayList<>();
        String prefix = folderPath + "/";
        for (String path : documents.keySet()) {
            if (path.startsWith(prefix) && path.indexOf('/', prefix.length()) < 0) {
                names.add(path.substring(prefix.length()));
            }
        }
        return names;
    }

    @Override
    Optional<byte[]> read(String cmsPath) {
        return Optional.ofNullable(documents.get(cmsPath));
    }

    @Override
    void write(String cmsPath, byte[] content) {
        documents.put(cmsPath, content.clone());
    }

    @Override
    void move(String cmsPath, String newName, byte[] content) throws IOException {
        String target = cmsPath.substring(0, cmsPath.lastIndexOf('/') + 1) + newName;
        if (!documents.containsKey(cmsPath) || documents.containsKey(target)) {
            throw new IOException("Cannot move [" + cmsPath + "] to [" + target + "]");
        }
        documents.put(target, content.clone());
        documents.remove(cmsPath);
    }

    @Override
    boolean delete(String cmsPath) {
        return documents.remove(cmsPath) != null;
    }

    /** The document names of a folder. */
    Set<String> names(String folderPath) {
        return new TreeSet<>(listDocuments(folderPath));
    }
}
