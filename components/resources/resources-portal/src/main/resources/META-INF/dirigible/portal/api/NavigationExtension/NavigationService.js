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

const navigationList = [];
const navigationExtensions = extensions.getExtensions("dashboard-navigations");

for (let i = 0; i < navigationExtensions.length; i++) {
    const extensionPath = navigationExtensions[i];

    let path = `../../../${extensionPath}`;

    const { getNavigation } = await import(path);

    try {
        const navigation = getNavigation();
        navigationList.push(navigation);
    } catch (err) {
        console.error(`Failed to load a navigation group in NavigationService: ${err}\npath: ${path}`);
    }
}

response.println(JSON.stringify(navigationList));
