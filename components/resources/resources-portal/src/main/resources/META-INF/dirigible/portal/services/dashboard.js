/*
 * Copyright (c) 2010-2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
const viewData = {
    id: "portal-dashboard",
    label: "Dashboard",
    lazyLoad: true,
    link: "/services/web/portal/subviews/dashboard.html"
};
if (typeof exports !== 'undefined') {
    exports.getView = function () {
        return viewData;
    }
}