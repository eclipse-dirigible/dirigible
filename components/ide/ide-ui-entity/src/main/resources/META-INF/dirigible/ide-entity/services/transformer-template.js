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
import * as transformer from "ide-entity/template/transform-edm";

export function generate(model, parameters) {
    let workspaceName = parameters.workspaceName;
    let projectName = parameters.projectName;
    let filePath = parameters.filePath;
    let fileName = filePath.substring(0, filePath.indexOf(".edm"));
    return [{
        path: `${fileName}.model`,
        content: transformer.transform(workspaceName, projectName, filePath)
    }]
};

export function getTemplate() {
    return {
        name: "Entity Data to JSON Model Transformer",
        description: "Model transformer template",
        extension: "edm",
        sources: [],
        parameters: []
    }
}