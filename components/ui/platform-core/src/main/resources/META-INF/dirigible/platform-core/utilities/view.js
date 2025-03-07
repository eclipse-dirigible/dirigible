/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
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
function UUIDGenerate() {
    function _p8(s) {
        const p = (Math.random().toString(16) + '000000000').substring(2, 10);
        return s ? `-${p.substring(0, 4)}-${p.substring(4, 8)}` : p;
    }
    return _p8() + _p8(true) + _p8(true) + _p8();
}
function getBrandingInfo() {
    if (top.hasOwnProperty('PlatformBranding')) return top.PlatformBranding;
    throw Error("PlatformBranding is not set!");
}
function setBrandingInfo({ name, brand, brandUrl, icons, logo, theme, prefix } = {}) {
    if (name) top.PlatformBranding.name = name;
    if (brand) top.PlatformBranding.brand = brand;
    if (brandUrl) top.PlatformBranding.brandUrl = brandUrl;
    if (icons && icons['favicon']) top.PlatformBranding.icons.favicon = icons['favicon'];
    if (logo) top.PlatformBranding.logo = logo;
    if (theme) top.PlatformBranding.theme = theme;
    if (prefix) top.PlatformBranding.prefix = prefix;
}
function getViewParameters({ vframe = window, attribute = 'data-parameters' } = {}) {
    if (vframe.frameElement && vframe.frameElement.hasAttribute(attribute)) {
        return JSON.parse(vframe.frameElement.getAttribute(attribute) ?? '{}');
    }
    return {};
}