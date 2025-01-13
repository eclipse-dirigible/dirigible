/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms.s3.repository;

import org.eclipse.dirigible.repository.api.IRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The Class S3ObjectUtil.
 */
class S3ObjectUtil {

    /**
     * The Class S3ObjectDescriptor.
     */
    static class S3ObjectDescriptor {

        /** The name. */
        private final String name;

        /** The folder. */
        private final boolean folder;

        /** The file. */
        private final boolean file;

        /**
         * Instantiates a new s 3 object descriptor.
         *
         * @param folder the folder
         * @param name the name
         */
        S3ObjectDescriptor(boolean folder, String name) {
            this.folder = folder;
            this.file = !folder;
            this.name = name;
        }

        /**
         * Checks if is folder.
         *
         * @return true, if is folder
         */
        public boolean isFolder() {
            return folder;
        }

        /**
         * Checks if is file.
         *
         * @return true, if is file
         */
        public boolean isFile() {
            return file;
        }

        /**
         * Gets the name.
         *
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * To string.
         *
         * @return the string
         */
        @Override
        public String toString() {
            return "S3ObjectDescriptor{" + "name='" + name + '\'' + ", folder=" + folder + ", file=" + file + '}';
        }

        /**
         * Equals.
         *
         * @param o the o
         * @return true, if successful
         */
        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            S3ObjectDescriptor that = (S3ObjectDescriptor) o;
            return folder == that.folder && file == that.file && Objects.equals(name, that.name);
        }

        /**
         * Hash code.
         *
         * @return the int
         */
        @Override
        public int hashCode() {
            return Objects.hash(name, folder, file);
        }
    }

    /**
     * Gets the direct children.
     *
     * @param rootPath the root path
     * @param objectKeys the object keys
     * @return the direct children
     */
    static Set<S3ObjectDescriptor> getDirectChildren(String rootPath, List<String> objectKeys) {
        return getDirectChildren(rootPath, new HashSet<>(objectKeys));
    }

    /**
     * Gets the direct children.
     *
     * @param rootPath the root path
     * @param objectKeys the object keys
     * @return the direct children
     */
    static Set<S3ObjectDescriptor> getDirectChildren(String rootPath, Set<String> objectKeys) {
        Set<S3ObjectDescriptor> descriptors = new HashSet<>();

        for (String objectKey : objectKeys) {
            if (Objects.equals(rootPath, objectKey)) {
                continue;
            }
            String relativePath = objectKey.startsWith(rootPath) ? objectKey.replaceFirst(Pattern.quote(rootPath), "") : objectKey;

            if (isObjectInSubdir(relativePath)) {
                String childFolderName = extractFirstSegment(relativePath);
                childFolderName = "".equals(childFolderName) ? IRepository.SEPARATOR : childFolderName;
                S3ObjectDescriptor descriptor = new S3ObjectDescriptor(true, childFolderName);
                descriptors.add(descriptor);
            } else {
                String fileName = extractFirstSegment(relativePath);
                S3ObjectDescriptor descriptor = new S3ObjectDescriptor(isFolder(relativePath), fileName);
                descriptors.add(descriptor);
            }
        }

        return descriptors;
    }

    /**
     * Checks if is object in subdir.
     *
     * @param path the path
     * @return true, if is object in subdir
     */
    private static boolean isObjectInSubdir(String path) {
        return path.split(IRepository.SEPARATOR).length > 1;
    }

    /**
     * Extract first segment.
     *
     * @param relativePath the relative path
     * @return the string
     */
    private static String extractFirstSegment(String relativePath) {
        String[] parts = relativePath.split("(?<=/)");
        return parts[0];
    }

    /**
     * Checks if is folder.
     *
     * @param objectKey the object key
     * @return true, if is folder
     */
    private static boolean isFolder(String objectKey) {
        return objectKey.endsWith(IRepository.SEPARATOR);
    }

}
