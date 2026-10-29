/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.template.edm;

import java.nio.charset.StandardCharsets;

import org.eclipse.dirigible.components.workspace.domain.Project;
import org.eclipse.dirigible.components.ide.workspace.service.WorkspaceOnSaveHandler;
import org.springframework.stereotype.Component;

/**
 * Writes the {@code .model} of an entity data model whenever its {@code .edm} is saved in the
 * workspace - which is what saving a diagram in the entity modeler does - so that the model the
 * generation templates read always follows the diagram.
 *
 * <p>
 * The {@code .model} is written beside the {@code .edm}, under the same name, replacing the one
 * there. An {@code .edm} it cannot be derived from - not well-formed XML, or of a shape no
 * {@code .model} can be built from - leaves the {@code .model} as it was, and the failure is logged
 * by the workspace.
 */
@Component
class EdmModelOnSaveHandler implements WorkspaceOnSaveHandler {

    private static final String EDM_EXTENSION = ".edm";

    private static final String MODEL_EXTENSION = ".model";

    @Override
    public void onSave(Project project, String path) {
        if (!path.endsWith(EDM_EXTENSION)) {
            return;
        }
        String edm = new String(project.getFile(path)
                                       .getContent(),
                StandardCharsets.UTF_8);
        String model = EdmModelTransformation.transform(edm);
        String modelPath = path.substring(0, path.length() - EDM_EXTENSION.length()) + MODEL_EXTENSION;
        project.createFile(modelPath, model.getBytes(StandardCharsets.UTF_8));
    }

}
