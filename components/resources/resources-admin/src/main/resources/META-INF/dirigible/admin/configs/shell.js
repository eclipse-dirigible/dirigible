/*
 * Copyright (c) 2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
const shellData = {
    id: 'adminShell',
    path: '/services/web/admin/index.html',
    label: 'Administration',
    icon: 'shield',
    description: 'The administration surface: every entity as a plain table and form, one level above the database.',
    // Between the Partner portal (30) and the Workbench (40): the Administration shell is an
    // operator surface, so it belongs with the runtime shells rather than after the development one.
    order: 35
};
if (typeof exports !== 'undefined') {
    exports.getShell = () => shellData;
}
