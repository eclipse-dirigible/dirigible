/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
var transformer = require("ide-entity/template/transform-edm");
var workspaceManager = require("platform/v4/workspace");

var workspace = __context.get('workspace');
var project = __context.get('project');
var path = __context.get('path');

var modelPath = path.replace(".edm", ".model");
var content = transformer.transform(workspace, project, path);

if (content !== null) {
    var bytes = require("io/v4/bytes");
    input = bytes.textToByteArray(content);

    if (workspaceManager.getWorkspace(workspace)
        .getProject(project).getFile(path).exists()) {
            workspaceManager.getWorkspace(workspace)
                .getProject(project).createFile(modelPath, input);
    } else {
        workspaceManager.getWorkspace(workspace)
            .getProject(project).getFile(modelPath).setContent(input);
    }
}
