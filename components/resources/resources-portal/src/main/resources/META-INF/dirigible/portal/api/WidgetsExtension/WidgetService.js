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

const widgetList = [];
const widgetExtensions = extensions.getExtensions("dashboard-widgets");

for (let i = 0; i < widgetExtensions.length; i++) {
    const extensionPath = widgetExtensions[i];

    let path = `../../../${extensionPath}`;

    const { getWidget } = await import(path);

    try {
        const widget = getWidget();
        widgetList.push(widget);
    } catch (err) {
        console.error(err)
    }
}

response.println(JSON.stringify(widgetList));
