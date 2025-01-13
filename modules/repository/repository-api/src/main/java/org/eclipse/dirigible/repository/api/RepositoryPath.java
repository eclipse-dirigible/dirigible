/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.repository.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Utility class representing the path object in the Repository.
 */
public class RepositoryPath {

    /** The path. */
    private final String path;

    /** The segments. */
    private final String[] segments;

    /**
     * Instantiates a new repository path.
     *
     * @param path the path
     */
    public RepositoryPath(String path) {
        this.path = path.replace('\\', '/');
        final StringTokenizer tokenizer = new StringTokenizer(this.path, IRepository.SEPARATOR);
        segments = new String[tokenizer.countTokens()];
        for (int i = 0; i < segments.length; ++i) {
            segments[i] = tokenizer.nextToken();
        }
    }

    /**
     * Instantiates a new repository path.
     *
     * @param repositoryPath the repository path
     */
    public RepositoryPath(RepositoryPath repositoryPath) {
        this(repositoryPath.segments);
    }

    /**
     * Instantiates a new repository path.
     *
     * @param input the input
     */
    public RepositoryPath(String... input) {
        List<String> allSegments = new ArrayList<>();
        for (String segment : input) {
            final StringTokenizer tokenizer = new StringTokenizer(segment, IRepository.SEPARATOR);
            int tokens = tokenizer.countTokens();
            for (int idx = 0; idx < tokens; idx++) {
                allSegments.add(tokenizer.nextToken());
            }
        }
        this.segments = allSegments.toArray(new String[] {});
        this.path = toString();
    }

    /**
     * Getter for the last segment.
     *
     * @return the last segment
     */
    public String getLastSegment() {
        if (segments.length == 0) {
            return ""; //$NON-NLS-1$
        }
        return segments[segments.length - 1];
    }

    /**
     * Getter for the path of the parent.
     *
     * @return the parent path
     */
    public RepositoryPath getParentPath() {
        if (segments.length == 0) {
            return null;
        }
        final String[] newSegments = Arrays.copyOf(segments, segments.length - 1);
        return new RepositoryPath(newSegments);
    }

    /**
     * Add new segment after the last position.
     *
     * @param name the name
     * @return the repository path
     */
    public RepositoryPath append(String name) {
        final StringTokenizer tokenizer = new StringTokenizer(name, IRepository.SEPARATOR);
        final String[] newSegments = Arrays.copyOf(segments, segments.length + tokenizer.countTokens());
        int i = 0;
        while (tokenizer.hasMoreTokens()) {
            newSegments[segments.length + i++] = tokenizer.nextToken();
        }
        return new RepositoryPath(newSegments);
    }

    /**
     * To string.
     *
     * @return the string
     */
    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        if (segments.length == 0) {
            return IRepository.SEPARATOR;
        }
        final StringBuilder builder = new StringBuilder();
        for (String segment : segments) {
            if (!segment.equals(IRepository.SEPARATOR)) {
                if (segment.indexOf(':') == -1) { // hack for windows paths
                    builder.append(IRepository.SEPARATOR);
                }
            }
            builder.append(segment);
        }
        return builder.toString();
    }

    /**
     * Builds the.
     *
     * @return the string
     */
    public String build() {
        return toString();
    }

    /**
     * Gets the path.
     *
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * Equals.
     *
     * @param obj the obj
     * @return true, if successful
     */
    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof final RepositoryPath other)) {
            return false;
        }
        return getPath().equals(other.getPath());
    }

    /**
     * Hash code.
     *
     * @return the int
     */
    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return getPath().hashCode();
    }

    /**
     * Gets the segments.
     *
     * @return the segments
     */
    public String[] getSegments() {
        return Arrays.copyOf(segments, segments.length);
    }

    /**
     * Construct path to.
     *
     * @param number the number
     * @return the string
     */
    public String constructPathTo(int number) {
        if (number >= segments.length) {
            return toString();
        }
        if (segments.length == 0) {
            return IRepository.SEPARATOR;
        }
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < number; i++) {
            builder.append(IRepository.SEPARATOR);
            builder.append(segments[i]);
        }
        return builder.toString();
    }

    /**
     * Construct path from.
     *
     * @param number the number
     * @return the string
     */
    public String constructPathFrom(int number) {
        if (number == segments.length) {
            return IRepository.SEPARATOR;
        }
        if (number > segments.length) {
            return toString();
        }
        if (segments.length == 0) {
            return IRepository.SEPARATOR;
        }
        final StringBuilder builder = new StringBuilder();
        for (int i = number; i < segments.length; i++) {
            builder.append(IRepository.SEPARATOR);
            builder.append(segments[i]);
        }
        return builder.toString();
    }

    /**
     * Normalize path.
     *
     * @param path the path
     * @param name the name
     * @return the string
     */
    public static String normalizePath(String path, String name) {
        String normalizedPath = null;
        if (path != null) {
            if (path.endsWith(IRepository.SEPARATOR)) {
                normalizedPath = path + name;
            } else {
                normalizedPath = path + IRepository.SEPARATOR + name;
            }
        }
        return normalizedPath;
    }

    /**
     * Normalize name.
     *
     * @param name the name
     * @return the string
     */
    public static String normalizeName(String name) {
        return name.replaceAll("[^A-Za-z0-9_]", "_");
    }

    /**
     * Set the segment of the given index position to the given value, starting with zero.
     *
     * @param index the index
     * @param value the value
     */
    public void setSegment(int index, String value) {
        this.segments[index] = value;
    }
}
