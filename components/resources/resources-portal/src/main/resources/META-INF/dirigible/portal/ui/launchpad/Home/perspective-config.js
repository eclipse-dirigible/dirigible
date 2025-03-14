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
const viewData = {
    id: 'portal-home-launchpad',
    label: 'Home Launchpad',
    region: 'center',
    path: '/services/web/portal/ui/launchpad/Home/index.html',
    order: 1,
    icon: '/services/web/resources/unicons/estate.svg',
};
if (typeof exports !== 'undefined') {
    exports.getPerspective = () => viewData;
}