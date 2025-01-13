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
import { extensions } from "sdk/extensions";
import { response } from "sdk/http";

const groupList = [];
const groupExtensions = extensions.getExtensions("dashboard-navigation-groups");

for (let i = 0; i < groupExtensions.length; i++) {
    const extensionPath = groupExtensions[i];

    let path = `../../../${extensionPath}`;

    const { getGroup } = await import(path);

    try {
        const group = getGroup();
        groupList.push(group);
    } catch (err) {
        console.error(`Failed to load a widget in WidgetService: ${err}\npath: ${path}`);
    }
}

response.println(JSON.stringify(groupList));
