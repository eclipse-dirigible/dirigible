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
import * as transformer from "editor-entity/template/transform-edm";
import { Workspace as workspaceManager } from "@aerokit/sdk/platform";
import { Bytes } from "@aerokit/sdk/io";

const workspace = __context.get('workspace');
const project = __context.get('project');
const path = __context.get('path');

if (path && path.endsWith(".edm")) {
    const modelPath = path.replace(".edm", ".model");
    const content = transformer.transform(workspace, project, path);

    if (content !== null) {
        const input = Bytes.textToByteArray(content);

        if (workspaceManager.getWorkspace(workspace)
            .getProject(project).getFile(path).exists()) {
            workspaceManager.getWorkspace(workspace)
                .getProject(project).createFile(modelPath, input);
        } else {
            workspaceManager.getWorkspace(workspace)
                .getProject(project).getFile(modelPath).setContent(input);
        }
    }
}
