/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.platform;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.commons.api.helpers.FileSystemUtils;
import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.repository.api.RepositoryPath;
import org.eclipse.dirigible.repository.local.LocalCollection;
import org.springframework.stereotype.Component;

/**
 * The Class RegistryFacade.
 */
@Component
public class RegistryFacade {

    /** The Constant META_INF_DIRIGIBLE. */
    private static final String META_INF_DIRIGIBLE = "/META-INF/dirigible";

    /** The Constant META_INF_WEBJARS. */
    private static final String META_INF_WEBJARS = "/META-INF/resources/webjars";

    /**
     * Gets the content.
     *
     * @param path the path
     * @return the content
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static byte[] getContent(String path) throws IOException {
        // Check in the repository first
        IResource resource = RepositoryFacade.getResource(toRepositoryPath(path));
        if (resource.exists()) {
            return resource.getContent();
        }
        // Check in the pre-delivered content from dirigible folder
        byte[] content = getFromClasspath(META_INF_DIRIGIBLE + toResourcePath(path));
        if (content != null) {
            return content;
        }
        // Check in the pre-delivered content from webjars
        content = getFromClasspath(META_INF_WEBJARS + toResourcePath(path));
        if (content != null) {
            return content;
        }

        return null;
    }

    /**
     * Gets the from classpath.
     *
     * @param path the path
     * @return the from classpath
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static byte[] getFromClasspath(String path) throws IOException {
        InputStream in = RegistryFacade.class.getResourceAsStream(path);
        try {
            if (in != null) {
                return IOUtils.toByteArray(in);
            } else {
                return null;
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /**
     * Gets the text.
     *
     * @param path the path
     * @return the text
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static String getText(String path) throws IOException {
        byte[] content = getContent(path);
        if (content == null) {
            return null;
        }
        return new String(content);
    }

    /**
     * Exists.
     *
     * @param path the path
     * @return the boolean
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static Boolean exists(String path) throws IOException {
        return getContent(path) == null;
    }

    /**
     * To repository path.
     *
     * @param path the path
     * @return the string
     */
    private static String toRepositoryPath(String path) {
        return new RepositoryPath().append(IRepositoryStructure.PATH_REGISTRY_PUBLIC)
                                   .append(path)
                                   .build();
    }

    /**
     * To resource path.
     *
     * @param path the path
     * @return the string
     */
    private static String toResourcePath(String path) {
        if (!path.startsWith(IRepositoryStructure.SEPARATOR)) {
            return IRepositoryStructure.SEPARATOR + path;
        }
        return path;
    }

    /**
     * Find all the files matching the pattern.
     *
     * @param path the root path
     * @param pattern the glob pattern
     * @return the list of file names
     * @throws IOException in case of an error
     */
    public static String find(String path, String pattern) throws IOException {
        ICollection collection = RepositoryFacade.getCollection(toRepositoryPath(path));
        if (collection.exists() && collection instanceof LocalCollection) {
            List<String> list = FileSystemUtils.find(((LocalCollection) collection).getFolder()
                                                                                   .getPath(),
                    pattern);
            int repositoryRootLength = ((LocalCollection) collection.getRepository()
                                                                    .getRoot()).getFolder()
                                                                               .getPath()
                                                                               .length()
                    + IRepositoryStructure.PATH_REGISTRY_PUBLIC.length();
            List<String> prepared = new ArrayList<String>();
            list.forEach(item -> {
                String truncated = item.substring(repositoryRootLength);
                if (!IRepository.SEPARATOR.equals(File.separator)) {
                    truncated = truncated.replace(File.separator, IRepository.SEPARATOR);
                }
                prepared.add(truncated);
            });
            return GsonHelper.toJson(prepared);
        }
        return "[]";
    }
}
