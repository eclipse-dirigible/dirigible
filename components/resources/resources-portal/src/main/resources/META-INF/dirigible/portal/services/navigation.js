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
const perspectiveData = {
    id: "portal-launchpad",
    name: "Dashboard",
    link: "../dashboard.html",
    order: "0",
    icon: "../images/navigation.svg",
};

if (typeof exports !== 'undefined') {
    exports.getPerspective = function () {
        return perspectiveData;
    }
}
