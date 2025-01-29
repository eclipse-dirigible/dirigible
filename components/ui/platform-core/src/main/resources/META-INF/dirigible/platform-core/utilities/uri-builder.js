/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
function UriBuilder() {
    let pathSegments = [];
    this.path = function (paths) {
        if (!Array.isArray(paths))
            paths = [paths];
        paths = paths.filter((segment) => segment).map((segment) => {
            if (segment.length) {
                if (segment.charAt(segment.length - 1) === '/')
                    segment = segment.substring(0, segment.length - 2);
                segment = encodeURIComponent(segment);
            }
            return segment;
        });
        pathSegments = pathSegments.concat(paths);
        return {
            path: this.path,
            build: this.build
        };
    };
    this.build = function (isBasePath = true) {
        let path;
        if (isBasePath) {
            path = '/' + pathSegments.join('/');
        } else path = pathSegments.join('/');
        pathSegments.length = 0;
        return path;
    };
    return this;
};