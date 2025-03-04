/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests;

import org.eclipse.dirigible.integration.tests.ui.tests.projects.BaseTestProject;
import org.eclipse.dirigible.tests.IDE;
import org.eclipse.dirigible.tests.util.ProjectUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
class BPMLeaveRequestTestProject extends BaseTestProject {

    private static final String PROCESS_LEAVE_REQUEST_FORM_FILENAME = "process-leave-request.form";
    private static final String SUBMIT_LEAVE_REQUEST_FORM_FILENAME = "submit-leave-request.form";

    BPMLeaveRequestTestProject(IDE ide, ProjectUtil projectUtil) {
        super("BPMLeaveRequestIT", ide, projectUtil);
    }

    @Override
    public void verify() {
        // something
    }

    void generateForms() {
        generateForm(PROCESS_LEAVE_REQUEST_FORM_FILENAME);
        generateForm(SUBMIT_LEAVE_REQUEST_FORM_FILENAME);
    }

}
