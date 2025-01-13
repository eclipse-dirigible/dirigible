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

/**
 * Utility class for handling Resource objects.
 */
public final class ResourceUtil {

    /**
     * Returns the extension of the resource name if there is such, otherwise returns <code>null</code>.
     * If there is a dot but there is no extension, then this method returns the empty string.
     * <p>
     * For example:
     * <ul>
     * <li><b>request.xml</b> yields <b>"xml"</b></li>
     * <li><b>page.html</b> yields <b>"html"</b></li>
     * <li><b>sample.</b> yields <b><code>""</code></b></li>
     * <li><b>sample</b> yields <b><code>null</code></b></li>
     * </ul>
     *
     * @param resource resource who's name extension will be returned.
     * @return the extension of a resource name
     */
    public static String getResourceExtension(IResource resource) {
        final String name = resource.getName();
        final int lastDotIndex = name.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return null;
        }
        return name.substring(lastDotIndex + 1);
    }

    /**
     * Returns the name of a resource without the extension.
     * <p>
     * For example:
     * <ul>
     * <li><b>request.xml</b> yields <b>"request"</b></li>
     * <li><b>page.html</b> yields <b>"page"</b></li>
     * <li><b>sample.</b> yields <b><code>"sample"</code></b></li>
     * <li><b>sample</b> yields <b><code>"sample"</code></b></li>
     * </ul>
     *
     * @param resource resource who's pure name will be returned.
     * @return the name of a resource without the extension at the end.
     */
    public static String getResourcePureName(IResource resource) {
        final String name = resource.getName();
        final int lastDotIndex = name.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return name;
        }
        return name.substring(0, lastDotIndex);
    }

    /**
     * Instantiates a new resource util.
     */
    /*
     * Disable instantiation
     */
    private ResourceUtil() {
        super();
    }

}
