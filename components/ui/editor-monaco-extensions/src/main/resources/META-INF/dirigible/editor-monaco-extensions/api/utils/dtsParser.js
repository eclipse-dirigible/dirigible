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
import { registry } from "@aerokit/sdk/platform";
import { extensions } from "@aerokit/sdk/extensions";

// Returns the path to d.ts foreach modules.json entry where isPackageDescription = true
let dtsPaths = [];

let apiModulesExtensions = await extensions.loadExtensionModules("api-modules");
let extModulesExtensions = await extensions.loadExtensionModules("ext-modules");
let apis = apiModulesExtensions.concat(extModulesExtensions);

apis.forEach(function (apiModule) {
    const content = apiModule.getContent();

    for (let [property, value] of Object.entries(content)) {
        let isPackageDescription = value["isPackageDescription"];
        let shouldBeUnexposedToESM = value["shouldBeUnexposedToESM"];
        if (shouldBeUnexposedToESM) {
            continue;
        }
        if (typeof isPackageDescription === 'boolean' && isPackageDescription === true) {
            dtsPaths.push(value["dtsPath"])
        }
    }
});

export const getDtsPaths = () => {
    return dtsPaths;
}

// Returns the concatenated file content.
export const getDtsFileContents = (paths) => {
    let result = ""
    for (const path of paths) {
        result = result.concat(registry.getText(path) + "\n");
    }

    return result;
}
